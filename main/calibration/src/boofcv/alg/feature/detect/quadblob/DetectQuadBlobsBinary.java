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

package boofcv.alg.feature.detect.quadblob;

import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.RectangleCorner2D_I32;

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

	// smallest allowed size of a blob's contour
	private int minContourSize;

	// maximum different between smallest and largest side in a candidate square
	private double polySideRatio;

	// minimum number of expected blobs
	private int minimumBlobCount;

	// computes the contour around binary images
	LinearContourLabelChang2004 contourAlg = new LinearContourLabelChang2004(ConnectRule.EIGHT);

	// labeled blobs blobs found in the binary image
	ImageSInt32 labeledBlobs = new ImageSInt32(1,1);
	int numLabels;
	List<Contour> contours = new ArrayList<Contour>();

	// list of blobs where are declared to be valid quadrilaterals
	List<QuadBlob> squares;
	// list of blobs that are declared as not valid
	List<QuadBlob> squaresBad;
	
	// explanation for why it failed
	String message;

	/**
	 * Constructor and specify pruning parameters
	 *
	 * @param minContourSize Minimum allowed pixels in blob contour. Try 10
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
	 * Specify the minimum size of a blob's contour
	 *
	 * @param minContourSize Contour size in pixels
	 */
	public void setMinContourSize(int minContourSize) {
		this.minContourSize = minContourSize;
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
		contourAlg.process(binary,labeledBlobs);
		contours.clear();
		contours.addAll(contourAlg.getContours().toList());
		numLabels = contours.size();

		// See if there are enough blobs to continue processing
		if( contours.size() < minimumBlobCount)
			return fail("Not enough blobs detected");

		//remove blobs with holes
		removeTooSmall();

		removeBadPerimeterRatio();

		// remove blobs that touch the image border
		filterTouchEdge();

		filterDoesNotContainsCenter();

		// create  list of squares and find an initial estimate of their corners
		squares = new ArrayList<QuadBlob>();
		for( Contour c : contours ) {
			List<Point2D_I32> corners = cornerFinder.process(c.external);
			if( corners.size() == 4 )
				squares.add(new QuadBlob(c.external, corners));
		}

		// remove blobs which are not like a polygon at all
		filterNotPolygon(squares);
		if( squares.size() < minimumBlobCount)
			return fail("Too few valid squares");

		return true;
	}

	/**
	 * Remove blobs with external contour that are too small
	 */
	private void removeTooSmall()
	{
		for( int i = 0; i < contours.size(); ) {
			Contour c = contours.get(i);
			if( c.external.size() < minContourSize ) {
				contours.remove(i);
			} else {
				i++;
			}
		}
	}

	RectangleCorner2D_I32 rectangle = new RectangleCorner2D_I32();
	private void removeBadPerimeterRatio()
	{
		for( int i = 0; i < contours.size(); ) {
			Contour c = contours.get(i);
			UtilPolygons2D_I32.bounding(c.external,rectangle);
			int perimeter = (rectangle.getWidth() + rectangle.getHeight())*2;

			boolean remove = rectangle.getWidth() <= 2 || rectangle.getHeight() <= 2;

			if( !remove ) {
				remove = perimeter*1.3 < c.external.size();
			}

			if( remove ) {
				contours.remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * Set the value of any blob which touches the image border to zero.  Then
	 * relabel the binary image.
	 */
	private void filterTouchEdge() {

		int w = labeledBlobs.width-1;
		int h = labeledBlobs.height-1;

		for( int i = 0; i < contours.size();  ) {
			Contour c = contours.get(i);

			boolean touching = false;
			for( Point2D_I32 p : c.external ) {
				if( p.x == 0 || p.y == 0 || p.x == w || p.y == h ) {
					touching = true;
					break;
				}
			}
			if( touching ) {
				contours.remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * Remove shapes which do not contain the center inside their polygon
	 */
	private void filterDoesNotContainsCenter() {

		Polygon2D_F64 polygon = new Polygon2D_F64();
		Point2D_F64 center = new Point2D_F64();

		for( int i = 0; i < contours.size();  ) {
			Contour c = contours.get(i);

			polygon.vertexes.reset();
			double centerX=0,centerY=0;

			for( int j = 0; j < c.external.size(); j++ ) {
				Point2D_I32 p = c.external.get(j);

				centerX += p.x;
				centerY += p.y;

				polygon.vertexes.grow().set(p.x,p.y);
			}

			center.x = centerX /= c.external.size();
			center.y = centerY /= c.external.size();

			if(Intersection2D_F64.containConcave(polygon,center) ) {
				i++;
			} else {
				contours.remove(i);
			}
		}
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

			d[0] = p1.distance(p2);
			d[1] = p2.distance(p3);
			d[2] = p3.distance(p4);
			d[3] = p4.distance(p1);

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
