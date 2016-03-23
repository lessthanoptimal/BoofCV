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

package boofcv.alg.feature.detect.edge;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;

import java.util.List;


/**
 * Implementation of canny edge detector.  The canny edge detector detects the edges of objects
 * using a hysteresis threshold.  When scanning the image pixels with edge intensities below
 * the high threshold are ignored.  After a pixel is found that exceeds the high threshold any
 * pixel that is connect to it directly or indirectly just needs to exceed the low threshold.
 *
 * The output from this class can be configured to output a binary edge image and/or a set of contours
 * for each point in the contour image.
 *
 * @author Peter Abeles
 */
public class CannyEdge<T extends ImageGray, D extends ImageGray> {

	// blurs the input image
	private BlurFilter<T> blur;

	// computes the image gradient
	private ImageGradient<T,D> gradient;

	// blurred input image
	private T blurred;

	// image gradient
	private D derivX;
	private D derivY;

	// edge intensity
	private GrayF32 intensity = new GrayF32(1,1);
	protected GrayF32 suppressed = new GrayF32(1,1);
	// edge direction in radians
	private GrayF32 angle = new GrayF32(1,1);
	// quantized direction
	private GrayS8 direction = new GrayS8(1,1);
	// work space
	private GrayU8 work = new GrayU8(1,1);

	// different algorithms for performing hysteresis thresholding
	protected HysteresisEdgeTracePoints hysteresisPts; // saves a list of points
	protected HysteresisEdgeTraceMark hysteresisMark; // just marks a binary image

	/**
	 * Specify internal algorithms and behavior.
	 *
	 * @param blur Initial blur applied to image.
	 * @param gradient Computes the image gradient.
	 * @param saveTrace Should it save a list of points that compose the objects contour/trace?
	 */
	public CannyEdge(BlurFilter<T> blur, ImageGradient<T, D> gradient, boolean saveTrace) {
		this.blur = blur;
		this.gradient = gradient;

		Class<T> imageType = blur.getInputType().getImageClass();

		blurred = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		derivX = gradient.getDerivativeType().createImage(1,1);
		derivY = gradient.getDerivativeType().createImage(1, 1);

		if( saveTrace ) {
			hysteresisPts = new HysteresisEdgeTracePoints();
		} else {
			hysteresisMark = new HysteresisEdgeTraceMark();
		}
	}

	/**
	 * <p>
	 * Runs a canny edge detector on the input image given the provided thresholds.  If configured to save
	 * a list of trace points then the output image is optional.
	 * </p>
	 * <p>
	 * NOTE: Input and output can be the same instance, if the image type allows it.
	 * </p>
	 * @param input Input image. Not modified.
	 * @param threshLow Lower threshold. &ge; 0.
	 * @param threshHigh Upper threshold. &ge; 0.
	 * @param output (Might be option) Output binary image.  Edge pixels are marked with 1 and everything else 0.
	 */
	public void process(T input , float threshLow, float threshHigh , GrayU8 output ) {

		if( threshLow < 0 || threshHigh < 0 )
			throw new IllegalArgumentException("Threshold must be >= zero!");

		if( hysteresisMark != null ) {
			if( output == null )
				throw new IllegalArgumentException("An output image must be specified when configured to mark edge points");
		}

		// setup internal data structures
		blurred.reshape(input.width,input.height);
		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);
		intensity.reshape(input.width,input.height);
		suppressed.reshape(input.width,input.height);
		angle.reshape(input.width,input.height);
		direction.reshape(input.width,input.height);
		work.reshape(input.width,input.height);

		// run canny edge detector
		blur.process(input,blurred);
		gradient.process(blurred, derivX, derivY);
		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, intensity);
		GGradientToEdgeFeatures.direction(derivX, derivY, angle);
		GradientToEdgeFeatures.discretizeDirection4(angle, direction);
		GradientToEdgeFeatures.nonMaxSuppression4(intensity, direction, suppressed);

		performThresholding(threshLow, threshHigh, output);
	}

	protected void performThresholding(float threshLow, float threshHigh, GrayU8 output) {
		if( hysteresisPts != null ) {
			hysteresisPts.process(suppressed,direction,threshLow,threshHigh);

			// if there is an output image write the contour to it
			if( output != null ) {
				ImageMiscOps.fill(output, 0);
				for( EdgeContour e : hysteresisPts.getContours() ) {
					for( EdgeSegment s : e.segments)
						for( Point2D_I32 p : s.points )
							output.unsafe_set(p.x,p.y,1);
				}
			}
		} else {
			hysteresisMark.process(suppressed,direction,threshLow,threshHigh,output);
		}
	}

	public List<EdgeContour> getContours() {
		return hysteresisPts.getContours();
	}
}
