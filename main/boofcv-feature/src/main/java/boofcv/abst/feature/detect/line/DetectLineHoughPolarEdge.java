/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.line;


import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.HoughTransformLinePolar;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Complete process chain for detecting lines along the edges of objects using {@link HoughTransformLinePolar}.
 * A binary image is created using edge intensity and a threshold. This is then feed into the hough transform.
 * Additional processing is done to remove duplicate lines.
 * </p>
 *
 * <p>
 * USAGE NOTES: Blurring the image prior to processing can often improve performance.
 * Results will not be perfect and to detect all the obvious lines in the image several false
 * positives might be returned.
 * </p>
 *
 * @see boofcv.alg.feature.detect.line.HoughTransformLinePolar
 *
 * @author Peter Abeles
 */
public class DetectLineHoughPolarEdge<D extends ImageGray<D>>
	extends DetectLineHoughPolarBinary implements DetectEdgeLines<D>
{
	// used to create binary edge image
	float thresholdEdge;

	// edge intensity image
	GrayF32 intensity = new GrayF32(1,1);

	// detected edge image
	GrayU8 binary = new GrayU8(1,1);

	GrayF32 suppressed = new GrayF32(1,1);
//	GrayF32 angle = new GrayF32(1,1);
//	ImageSInt8 direction = new ImageSInt8(1,1);

	/**
	 * Configures hough line detector.
	 *
	 * @param localMaxRadius Radius for local maximum suppression.  Try 2.
	 * @param minCounts Minimum number of counts for detected line.  Critical tuning parameter and image dependent.
	 * @param resolutionRange Resolution of line range in pixels.  Try 2
	 * @param resolutionAngle Resolution of line angle in radius.  Try PI/180
	 * @param thresholdEdge Edge detection threshold. Try 50.
	 * @param maxLines Maximum number of lines to return. If &le; 0 it will return them all.
	 */
	public DetectLineHoughPolarEdge(int localMaxRadius,
									int minCounts,
									double resolutionRange ,
									double resolutionAngle ,
									float thresholdEdge,
									int maxLines )
	{
		super(localMaxRadius, minCounts, resolutionRange, resolutionAngle, maxLines);
		this.thresholdEdge = thresholdEdge;
	}

	@Override
	public void detect( D derivX , D derivY ) {
		InputSanityCheck.checkSameShape(derivX,derivY);
		setInputSize(derivX.width,derivX.height);

		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, intensity);

		// non-max suppression reduces the number of line pixels, reducing the number of false positives
		// When too many pixels are flagged, then more curves randomly cross over in transform space causing
		// false positives

//		GGradientToEdgeFeatures.direction(derivX, derivY, angle);
//		GradientToEdgeFeatures.discretizeDirection4(angle, direction);
//		GradientToEdgeFeatures.nonMaxSuppression4(intensity,direction, suppressed);

		GGradientToEdgeFeatures.nonMaxSuppressionCrude4(intensity,derivX,derivY,suppressed);

		ThresholdImageOps.threshold(suppressed, binary, thresholdEdge, false);

		super.detect(binary);
	}

	public void setInputSize( int width , int height ) {
		super.setInputSize(width, height);

		// see if the input image shape has changed.
		if( intensity.width != width || intensity.height != height ) {
			intensity.reshape(width,height);
			binary.reshape(width, height);
			suppressed.reshape(width, height);
		}
	}

	public GrayF32 getEdgeIntensity() {
		return intensity;
	}

	public GrayU8 getBinary() {
		return binary;
	}

}
