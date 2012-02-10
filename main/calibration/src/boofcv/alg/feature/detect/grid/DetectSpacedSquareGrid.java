/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import pja.util.Shuffle;

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
public class DetectSpacedSquareGrid {

	// images which store intermediate steps in processing cycle
	private ImageUInt8 binaryA = new ImageUInt8(1,1);
	private ImageUInt8 binaryB = new ImageUInt8(1,1);

	// Orders corner points found in the image
	private PutTargetSquaresIntoOrder extractTarget;

	// detects the initial list of squares
	DetectQuadBlobsBinary detectBlobs;

	// list if found corners/blobs
	private List<QuadBlob> squares;

	// number of black squares in calibration grid
	private int gridCols;
	private int gridRows;

	// maximum number of possible targets it will consider
	private int maxShuffle;
	
	// Explaining why it failed
	private String errorMessage;

	// blobs in correct order and orientation
	List<QuadBlob> orderedBlobs;

	/**
	 *
	 * @param maxShuffle Maximum number of combinations of squares it will try when looking for a target. Try 500.
	 * @param gridCols Number of squares wide the grid is. Target dependent.
	 * @param gridRows Number of squares tall the grid is. Target dependent.
	 */
	public DetectSpacedSquareGrid(int maxShuffle,
								  int gridCols, int gridRows) {
		this.gridCols = gridCols;
		this.gridRows = gridRows;
		this.maxShuffle = maxShuffle;

		detectBlobs = new DetectQuadBlobsBinary(20*4,0.25,gridCols*gridRows);
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

		// given all the blobs, only consider N at one time until a valid target is found
		return shuffleToFindTarget(squaresPruned);
	}

	/**
	 * Shuffles through all the different possible sets of blobs to find the valid target
	 *
	 * @return true of it worked
	 */
	private boolean shuffleToFindTarget( List<QuadBlob> squares ) {
		
		int N = gridCols * gridRows;
		Shuffle<QuadBlob> shuffle = new Shuffle<QuadBlob>(squares,N);

//		System.out.println("------------------------------------"+squares.size()+"  N "+N);
//		System.out.println("Total Shuffles: "+shuffle.numShuffles());
		if( shuffle.numShuffles() > maxShuffle ) {
			return fail("Not enough blobs detected");
		}

		List<QuadBlob> list = new ArrayList<QuadBlob>();

		int num = 0;
		boolean success = false;
		while( true ) {
			shuffle.getList(list);

			// assumes that all the items in the list are part of a target
			// see if it fails internal sanity checks
			try {
				List<QuadBlob> subgraph = ConnectGridSquares.copy(list);
				extractTarget = new PutTargetSquaresIntoOrder();
				extractTarget.process(subgraph);

				// additional validation checks
				if( validateTarget(subgraph)) {
					success = true;
					break;
				}
			} catch (InvalidCalibrationTarget invalidTarget) {
//				System.out.println(invalidTarget.getMessage());
			}

			try {
				shuffle.shuffle();
			} catch (Shuffle.ExhaustedException e) {
				break;
			}
		}
		
		if( !success )
			return fail("No target found after shuffling");
		return true;
	}
	
	/**
	 * Performs additional validation checks to make sure a valid target was found
	 */
	private boolean validateTarget( List<QuadBlob> list ) {
		// see if each blob's center is inside the bounding quadrilateral
		List<Point2D_I32> quad_I = extractTarget.getQuadrilateral();

		Polygon2D_F64 poly = new Polygon2D_F64(4);
		for( int i = 0; i < 4; i++ ) {
			Point2D_I32 p = quad_I.get(i);
			poly.vertexes[3-i].set(p.x, p.y);
		}

		Point2D_F64 center = new Point2D_F64();
		for( QuadBlob b : list ) {
			center.set(b.center.x,b.center.y);

			if( !Intersection2D_F64.containConvex(poly,center)) {
//				System.out.println("Failed EXTRA");
				return false;
			}
		}

		// see if it is transposed from what it should be and if it is, transpose the output
		orderedBlobs = extractTarget.getBlobsOrdered();
		if( gridRows != gridCols && extractTarget.getNumCols() == gridRows) {
			orderedBlobs = UtilCalibrationGrid.transposeOrdered(orderedBlobs, gridRows, gridCols);
		}

		return true;
	}

	/**
	 * Returns corner points in quadrilateral that bounds all the target points, approximately.
	 */
	public List<Point2D_I32> getTargetQuadrilateral() {
		if( extractTarget == null )
			return null;
		return extractTarget.getQuadrilateral();
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

	public List<QuadBlob> getSquaresUnordered() {
		return squares;
	}

	public List<QuadBlob> getSquaresOrdered() {
		return orderedBlobs;
	}

	public List<QuadBlob> getSquaresBad() {
		return detectBlobs.getInvalid();
	}

	public int getNumberOfLabels() {
		return detectBlobs.getNumLabels();
	}
}
