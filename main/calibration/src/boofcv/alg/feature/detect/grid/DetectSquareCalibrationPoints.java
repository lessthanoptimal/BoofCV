/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.grid;

import boofcv.alg.feature.detect.InvalidCalibrationTarget;
import boofcv.alg.feature.detect.quadblob.DetectQuadBlobsBinary;
import boofcv.alg.feature.detect.quadblob.OrderPointsIntoGrid;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Detects square grid calibration targets from binary images.  The output is a set of ordered calibration
 * points with the number of rows and columns.  Calibration points are in a row-major ordering. Processing
 * steps:
 * <ol>
 * <li>Morphological noise reduction</li>
 * <li>Create blob clusters</li>
 * <li>Detect rectangles</li>
 * <li>Order rectangles and extract calibration points.</li>
 * </ol>
 * See comments inside of code for how false positive blobs are removed.  Rotational orientation is not ensured and
 * for some applications the orientation might need to be adjusted.
 * <p>
 *
 * @author Peter Abeles
 */
public class DetectSquareCalibrationPoints {

	// images which store intermediate steps in processing cycle
	private ImageUInt8 binaryA = new ImageUInt8(1,1);
	private ImageUInt8 binaryB = new ImageUInt8(1,1);

	// detects the initial list of squares
	DetectQuadBlobsBinary detectBlobs;

	// list if found corners/blobs
	private List<QuadBlob> squares;

	// number of black squares in calibration grid
	private int numSquares;

	// number of block squares in each grid row/column
	private int numBlackCols, numBlackRows;

	// relative blob size threshold.  Adjusted relative to image size.  Small objects are pruned
	private double relativeSizeThreshold;

	// Explaining why it failed
	private String errorMessage;

	// used to order points
	OrderPointsIntoGrid orderAlg = new OrderPointsIntoGrid();

	// Found interest points on order
	List<Point2D_F64> interestPoints;
	// squares that interest points originated from
	private List<QuadBlob> interestSquares = new ArrayList<QuadBlob>();

	// length of the space relative to the length of a square in the grid
	private double spaceToSquareRatio;

	/**
	 *
	 * @param relativeSizeThreshold Increases or decreases the minimum allowed blob size. Try 1.0
	 * @param spaceToSquareRatio length of the space relative to the length of a square in the grid
	 * @param gridCols Number of squares wide the grid is. Target dependent.
	 * @param gridRows Number of squares tall the grid is. Target dependent.
	 */
	public DetectSquareCalibrationPoints(double relativeSizeThreshold,
										 double spaceToSquareRatio ,
										 int gridCols, int gridRows) {
		if( gridCols <= 0 || gridRows <= 0 )
			throw new IllegalArgumentException("Columns and rows must be more than zero");
		if( gridCols%2 == 0 || gridRows%2 == 0)
			throw new IllegalArgumentException("Number of columns and rows must be odd");

		// black squares are every other element in the grid
		this.numBlackRows = gridRows/2+1;
		this.numBlackCols = gridCols/2+1;

		this.spaceToSquareRatio = spaceToSquareRatio;

		this.numSquares = numBlackRows *numBlackCols;
		this.relativeSizeThreshold = relativeSizeThreshold;

		// minContourSize is specified later after the image's size is known
		detectBlobs = new DetectQuadBlobsBinary(0,0.25,numSquares);
	}

