/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.feature.detect.squares.*;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Polygon2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a binary image it detects the presence of chess board calibration grids. 1) Detect blobs
 * in binary image and select square like ones. 2) Connect blobs based distance of corners. 3)
 * prune graph. 4) sanity check graph structure. 5) Find bounding quadrilateral.
 *
 * @author Peter Abeles
 */
public class DetectChessSquaresBinary<T extends ImageSingleBand> {

	// detector for squares
	BinaryPolygonConvexDetector<T> detectorSquare;

	// Converts detected squares into a graph and into grids
	SquaresIntoClusters s2c;
	ClustersIntoGrids c2g;

	// size of square grids
	private int outerRows,outerCols;
	private int innerRows,innerCols;

	// bounding quadrilateral
	private Polygon2D_I32 boundPolygon = new Polygon2D_I32();

	SquareGridTools tools = new SquareGridTools();

	// bounding polygon of inner grid
	private Polygon2D_F64 innerPolygon = new Polygon2D_F64();

	// nodes in the outer which could be the zero node
	private List<SquareNode> outerOrigins = new ArrayList<SquareNode>();

	SquareGrid uberGrid = new SquareGrid();

	FastQueue<Point2D_F64> calibrationPoints = new FastQueue<Point2D_F64>(Point2D_F64.class,true);

	// maximum distance two corners can be from each other
	double maxCornerDistanceSq;

	// storage for the nodes which are used to align the two grids
	SquareNode seedInner,seedOuter;

	/**
	 * Configures chess board detector.
	 *
	 * @param numCols Number of columns in square grid
	 * @param numRows Number of rows in square grid
	 */
	public DetectChessSquaresBinary(int numCols, int numRows, double maxCornerDistance ,
									BinaryPolygonConvexDetector<T> detectorSquare )
	{
		this.maxCornerDistanceSq = maxCornerDistance*maxCornerDistance;

		// number of black squares in rows/columns
		outerCols = numCols/2 + numCols%2;
		outerRows = numRows/2 + numRows%2;

		innerCols = numCols/2;
		innerRows = numRows/2;

		this.detectorSquare = detectorSquare;

		s2c = new SquaresIntoClusters(1.0);
		c2g = new ClustersIntoGrids(innerCols*innerRows);
	}

	/**
	 * Detects chessboard in the binary image.  Square corners must be disconnected.
	 * Returns true if a chessboard was found, false otherwise.
	 *
	 * @param input Original input image.
	 * @param binary Binary image of chessboard
	 * @return True if successful.
	 */
	public boolean process( T input , ImageUInt8 binary ) {
		boundPolygon.vertexes.reset();

		detectorSquare.process(input, binary);

		FastQueue<Polygon2D_F64> found = detectorSquare.getFound();

		List<List<SquareNode>> clusters = s2c.process(found.toList());
		c2g.process(clusters);
		List<SquareGrid> grids = c2g.getGrids();

		// find inner and outer grids of squares
		SquareGrid inner = null;
		SquareGrid outer = null;

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);

