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

package boofcv.alg.shapes.square;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.shapes.SplitMergeLineFitLoop;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class BinarySquareDetector<T extends ImageSingleBand> {

	// Converts the input image into a binary one
	private InputToBinary<T> thresholder;

	// minimum size of a shape's contour
	private double minContourFraction;
	private int minimumContour;
	private double minimumArea;

	// Storage for the binary image
	private ImageUInt8 binary = new ImageUInt8(1,1);

	private LinearContourLabelChang2004 contourFinder = new LinearContourLabelChang2004(ConnectRule.FOUR);
	private ImageSInt32 labeled = new ImageSInt32(1,1);

	// finds the initial polygon around a target candidate
	private SplitMergeLineFitLoop fitPolygon;

	// refines the initial estimate of the quadrilateral around the fiducial
	private FitBinaryQuadrilateralEM fitQuad = new FitBinaryQuadrilateralEM();

	// List of all squares that it finds
	private FastQueue<Quadrilateral_F64> found = new FastQueue<Quadrilateral_F64>(Quadrilateral_F64.class,true);

	// type of input image
	private Class<T> inputType;

	/**
	 * Configures the detector.
	 *
	 * @param thresholder Converts the input image into a binary one
	 * @param fitPolygon Provides a crude polygon fit around a shape
	 * @param minContourFraction Size of minimum contour as a fraction of the input image's width.  Try 0.23
	 * @param inputType Type of input image it's processing
	 */
	protected BinarySquareDetector(InputToBinary<T> thresholder,
								   SplitMergeLineFitLoop fitPolygon,
								   double minContourFraction,
								   Class<T> inputType) {

		this.thresholder = thresholder;
		this.inputType = inputType;
		this.minContourFraction = minContourFraction;
		this.fitPolygon = fitPolygon;

	}

	/**
	 * Specifies the image's intrinsic parameters and target size
	 *
	 * @param width Width of the input image
	 * @param height Height of the input image
	 *
	 */
	public void configure( int width , int height ) {

		// resize storage images
		binary.reshape(width, height);
		labeled.reshape(width, height);

		// adjust size based parameters based on image size
		this.minimumContour = (int)(width*minContourFraction);
		this.minimumArea = Math.pow(this.minimumContour /4.0,2);
	}

	/**
	 * Examines the undistorted gray scake input image for squares.
	 *
	 * @param gray Input image
	 */
	public void process( T gray ) {
		if( binary.width == 0 || binary.height == 0 )
			throw new RuntimeException("Did you call configure() yet? zero width/height");

		found.reset();

		thresholder.process(gray, binary);

		// Find quadrilaterals that could be fiducials
		findCandidateShapes();

		// sub-pixel optimize them
	}

	/**
	 * Finds blobs in the binary image.  Then looks for blobs that meet size and shape requirements.  See code
	 * below for the requirements.  Those that remain are considered to be target candidates.
	 */
	private void findCandidateShapes() {
		// find binary blobs
		contourFinder.process(binary, labeled);

		// find blobs where all 4 edges are lines
		FastQueue<Contour> blobs = contourFinder.getContours();
		for (int i = 0; i < blobs.size; i++) {
			Contour c = blobs.get(i);

			if( c.external.size() >= minimumContour) {
				// ignore shapes which touch the image border
				if( touchesBorder(c.external))
					continue;

				fitPolygon.process(c.external);

				GrowQueue_I32 splits = fitPolygon.getSplits();

				// If there are too many splits it's probably not a quadrilateral
				if( splits.size <= 8 && splits.size >= 4 ) {

					Quadrilateral_F64 q = found.grow();
//					if( !fitQuad.fit(contourUndist.toList(),splits,q) ) {
//						found.removeTail();
//					} else {
//						// remove small and flat out bad shapes
//						double area = q.area();
//						if(UtilEjml.isUncountable(area) || area < minimumArea ) {
//							found.removeTail();
//						}
//					}
				}
			}
		}
	}

	/**
	 * Checks to see if some part of the contour touches the image border.  Most likely cropped
	 */
	protected final boolean touchesBorder( List<Point2D_I32> contour ) {
		int endX = binary.width-1;
		int endY = binary.height-1;

		for (int j = 0; j < contour.size(); j++) {
			Point2D_I32 p = contour.get(j);
			if( p.x == 0 || p.y == 0 || p.x == endX || p.y == endY )
			{
				return true;
			}
		}

		return false;
	}

	public ImageUInt8 getBinary() {
		return binary;
	}


	public Class<T> getInputType() {
		return inputType;
	}

	public static class Result {
		Point2D_F64 corners[] = new Point2D_F64[4];

		public Result() {
			for (int i = 0; i < corners.length; i++) {
				corners[i]= new Point2D_F64();
			}
		}
	}
}