	/**
	 * Processes the image and detects calibration targets.  If one is found then
	 * true is returned and calibration points are extracted.
	 *
	 * @param thresholded Binary image where potential grid squares are set to one.
	 * @return True if it was successful and false otherwise.  If false call getMessage() for details.
	 */
	public boolean process( ImageUInt8 thresholded )
	{
		// discard old results
		interestPoints = new ArrayList<Point2D_F64>();
		interestSquares = new ArrayList<QuadBlob>();

		// adjust threshold for image size
		int contourSize = (int)(relativeSizeThreshold*30.0/640.0*thresholded.width);
		detectBlobs.setMinContourSize(contourSize);

		// initialize data structures
		binaryA.reshape(thresholded.width,thresholded.height);
		binaryB.reshape(thresholded.width,thresholded.height);

		// filter out small objects
		BinaryImageOps.erode8(thresholded, 1, binaryA);
		BinaryImageOps.erode8(binaryA, 1, binaryB);
		BinaryImageOps.dilate8(binaryB, 1, binaryA);
		BinaryImageOps.dilate8(binaryA, 1, binaryB);

		if( !detectBlobs.process(binaryB) )
			return fail(detectBlobs.getMessage());

		squares = detectBlobs.getDetected();

		// find connections between squares
		ConnectGridSquares.connect(squares,spaceToSquareRatio);

		// Remove all but the largest islands in the graph to reduce the number of combinations
		List<QuadBlob> squaresPruned = ConnectGridSquares.pruneSmallIslands(squares);

		// given all the blobs, only consider N at one time until a valid target is found
		if( findTargetByAssumption(squaresPruned,interestSquares)) {
			computeInterestPoints(interestSquares);
			return true;
		} else {
			interestSquares.addAll(squaresPruned);
			return false;
		}
	}

	/**
	 * For each blob assume it is the first blob in the grid.  Continue with that hypothesis until it is proven false.
	 *
	 * @return true of it worked
	 */
	private boolean findTargetByAssumption( List<QuadBlob> squares , List<QuadBlob> targetSquares ) {
		if( squares.size() < numSquares )
			return fail("Not enough blobs detected");

		targetSquares.clear();
		for( int i = 0; i < squares.size(); i++ ) {
			QuadBlob seed = squares.get(i);

			if(constructTarget(seed,targetSquares) ) {
				return true;
			}
		}

		return false;
	}

	private boolean constructTarget( QuadBlob seed , List<QuadBlob> targetSquares ) {
		if( seed.conn.size() < 2 )
			return false;
		targetSquares.clear();

		// find the top most row
		List<QuadBlob> firstRow = new ArrayList<QuadBlob>();

		QuadBlob nextRow = null;
		for( int i = 0; i < seed.conn.size(); i++ ) {
			if( findLine(seed, seed.conn.get(i), firstRow,numBlackCols)) {
				nextRow = seed.conn.get(i);
				break;
			}
		}

		if( nextRow == null )
			return false;

		double angleDown = findDownDirection(seed,nextRow);
		if( Double.isNaN(angleDown))
			return false;

		List<QuadBlob> columns[] = new ArrayList[ numBlackCols ];
		for( int i = 0; i < numBlackCols; i++ ) {
			columns[i] = new ArrayList<QuadBlob>();
		}

		SelectResult columnResult = new SelectResult();
		for( int i = 0; i < firstRow.size(); i++ ) {
			QuadBlob target = firstRow.get(i);
			QuadBlob left = i > 0 ? firstRow.get(i-1) : firstRow.get(i);
			QuadBlob right = i < firstRow.size()-1 ? firstRow.get(i+1) : firstRow.get(i);

			if( !selectDown(target,left,right,angleDown,columnResult) ) {
				return false;
			}

			angleDown = columnResult.angle;
			if( !findLine(target,columnResult.quad,columns[i], numBlackRows) )
				return false;
		}

		for( int i = 0; i < numBlackRows; i++ ) {
			for( int j = 0; j < numBlackCols; j++ ) {
			 	targetSquares.add( columns[j].get(i) );
			}
		}

		return true;
	}

	private double findDownDirection( QuadBlob pivot , QuadBlob next ) {
		double angle0 = UtilAngle.bound(angleQuad(pivot,next) + Math.PI/2);

		double bestAcute = 0.6;
		double bestAngle = Double.NaN;
		QuadBlob bestQuad = null;
		for( int i = 0; i < pivot.conn.size(); i++ ) {
			QuadBlob q = pivot.conn.get(i);

			double angle1 = angleQuad(pivot,q);

			double acute = UtilAngle.distHalf(angle0,angle1);

			if( acute < bestAcute ) {
				bestAcute = acute;
				bestAngle = angle1;
				bestQuad = q;
			}
		}

		if( bestQuad == null )
			return Double.NaN;
		return bestAngle;
	}