			if( (g.columns == innerCols && g.rows == innerRows) ||
					(g.columns == innerRows && g.rows == innerCols)) {
				if( inner != null )
					return false;
				inner = g;
			} else if( (g.columns == outerCols && g.rows == outerRows) ||
					(g.columns == outerRows && g.rows == outerCols)) {
				if( outer != null )
					return false;
				outer = g;
			}
		}

		if( inner == null || outer == null )
			return false;

		// make sure the rows/columns are correctly aligned
		if( inner.columns != innerCols ) {
			tools.transpose(inner);
		}

		if( outer.columns != outerCols ) {
			tools.transpose(outer);
		}

		// find a corner to align the two grids by
		tools.boundingPolygon(inner, innerPolygon);
		selectSeedZero(inner, outer, innerPolygon);
		// now align the grids
		forceToZero(seedInner, inner);
		forceToZero(seedOuter, outer);

		// create one big grid for easier processing
		createUber(outer, inner, uberGrid);

		// put it into canonical order
		putIntoCanonical(uberGrid,inner,outer);
		tools.orderSquareCorners(inner);
		tools.orderSquareCorners(outer);

		// now extract the calibration points
		return getCalibrationPoints(uberGrid);
	}

	private void selectSeedZero( SquareGrid gridOuter , SquareGrid gridInner , Polygon2D_F64 bounding  ) {
		listPossibleZeroNodes(gridOuter);

		SquareNode best = null;
		int bestCorner = 0;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < outerOrigins.size(); i++) {
			SquareNode n = outerOrigins.get(i);

			double d = Double.MAX_VALUE;
			int corner = -1;
			for (int j = 0; j < 4; j++) {
				double a = n.distanceSqCorner(bounding.get(j));
				if( a < d ) {
					corner = j;
					d = a;
				}
			}
			if( d < bestDistance ) {
				best = n;
				bestCorner = corner;
				bestDistance = d;
			}
		}

		seedOuter = best;
		switch( bestCorner ) {
			case 0:seedInner = gridInner.get( 0, 0); break;
			case 1:seedInner = gridInner.get( 0,-1); break;
			case 2:seedInner = gridInner.get(-1,-1); break;
			case 3:seedInner = gridInner.get(-1, 0); break;
			default: throw new RuntimeException("Bug!");
		}
	}

	private void listPossibleZeroNodes(SquareGrid grid) {
		outerOrigins.clear();
		if( grid.columns == grid.rows ) {
			outerOrigins.add(grid.get(0, 0));
			checkAdd(grid.get( 0,-1),outerOrigins);
			checkAdd(grid.get(-1,-1),outerOrigins);
			checkAdd(grid.get(-1, 0),outerOrigins);
		} else {
			outerOrigins.add(grid.get(0, 0));
			checkAdd(grid.get(-1,-1),outerOrigins);
		}
	}

	/**
	 * Rotates or flips the grid until the specified node is the zero index node
	 */
	private void forceToZero( SquareNode zero , SquareGrid grid) {

		int cornerIndex = grid.getCornerIndex(zero);

		if( cornerIndex != 0 ) {
			if( grid.rows == grid.columns ) {
				tools.reverse(grid);
			} else {
				for (int i = 0; i < cornerIndex; i++) {
					tools.rotateCCW(grid);
				}
			}
		}
	}

	private void createUber( SquareGrid outer , SquareGrid inner , SquareGrid uber ) {
		uber.columns = outer.columns + inner.columns;
		uber.rows = outer.rows + inner.rows;
		uber.nodes.clear();

		for (int row = 0; row < uber.rows; row++) {
			if( row % 2 == 0 ) {
				for (int col = 0; col < uber.columns; col++) {
					if( col % 2 == 0 )
						uber.nodes.add(outer.get(row/2,col/2));
					else
						uber.nodes.add(null);
				}
			} else {
				if( row % 2 == 0 ) {
					for (int col = 0; col < uber.columns; col++) {
						if( col % 2 == 1 )
							uber.nodes.add(inner.get(row/2,col/2));
						else
							uber.nodes.add(null);
					}
				}
			}
		}
	}

	private void putIntoCanonical( SquareGrid uber , SquareGrid inner , SquareGrid outer ) {

		boolean rowOdd = uber.rows%2 == 1;
		boolean colOdd = uber.columns%2 == 1;

		if( rowOdd == colOdd ) {
			if( rowOdd && uber.rows == uber.columns ) {
				int best = -1;
				double bestDistance = Double.MAX_VALUE;
				for (int i = 0; i < 4; i++) {
					SquareNode n = uber.getCornerByIndex(i);
					double d = n.center.normSq();
					if( d < bestDistance ) {
						best = 0;
						bestDistance = d;
					}
				}

				for (int i = 0; i < best; i++) {
					tools.rotateCCW(uber);
					tools.rotateCCW(inner);
					tools.rotateCCW(outer);
				}
			} else {
				double first = uber.get(0,0).center.normSq();
				double last = uber.getCornerByIndex(2).center.normSq();

				if( last < first ) {
					tools.reverse(uber);
					tools.reverse(inner);
					tools.reverse(outer);
				}
			}
		}
	}

	private boolean getCalibrationPoints( SquareGrid uber ) {
		calibrationPoints.reset();

		for (int row = 1; row < uber.rows; row++) {
			for (int col = row%2; col < uber.columns; col += 2) {
				SquareNode n = uber.get(row,col);
				Point2D_F64 p = calibrationPoints.grow();
				int left = col-1;
				int right = col+1;

				if( left >= 0 ) {
					Point2D_F64 a = uber.get(row-1,left).corners.get(2);
					Point2D_F64 b = n.corners.get(0);

					if( a.distance2(b) > maxCornerDistanceSq ) {
						return false;
					}

					p.x = (a.x+b.x)/2.0;
					p.y = (a.y+b.y)/2.0;
				}
				if( right >= 0 ) {
					Point2D_F64 a = uber.get(row-1,right).corners.get(3);
					Point2D_F64 b = n.corners.get(1);

					if( a.distance2(b) > maxCornerDistanceSq ) {
						return false;
					}

					p.x = (a.x+b.x)/2.0;
					p.y = (a.y+b.y)/2.0;
				}
			}
		}

		return true;
	}

	private void checkAdd( SquareNode node , List<SquareNode> list ) {
		if( !list.contains(node)) {
			list.add(node);
		}
	}

	boolean isAsymmetric() {
		return innerCols == outerCols || innerRows == outerRows;
	}

}
