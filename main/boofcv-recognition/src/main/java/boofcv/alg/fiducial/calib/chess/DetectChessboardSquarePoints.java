/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.fiducial.calib.chess;

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.fiducial.calib.squares.*;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.misc.CircularIndex;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Polygon2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;

import java.util.List;

/**
 * Given a binary image it detects the presence of chess board calibration grids. 1) Detect blobs
 * in binary image and select square like ones. 2) Create two grids, inner and outer, 3) Put the
 * grids together, 4) extract initial estimate for corners
 *
 * @author Peter Abeles
 */
public class DetectChessboardSquarePoints<T extends ImageGray<T>>
{

	// detector for squares
	DetectPolygonBinaryGrayRefine<T> detectorSquare;

	// Converts detected squares into a graph and into grids
	SquaresIntoCrossClusters s2c;
	SquareCrossClustersIntoGrids c2g;

	// size of square grids
	private int numRows,numCols;

	// bounding quadrilateral
	private Polygon2D_I32 boundPolygon = new Polygon2D_I32();

	SquareGridTools tools = new SquareGridTools();

	FastQueue<Point2D_F64> calibrationPoints = new FastQueue<>(Point2D_F64.class, true);

	// maximum distance two corners can be from each other
	ConfigLength maxCornerDistance;

	// List of nodes put into clusters
	List<List<SquareNode>> clusters;

	Polygon2D_F64 work = new Polygon2D_F64();

	/**
	 * Configures chess board detector.
	 *
	 * @param numRows Number of rows in square grid
	 * @param numCols Number of columns in square grid
	 * @param maxCornerDistance Maximum distance in pixels that two "overlapping" corners can be from each other.
	 */
	public DetectChessboardSquarePoints(int numRows, int numCols, ConfigLength maxCornerDistance,
										DetectPolygonBinaryGrayRefine<T> detectorSquare)
	{
		this.maxCornerDistance = maxCornerDistance;

		this.numRows = numRows;
		this.numCols = numCols;

		this.detectorSquare = detectorSquare;
		if( detectorSquare != null ) { // null in some unit tests for simplicity
			// configure the detector
			detectorSquare.setHelper(new ChessboardPolygonHelper<>());
			detectorSquare.getDetector().setOutputClockwise(true);
			detectorSquare.getDetector().setConvex(true);
//			detectorSquare.getDetector().setNumberOfSides(3,8);  <--- this is handled by the helper
			this.detectorSquare.setFunctionAdjust((info, clockwise) ->
					DetectChessboardSquarePoints.this.adjustBeforeOptimize(info.polygon, info.borderCorners,clockwise));
		}

		s2c = new SquaresIntoCrossClusters(-1,-1);
		c2g = new SquareCrossClustersIntoGrids();
	}

	/**
	 * Detects chessboard in the binary image.  Square corners must be disconnected.
	 * Returns true if a chessboard was found, false otherwise.
	 *
	 * @param input Original input image.
	 * @param binary Binary image of chessboard
	 * @return True if successful.
	 */
	public boolean process( T input , GrayU8 binary ) {

		double maxCornerDistancePixels = maxCornerDistance.computeI(Math.min(input.width,input.height));
		s2c.setMaxCornerDistance(maxCornerDistancePixels);

		configureContourDetector(input);
		boundPolygon.vertexes.reset();

		detectorSquare.process(input, binary);
		detectorSquare.refineAll();
		List<DetectPolygonFromContour.Info> found = detectorSquare.getPolygonInfo();

		clusters = s2c.process(found);

		c2g.process(clusters);
		List<SquareGrid> grids = c2g.getGrids().toList();

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid grid = grids.get(i);
			if( grid.rows == numCols && grid.columns == numRows ) {
				tools.transpose(grid);
			}
			if( grid.rows == numRows && grid.columns == numCols ) {
				// this detector requires that the (0,0) grid cell has a square inside of it
				if( grid.get(0,0) == null ){
					if( grid.get(0,-1) != null ) {
						tools.flipColumns(grid);
					} else if( grid.get(-1,0) != null ) {
						tools.flipRows(grid);
					} else {
						continue;
					}
				}

				// make sure its in the expected orientation
				if( !ensureCCW(grid) )
					continue;

				// If symmetric, ensure that the (0,0) is closest to top-left image corner
				putIntoCanonical(grid);

				// now extract the calibration points
				return computeCalibrationPoints(grid);
			}
		}

