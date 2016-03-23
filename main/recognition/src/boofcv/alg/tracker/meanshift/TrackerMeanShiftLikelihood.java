/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.meanshift;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.sparse.SparseImageSample_F32;
import georegression.struct.shapes.Rectangle2D_I32;
import georegression.struct.shapes.RectangleLength2D_I32;

/**
 * <p>
 * Mean-shift [1] based tracker which tracks the target inside a likelihood image using a flat rectangular kernel
 * of fixed size. The likelihood for each pixel is computed using {@link SparseImageSample_F32}.  How that
 * model is computed is not specified by this class, but is often based on color.  For sake of efficiency, the
 * likelihood for a pixel is only computed as needed.
 * </p>
 *
 * <p>
 * This algorithm can run very fast and works well when the target being tracked is visually distinctive from
 * the background and largely composed of one color.  It can't handle changes in scale or shape of the target,
 * which does limit its applications.
 * </p>
 *
 * <p>
 * [1] Yizong Chen, "Mean Shift, Mode Seeking, and Clustering" IEEE Trans. Pattern Analysis and Machine Intelligence,
 * VOL. 17, NO. 8, August 1995<br>
 * </p>
 *
 * @author Peter Abeles
 */
public class TrackerMeanShiftLikelihood<T extends ImageBase> {

	// likelihood model for the target being tracked
	private SparseImageSample_F32<T> targetModel;

	// image used to store the likelihood
	private GrayF32 pdf = new GrayF32(1,1);
	// current location of the target
	private RectangleLength2D_I32 location = new RectangleLength2D_I32();

	// rectangle inside of PDF which has been modified.  Used to minimize writing to the image.  probably
	// premature optimization
	private Rectangle2D_I32 dirty = new Rectangle2D_I32();

	// maximum number of iterations
	private int maxIterations;

	// if the total sum of the likelihood drops below this value it is assumed that the target has been lost
	private float minimumSum;
	// fraction of initial likelihood sum which minimumSum is set to
	private float minFractionDrop;

	// if true the tracker has failed
	private boolean failed;

	/**
	 * Configures tracker
	 *
	 * @param targetModel Target used to model the target's likelihood
	 * @param maxIterations Maximum number of iterations.  try 20
	 * @param minFractionDrop If the likelihood drops below its initial value by this fraction the track is
	 *                           assumed to be lost
	 */
	public TrackerMeanShiftLikelihood(PixelLikelihood<T> targetModel, int maxIterations, float minFractionDrop) {
		this.targetModel = targetModel;
		this.maxIterations = maxIterations;
		this.minFractionDrop = minFractionDrop;
	}

	/**
	 * Specifies the initial target location so that it can learn its description
	 * @param image Image
	 * @param initial Initial target location and the mean-shift bandwidth
	 */
	public void initialize( T image , RectangleLength2D_I32 initial ) {
		if( !image.isInBounds(initial.x0,initial.y0) )
			throw new IllegalArgumentException("Initial rectangle is out of bounds!");
		if( !image.isInBounds(initial.x0+initial.width,initial.y0+initial.height) )
			throw new IllegalArgumentException("Initial rectangle is out of bounds!");


		pdf.reshape(image.width,image.height);
		ImageMiscOps.fill(pdf,-1);

		location.set(initial);

		// massage the rectangle so that it has an odd width and height
		// otherwise it could experience a bias when localizing
		location.width += 1-location.width%2;
		location.height += 1-location.height%2;

		failed = false;

		// compute the initial sum of the likelihood so that it can detect when the target is no longer visible
		minimumSum = 0;
		targetModel.setImage(image);

		for( int y = 0; y < initial.height; y++ ) {
			for( int x = 0; x < initial.width; x++ ) {
				minimumSum += targetModel.compute(x+initial.x0,y+initial.y0);
			}
		}
		minimumSum *= minFractionDrop;
	}

	/**
	 * Updates the target's location in the image by performing a mean-shift search.  Returns if it was
	 * successful at finding the target or not.  If it fails once it will need to be re-initialized
	 *
	 * @param image Most recent image in the sequence
	 * @return true for success or false if it failed
	 */
	public boolean process( T image ) {

		if( failed )
			return false;

		targetModel.setImage(image);

		// mark the region where the pdf has been modified as dirty
		dirty.set(location.x0, location.y0, location.x0 + location.width, location.y0 + location.height);
		// compute the pdf inside the initial rectangle
		updatePdfImage(location.x0 , location.y0 , location.x0+location.width , location.y0+location.height);

		// current location of the target
		int x0 = location.x0;
		int y0 = location.y0;

		// previous location of the target in the most recent iteration
		int prevX = x0;
		int prevY = y0;

		// iterate until it converges or reaches the maximum number of iterations
		for( int i = 0; i < maxIterations; i++ ) {

			// compute the weighted centroid using the likelihood function
			float totalPdf = 0;
			float sumX = 0;
			float sumY = 0;

			for( int y = 0; y < location.height; y++ ) {
				int indexPdf = pdf.startIndex + pdf.stride*(y+y0) + x0;
				for( int x = 0; x < location.width; x++ ) {
					float p = pdf.data[indexPdf++];

					totalPdf += p;
					sumX += (x0+x)*p;
					sumY += (y0+y)*p;
				}
			}

			// if the target isn't likely to be in view, give up
			if( totalPdf <= minimumSum ) {
				failed = true;
				return false;
			}

			// Use the new center to find the new top left corner, while rounding to the nearest integer
			x0 = (int)(sumX/totalPdf-location.width/2+0.5f);
			y0 = (int)(sumY/totalPdf-location.height/2+0.5f);

			// make sure it doesn't go outside the image
			if( x0 < 0 )
				x0 = 0;
			else if( x0 >= image.width-location.width )
				x0 = image.width-location.width;

			if( y0 < 0 )
				y0 = 0;
			else if( y0 >= image.height-location.height )
				y0 = image.height-location.height;

			// see if it has converged
			if( x0 == prevX && y0 == prevY )
				break;

			// save the previous location
			prevX = x0;
			prevY = y0;

			// update the pdf
			updatePdfImage(x0,y0,x0+location.width,y0+location.height);
		}

		// update the output
		location.x0 = x0;
		location.y0 = y0;

		// clean up the image for the next iteration
		ImageMiscOps.fillRectangle(pdf,-1,dirty.x0,dirty.y0,dirty.x1-dirty.x0,dirty.y1-dirty.y0);

		return true;
	}

	/**
	 * Computes the PDF only inside the image as needed amd update the dirty rectangle
	 */
	protected void updatePdfImage(  int x0 , int y0 , int x1 , int y1 ) {

		for( int y = y0; y < y1; y++ ) {
			int indexOut = pdf.startIndex + pdf.stride*y + x0;
			for( int x = x0; x < x1; x++ , indexOut++ ) {

				if( pdf.data[indexOut] < 0 )
					pdf.data[indexOut] = targetModel.compute(x, y);
			}
		}

		// update the dirty region
		if( dirty.x0 > x0 )
			dirty.x0 = x0;
		if( dirty.y0 > y0 )
			dirty.y0 = y0;
		if( dirty.x1 < x1 )
			dirty.x1 = x1;
		if( dirty.y1 < y1 )
			dirty.y1 = y1;
	}

	/**
	 * Current location of target in the image
	 * @return rectangle containing the target
	 */
	public RectangleLength2D_I32 getLocation() {
		return location;
	}

	/**
	 * If true the tracker has filed
	 */
	public boolean isFailed() {
		return failed;
	}
}
