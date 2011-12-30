/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Detect square grid calibration targets from gray scale images.  The output is a set of ordered calibration
 * points with the number of rows and columns.  Calibration points are in a row-major ordering. Processing
 * steps:
 * <ol>
 * <li>Create binary image by thresholding using a uniform intensity value</li>
 * <li>Morphological noise reduction</li>
 * <li>Create blob clusters</li>
 * <li>Detect rectangles</li>
 * <li>Order rectangles and extract calibration points.</li>
 * </ol>
 * See comments inside of code for how false positive blobs are removed.
 * <p>
 *
 * @author Peter Abeles
 */
public class DetectCalibrationTarget<T extends ImageSingleBand> {
	// type of gray scale image being processed
	Class<T> imageType;

	// images which store intermediate steps in processing cycle
	ImageUInt8 thresholded = new ImageUInt8(1,1);
	ImageUInt8 binaryA = new ImageUInt8(1,1);
	ImageUInt8 binaryB = new ImageUInt8(1,1);
	ImageSInt32 blobs = new ImageSInt32(1,1);

	// Orders corner points found in the image
	ExtractOrderedTargetPoints extractTarget;

	// smallest allowed size of a blob;s contour
	int minContourSize = 20*4;

	// maximum different between smallest and largest side in a candidate square
	double polySideRatio = 0.25;

	// given a blob it finds the 4 corners in the blob
	FindQuadCorners cornerFinder = new FindQuadCorners(1.5);

	public DetectCalibrationTarget(Class<T> imageType ) {
		this.imageType = imageType;
	}

	/**
	 * Processes the image and detects calibration targets.  If one is found then
	 * true is returned and calibration points are extracted
	 *
	 * @param image Gray scale image being processed
	 * @param binaryThreshold Threshold used to detect blobs
	 * @return True if it was successful and false otherwise
	 */
	public boolean process( T image , int binaryThreshold )
	{
		thresholded.reshape(image.width, image.height);
		binaryA.reshape(image.width,image.height);
		binaryB.reshape(image.width,image.height);
		blobs.reshape(image.width, image.height);

		GThresholdImageOps.threshold(image, thresholded,binaryThreshold,true);

		// filter out small objects
		BinaryImageOps.erode8(thresholded,binaryA);
		BinaryImageOps.erode8(binaryA,binaryB);
		BinaryImageOps.dilate8(binaryB, binaryA);
		BinaryImageOps.dilate8(binaryA,binaryB);

		// find blobs
		int numBlobs = BinaryImageOps.labelBlobs8(binaryB,blobs);

		//remove blobs with holes
		numBlobs = removeBlobsHoles(binaryB,blobs,numBlobs);

		// find their contours
		List<List<Point2D_I32>> contours = BinaryImageOps.labelEdgeCluster4(blobs,numBlobs,null);

		// remove blobs which touch the image edge
		filterTouchEdge(contours,image.width,image.height);

		// create  list of squares and find an initial estimate of their corners
		List<SquareBlob> squares = new ArrayList<SquareBlob>();
		for( List<Point2D_I32> l : contours ) {
			if( l.size() < minContourSize ) {
				// this might be a bug or maybe an artifact of detecting with 8 versus 4-connect
				continue;
			}
			List<Point2D_I32> corners = cornerFinder.process(l);
			squares.add(new SquareBlob(l, corners));
		}
		
		// remove blobs which are not like a polygon at all
		filterNotPolygon(squares);

		// All remaining squares are assumed to be part of the calibration target now
		// Given this set of squares order and extract calibration points
		try {
			extractTarget = new ExtractOrderedTargetPoints();
			extractTarget.process(squares);
		} catch (ExtractOrderedTargetPoints.InvalidTarget invalidTarget) {
			return false;
		}

		return true;
	}

	/**
	 * Returns corner points in quadrilateral that bounds all the target points, approximately.
	 */
	public List<Point2D_I32> getTargetQuadrilateral() {
		return extractTarget.getQuadrilateral();
	}

	/**
	 * Returns all the found calibration points the correct order.
	 */
	public List<Point2D_I32> getCalibrationPoints() {
		return extractTarget.getTargetPoints();
	}
	
	/**
	 * Looks at the ratio of each blob's side and sees if it could possibly by a square target or not
	 */
	private void filterNotPolygon( List<SquareBlob> squares )
	{
		Iterator<SquareBlob> iter = squares.iterator();
		
		double d[] = new double[4];
		
		while( iter.hasNext() ) {
			SquareBlob blob = iter.next();
			List<Point2D_I32> corners = blob.corners;
			Point2D_I32 p1 = corners.get(0);
			Point2D_I32 p2 = corners.get(1);
			Point2D_I32 p3 = corners.get(2);
			Point2D_I32 p4 = corners.get(3);

			d[0] = Math.sqrt(p1.distance2(p2));
			d[1] = Math.sqrt(p2.distance2(p3));
			d[2] = Math.sqrt(p3.distance2(p4));
			d[3] = Math.sqrt(p4.distance2(p1));

			double max = -1;
			double min = Double.MAX_VALUE;
			for( double v : d ) {
				if( v > max ) max = v;
				if( v < min ) min = v;
			}
			
			if( min/max < polySideRatio ) {
				iter.remove();
			}
		}
	}

	/**
	 * Remove contours which touch the image's edge
	 */
	private void filterTouchEdge(List<List<Point2D_I32>> contours , int w , int h ) {
		w--;
		h--;
		
		for( int i = 0; i < contours.size(); ) {
			boolean touched = false;
			for( Point2D_I32 p : contours.get(i)) {
				if( p.x == 0 || p.y == 0 || p.x == w || p.y == h ) {
					contours.remove(i);
					touched = true;
					break;
				}
			}
			if( !touched ) 
				i++;
		}
	}

	/**
	 * Remove blobs with holes and blobs with a contour that is too small.  Holes are detected by
	 * finding contour pixels in each blob.  If more than one set of contours exist then there
	 * must be a hole inside
	 */
	private int removeBlobsHoles( ImageUInt8 binary , ImageSInt32 labeled , int numLabels )
	{
		ImageUInt8 contourImg = new ImageUInt8(labeled.width,labeled.height);
		ImageSInt32 contourBlobs = new ImageSInt32(labeled.width,labeled.height);

		BinaryImageOps.edge8(binary,contourImg);
		int numContours = BinaryImageOps.labelBlobs8(contourImg,contourBlobs);
		List<List<Point2D_I32>> contours = BinaryImageOps.labelToClusters(contourBlobs, numContours, null);

		// see how many complete contours each blob has
		int counter[] = new int[ numLabels + 1 ];
		for( int i = 0; i < numContours; i++ ) {
			List<Point2D_I32> l = contours.get(i);
			Point2D_I32 p = l.get(0);
			int which = labeled.get(p.x,p.y);
			if( l.size() < minContourSize ) {
				// set it to a size larger than one so that it will be zeroed
				counter[which] = 20;
			} else {
				counter[which]++;
			}
		}

		// find the blobs with holes
		counter[0] = 0;
		int counts = 1;
		for( int i = 1; i < counter.length; i++ ) {

			if( counter[i] > 1 )
				counter[i] = 0;
			else if( counter[i] != 0 )
				counter[i] = counts++;
			else
				throw new RuntimeException("BUG!");
		}

		// relabel the image to remove blobs with holes inside
		BinaryImageOps.relabel(labeled,counter);

		return counts;
	}

}