		return false;
	}

	/**
	 * Configures the contour detector based on the image size. Setting a maximum contour and turning off recording
	 * of inner contours and improve speed and reduce the memory foot print significantly.
	 */
	private void configureContourDetector(T gray) {
		// determine the maximum possible size of a square when viewed head on
		// also take in account shapes touching the edge will be concave
		int maxContourSize = Math.max(gray.width,gray.height)/Math.max(numCols,numRows);
		BinaryContourFinder contourFinder = detectorSquare.getDetector().getContourFinder();
		contourFinder.setMaxContour(maxContourSize*4*2); // fisheye distortion can let one square go larger
		contourFinder.setSaveInnerContour(false);
	}

	/**
	 * The polygon detected from the contour is too small because the binary image was eroded. This expand the size
	 * of the polygon so that it fits the image edge better
	 */
	public void adjustBeforeOptimize(Polygon2D_F64 polygon, GrowQueue_B touchesBorder, boolean clockwise) {
		int N = polygon.size();
		work.vertexes.resize(N);
		for (int i = 0; i < N; i++) {
			work.get(i).set(0, 0);
		}

		for (int i = N - 1, j = 0; j < N; i = j, j++) {
			int ii,jj,kk,mm;
			if( clockwise ) {
				mm = CircularIndex.addOffset(-1,i,N);
				ii=i;jj=j;
				kk = CircularIndex.addOffset(1,j,N);
			} else {
				mm = CircularIndex.addOffset(1,j,N);
				ii=j;jj=i;
				kk = CircularIndex.addOffset(-1,i,N);
			}

			Point2D_F64 a = polygon.get(ii), b = polygon.get(jj);

			double dx = b.x - a.x;
			double dy = b.y - a.y;

			double l = Math.sqrt(dx * dx + dy * dy);

			// The input polygon has two identical corners. This is bad. We will just skip over the first corner
			if( l == 0 ) {
				throw new RuntimeException("Input polygon has two identical corners. You need to fix that.");
			}

			dx *= 1.5 / l;
			dy *= 1.5 / l;

			Point2D_F64 _a = work.get(ii);
			Point2D_F64 _b = work.get(jj);

			// move the point away from the line
			if( touchesBorder.size>0 && touchesBorder.get(ii) ) {
				if(!touchesBorder.get(mm)) {
					_a.x -= dx;
					_a.y -= dy;
				}
			} else {
				_a.x += -dy;
				_a.y += dx;
			}
			if( touchesBorder.size>0 && touchesBorder.get(jj) ) {
				if( !touchesBorder.get(kk) ) {
					_b.x += dx;
					_b.y += dy;
				}
			} else {
				_b.x += -dy;
				_b.y += dx;
			}
		}

		for (int i = 0; i < N; i++) {
			Point2D_F64 a = polygon.get(i);
			Point2D_F64 b = work.get(i);
			a.x += b.x;
			a.y += b.y;
		}
	}

	/**
	 * Ensures that the grid is in a CCW order.  It is assumed that (0,0) is a square.
	 *
	 * @return true if it was able to make it CCW or false if it failed to
	 */
	boolean ensureCCW( SquareGrid grid ) {
		if( grid.columns <= 2 && grid.rows <= 2 )
			return true;

		Point2D_F64 a,b,c;

		a = grid.get(0,0).center;
		if( grid.columns > 2)
			b = grid.get(0,2).center;
		else
			b = grid.get(1,1).center;

		if( grid.rows > 2)
			c = grid.get(2,0).center;
		else
			c = grid.get(1,1).center;

		double x0 = b.x-a.x;
		double y0 = b.y-a.y;

		double x1 = c.x-a.x;
		double y1 = c.y-a.y;

		double z = x0 * y1 - y0 * x1;
		if( z < 0 ) {
			// flip it along an axis which is symmetric
			if( grid.columns%2 == 1 )
				tools.flipColumns(grid);
			else if( grid.rows%2 == 1 )
				tools.flipRows(grid);
			else
				return false;
		}
		return true;
	}

	/**
	 * Examines the grid and makes sure the (0,0) square is the closest one ot the top left corner.
	 * Only flip operations are allowed along symmetric axises
	 */
	void putIntoCanonical( SquareGrid grid ) {

		boolean rowOdd = grid.rows%2 == 1;
		boolean colOdd = grid.columns%2 == 1;

		if( colOdd == rowOdd ) {
			// if odd and square then 4 solutions.  Otherwise just two solution that are on
			// opposite sides on the grid
			if( rowOdd && grid.rows == grid.columns ) {
				int best = -1;
				double bestDistance = Double.MAX_VALUE;
				for (int i = 0; i < 4; i++) {
					SquareNode n = grid.getCornerByIndex(i);
					double d = n.center.normSq();
					if( d < bestDistance ) {
						best = i;
						bestDistance = d;
					}
				}

				for (int i = 0; i < best; i++) {
					tools.rotateCCW(grid);
				}
			} else {
				double first = grid.get(0,0).center.normSq();
				double last = grid.getCornerByIndex(2).center.normSq();

				if( last < first ) {
					tools.reverse(grid);
				}
			}
		}
		// if only one is odd then there is a unique solution.  Since uber is already in a legit
		// configuration nothing needs ot be done
	}

	/**
	 * Find inner corner points across the grid.  Start from the "top" row and work its way down.  Corners
	 * are found by finding the average point between two adjacent corners on adjacent squares.
	 */
	boolean computeCalibrationPoints(SquareGrid grid) {
		calibrationPoints.reset();

		for (int row = 0; row < grid.rows-1; row++) {
			int offset = row%2;
			for (int col = offset; col < grid.columns; col += 2) {
				SquareNode a = grid.get(row,col);

				if( col > 0 ) {
					SquareNode b = grid.get(row+1,col-1);
					if( !setIntersection(a,b,calibrationPoints.grow()))
						return false;
				}

				if( col < grid.columns-1) {
					SquareNode b = grid.get(row+1,col+1);
					if( !setIntersection(a,b,calibrationPoints.grow()))
						return false;
				}
			}
		}

		return true;
	}

	private boolean setIntersection( SquareNode a , SquareNode n , Point2D_F64 point ) {
		for (int i = 0; i < a.edges.length; i++) {
			SquareEdge edge = a.edges[i];
			if( edge != null && edge.destination(a) == n ) {
				Point2D_F64 p0 = edge.a.square.get(edge.sideA);
				Point2D_F64 p1 = edge.b.square.get(edge.sideB);

				point.x = (p0.x+p1.x)/2.0;
				point.y = (p0.y+p1.y)/2.0;
				return true;
			}
		}
		return false;
	}

	public List<List<SquareNode>> getGraphs() {
		return clusters;
	}

	public SquaresIntoCrossClusters getShapeToClusters() {
		return s2c;
	}

	public SquareCrossClustersIntoGrids getGrids() {
		return c2g;
	}

	public DetectPolygonBinaryGrayRefine<T> getDetectorSquare() {
		return detectorSquare;
	}

	public FastQueue<Point2D_F64> getCalibrationPoints() {
		return calibrationPoints;
	}

	public int getNumRows() {
		return numRows;
	}

	public int getNumCols() {
		return numCols;
	}
}