	private boolean findLine(QuadBlob seed1, QuadBlob seed2, List<QuadBlob> line, int numExpected ) {
		line.clear();
		line.add(seed1);
		line.add(seed2);

		SelectResult result = new SelectResult();

		double angle = angleQuad(seed1, seed2);

		while( true ) {

			if( selectNexInLine(seed2, angle, result) && result.distance < 0.3 ) {
				line.add(result.quad);
				seed2 = result.quad;
				angle = result.angle;
			} else {
				break;
			}
		}

		return line.size() == numExpected;
	}

	private double angleQuad(QuadBlob seed1, QuadBlob seed2) {
		return Math.atan2( seed2.center.y - seed1.center.y , seed2.center.x - seed1.center.x );
	}

	private boolean selectNexInLine(QuadBlob seed1, double angleTarget, SelectResult result) {

		QuadBlob bestN = null;
		double bestDistance = Double.MAX_VALUE;
		double bestAngle = -1;
		for( int i = 0; i < seed1.conn.size(); i++ ) {
			QuadBlob n = seed1.conn.get(i);

			double angle2 = angleQuad(seed1, n);

			double d = UtilAngle.dist(angleTarget, angle2);
			if( d < bestDistance ) {
				bestN = n;
				bestDistance = d;
				bestAngle = angle2;
			}
		}

		if( bestN == null || bestDistance > 0.4  )
			return false;

		result.distance = bestDistance;
		result.quad = bestN;
		result.angle = bestAngle;

		return true;
	}

	private boolean selectDown( QuadBlob target , QuadBlob conn1 , QuadBlob conn2 , double angle ,
								SelectResult result ) {
		QuadBlob bestN = null;
		double bestDistance = 0.6;
		double bestAngle = -1;

		for( QuadBlob c : target.conn ) {
			if( c == conn1 )
				continue;
			if( c == conn2 )
				continue;

			double angle2 = angleQuad(target, c);

			double d = UtilAngle.dist(angle, angle2);
			if( d < bestDistance ) {
				bestN = c;
				bestDistance = d;
				bestAngle = angle2;
			}
		}

		if( bestN == null )
			return false;

		result.distance = bestDistance;
		result.quad = bestN;
		result.angle = bestAngle;

		return true;
	}

	private boolean computeInterestPoints(List<QuadBlob> list) {
		try {
			interestPoints = new ArrayList<Point2D_F64>();
			for( QuadBlob b : list ) {
				for( Point2D_I32 c : b.corners )
					interestPoints.add(new Point2D_F64(c.x,c.y));
			}

			interestSquares = list;
			orderAlg.process(interestPoints);
			interestPoints = orderAlg.getOrdered();
			return true;
		} catch (InvalidCalibrationTarget invalidTarget) {
//				System.out.println(invalidTarget.getMessage());
		}
		return false;
	}

	/**
	 * Returns corner points in quadrilateral that bounds all the target points, approximately.
	 */
	public List<Point2D_F64> getTargetQuadrilateral() {
		return orderAlg.getQuadrilateral();
	}


	private boolean fail( String message ) {
		this.errorMessage = message;
		return false;
	}
	
	public ImageSInt32 getBlobs() {
		return detectBlobs.getLabeledImage();
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public List<QuadBlob> getAllSquares() {
		return squares;
	}

	public List<QuadBlob> getInterestSquares() {
		return interestSquares;
	}

	public List<Point2D_F64> getInterestPoints() {
		return interestPoints;
	}

	public List<QuadBlob> getSquaresBad() {
		return detectBlobs.getInvalid();
	}

	public int getNumberOfLabels() {
		return detectBlobs.getNumLabels();
	}

	public DetectQuadBlobsBinary getDetectBlobs() {
		return detectBlobs;
	}

	private static class SelectResult {
		QuadBlob quad;
		double distance;
		double angle;
	}
}
