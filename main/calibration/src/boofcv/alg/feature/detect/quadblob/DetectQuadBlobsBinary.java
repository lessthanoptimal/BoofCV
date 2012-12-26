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

package boofcv.alg.feature.detect.quadblob;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Detects blobs inside a binary image which are at least approximate a quadrilateral crudely.
 * Blobs have a minimum contour size and side ratio.  If the number of blobs is known before
 * hand processing is reduced by specifying that minimum number.
 * </p>
 *
 * <p>
 * For each blob that it finds it will find the approximate location of the four corners.  Other
 * information is computed for each blob such as its contour and side length. Blobs are removed
 * if they touch the image border and have a hole inside.
 * </p>
 *
 * @author Peter Abeles
 */
public class DetectQuadBlobsBinary {

	// given a blob it finds the 4 corners in the blob
	FindQuadCorners cornerFinder = new FindQuadCorners();
	
	// smallest allowed size of a blob;s contour
	private int minContourSize;

	// maximum different between smallest and largest side in a candidate square
	private double polySideRatio;

	// minimum number of expected blobs
	private int minimumBlobCount;

	// labeled blobs blobs found in the binary image
	ImageSInt32 labeledBlobs = new ImageSInt32(1,1);
	int numLabels;

	// list of blobs where are declared to be valid quadrilaterals
	List<QuadBlob> squares;
	// list of blobs that are declared as not valid
	List<QuadBlob> squaresBad;
	
	// explanation for why it failed
	String message;

	/**
	 * Constructor and specify pruning parameters
	 * 
	 * @param minContourSize Minimum allowed pixels in blob contour.
	 * @param polySideRatio Prune threshold based on side length. 0 to 1. Typically 0.25
	 * @param minimumBlobCount Stop processing if fewer than this number of blobs have been detected
	 */
	public DetectQuadBlobsBinary(int minContourSize, 
								 double polySideRatio, 
								 int minimumBlobCount) 
	{
		this.minContourSize = minContourSize;
		this.polySideRatio = polySideRatio;
		this.minimumBlobCount = minimumBlobCount;
	}

	/**
	 * Detects quadrilateral like blobs in the binary image.  If the minimum number of blobs
	 * have not been detected then it will fail.  Use {@link #getMessage()} for more information
	 * of the failure.
	 * 
	 * @param binary Binary image being processed.
	 * @return True if it found the minimum number of blobs or false if it did not.
	 */
	public boolean process( ImageUInt8 binary ) {
		// initialize data structures
		squaresBad = new ArrayList<QuadBlob>();
		labeledBlobs.reshape(binary.width, binary.height);
		
		// find blobs
		numLabels = BinaryImageOps.labelBlobs8(binary, labeledBlobs);

		// See if there are enough blobs to continue processing
		if( numLabels < minimumBlobCount)
			return fail("Not enough blobs detected");

		//remove blobs with holes
		numLabels = removeBlobsHoles(binary, labeledBlobs,numLabels);

		// remove blobs that touch the image border
		numLabels = filterTouchEdge(labeledBlobs,numLabels);

		// find their contours
		List<List<Point2D_I32>> contours = BinaryImageOps.labelEdgeCluster4(labeledBlobs,numLabels,null);

		// create  list of squares and find an initial estimate of their corners
		squares = new ArrayList<QuadBlob>();
		for( List<Point2D_I32> l : contours ) {
			if( l.size() < minContourSize ) {
				// this might be a bug or maybe an artifact of detecting with 8 versus 4-connect
				continue;
			}
			List<Point2D_I32> corners = cornerFinder.process(l);
			if( corners.size() == 4 )
				squares.add(new QuadBlob(l, corners));
		}

		// remove blobs which are not like a polygon at all
		filterNotPolygon(squares);
		if( squares.size() < minimumBlobCount)
			return fail("Too few valid squares");

		return true;
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

		return counts-1;
	}

	/**
	 * Set the value of any blob which touches the image border to zero.  Then
	 * relabel the binary image.
	 */
	private int filterTouchEdge(ImageSInt32 labeled , int numLabels ) {
		int value[] = new int[ numLabels + 1 ];
		for( int i = 0; i < value.length; i++ ) {
			value[i] = i;
		}

		for( int y = 0; y < labeled.height; y++ ) {
			int row = labeled.startIndex + labeled.stride*y;
			int rowEnd = row+labeled.width-1;

			value[labeled.data[row]] = 0;
			value[labeled.data[rowEnd]] = 0;
		}

		for( int x = 0; x < labeled.width; x++ ) {
			int top = labeled.startIndex + x;
			int bottom = labeled.startIndex + labeled.stride*(labeled.height-1) + x;

			value[labeled.data[top]] = 0;
			value[labeled.data[bottom]] = 0;
		}

		int count = 1;
		for( int i = 0; i < value.length; i++ ) {
			if( value[i] != 0 ) {
				value[i] = count++;
			}
		}

		// relabel the image to remove blobs with holes inside
		BinaryImageOps.relabel(labeled,value);

		return count-1;
	}

	/**
	 * Looks at the ratio of each blob's side and sees if it could possibly by a square target or not
	 */
	private void filterNotPolygon( List<QuadBlob> squares )
	{
		Iterator<QuadBlob> iter = squares.iterator();

		double d[] = new double[4];

		while( iter.hasNext() ) {
			QuadBlob blob = iter.next();
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
				squaresBad.add(blob);
				iter.remove();
			}
		}
	}
	
	private boolean fail( String message ) {
		this.message = message;
		return false;
	}

	/**
	 * List of valid quadrilateral blobs.
	 */
	public List<QuadBlob> getDetected() {
		return squares;
	}

	/**
	 * List of found blobs that did not pass geometric tests..
	 */
	public List<QuadBlob> getInvalid() {
		return squaresBad;
	}

	/**
	 * Message explaining failure case.  Only valid when process returns false.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Return the labeled blob image.  Useful for debugging.  Use
	 * {@link #getNumLabels()} to get the number of labeled objects inside.
	 */
	public ImageSInt32 getLabeledImage() {
		return labeledBlobs;
	}

	/**
	 * Number of objects in the labeled image.
	 */
	public int getNumLabels() {
		return numLabels;
	}
}
