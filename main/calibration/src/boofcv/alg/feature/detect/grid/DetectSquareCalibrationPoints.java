/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.combinatorics.Combinations;

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

	// maximum number of possible targets it will consider
	private int maxCombinations;

	// relative blob size threshold.  Adjusted relative to image size.  Small objects are pruned
	private double relativeSizeThreshold;

	// Explaining why it failed
	private String errorMessage;

	// used to order points
	OrderPointsIntoGrid orderAlg = new OrderPointsIntoGrid();

	// Found interest points on order
	List<Point2D_F64> interestPoints;
	// squares that interest points originated from
	private List<QuadBlob> interestSquares;

	/**
	 *
	 * @param maxCombinations Maximum number of combinations of squares it will try when looking for a target. Try 500.
	 * @param relativeSizeThreshold Increases or decreases the minimum allowed blob size. Try 1.0
	 * @param gridCols Number of squares wide the grid is. Target dependent.
	 * @param gridRows Number of squares tall the grid is. Target dependent.
	 */
	public DetectSquareCalibrationPoints(int maxCombinations,
										 double relativeSizeThreshold,
										 int gridCols, int gridRows) {
		if( gridCols <= 0 || gridRows <= 0 )
			throw new IllegalArgumentException("Columns and rows must be more than zero");
		if( gridCols%2 == 0 || gridRows%2 == 0)
			throw new IllegalArgumentException("Number of columns and rows must be odd");

		// black squares are every other element in the grid
		this.numSquares = (gridCols/2+1)*(gridRows/2+1);
		this.maxCombinations = maxCombinations;
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
		int contourSize = (int)(relativeSizeThreshold*80.0/640.0*thresholded.width);
		detectBlobs.setMinContourSize(contourSize);

		// initialize data structures
		binaryA.reshape(thresholded.width,thresholded.height);
		binaryB.reshape(thresholded.width,thresholded.height);

		// filter out small objects
		BinaryImageOps.erode8(thresholded,binaryA);
		BinaryImageOps.erode8(binaryA,binaryB);
		BinaryImageOps.dilate8(binaryB, binaryA);
		BinaryImageOps.dilate8(binaryA,binaryB);

		if( !detectBlobs.process(binaryB) )
			return fail(detectBlobs.getMessage());

		squares = detectBlobs.getDetected();

		// find connections between squares
		ConnectGridSquares.connect(squares);

		// Remove all but the largest islands in the graph to reduce the number of combinations
		List<QuadBlob> squaresPruned = ConnectGridSquares.pruneSmallIslands(squares);
//		System.out.println("Found "+squaresPruned.size()+" blobs");
		
		// given all the blobs, only consider N at one time until a valid target is found
		return shuffleToFindTarget(squaresPruned);
	}

	/**
	 * Shuffles through all the different possible sets of blobs to find the valid target
	 *
	 * @return true of it worked
	 */
	private boolean shuffleToFindTarget( List<QuadBlob> squares ) {

		if( squares.size() < numSquares )
			return fail("Not enough blobs detected");

		Combinations<QuadBlob> combinations = new Combinations<QuadBlob>(squares,numSquares);

//		System.out.println("------------------------------------"+squares.size()+"  N "+N);
//		System.out.println("Total Shuffles: "+combinations.numShuffles());
		if( combinations.computeTotalCombinations() > maxCombinations) {
			return fail("Too many possible combinations");
		}

		List<QuadBlob> list = new ArrayList<QuadBlob>();

		while( true ) {
//			System.out.println("Next combination "+num++);
			combinations.getBucket(list);

			// assumes that all the items in the list are part of a target
			// see if it fails internal sanity checks
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
			if( !combinations.next() )
				return fail("No target found after shuffling");
		}
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
}
