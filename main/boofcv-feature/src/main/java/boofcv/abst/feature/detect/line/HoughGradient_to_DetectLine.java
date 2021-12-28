/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.HoughTransformGradient;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineParametric2D_F32;

import java.util.List;

/**
 * Converts {@link HoughTransformGradient} into {@link DetectLine}
 *
 * <p>
 * USAGE NOTES: Blurring the image prior to processing can often improve performance.
 * Results will not be perfect and to detect all the obvious lines in the image several false
 * positives might be returned.
 * </p>
 *
 * @author Peter Abeles
 * @see HoughTransformGradient
 * @see boofcv.alg.feature.detect.line.HoughParametersFootOfNorm
 */
public class HoughGradient_to_DetectLine<I extends ImageGray<I>, D extends ImageGray<D>>
		implements DetectLine<I> {
	HoughTransformGradient<D> hough;
	// computes image gradient
	ImageGradient<I, D> gradient;
	D derivX, derivY;

	// storage for gradient intensity image
	GrayF32 edgeIntensity = new GrayF32(1, 1);
	GrayF32 suppressed = new GrayF32(1, 1);
	// storage for edge binary image
	GrayU8 binary = new GrayU8(1, 1);

	// Minimum edge intensity to be used when computing edge mask
	public float thresholdEdge = 20.0f;
	// If non-maximum suppression is used when computing edge mask
	public boolean nonMaxSuppression = true;

	Class<I> inputType;
	Class<D> derivType;

	public HoughGradient_to_DetectLine( HoughTransformGradient hough, ImageGradient<I, D> gradient,
										Class<I> inputType ) {
		this.hough = hough;
		this.gradient = gradient;
		this.inputType = inputType;
		this.derivType = GImageDerivativeOps.getDerivativeType(inputType);

		derivX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
	}

	@Override
	public List<LineParametric2D_F32> detect( I input ) {
		gradient.process(input, derivX, derivY);
		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, edgeIntensity);
		if (nonMaxSuppression) {
			GGradientToEdgeFeatures.nonMaxSuppressionCrude4(edgeIntensity, derivX, derivY, suppressed);
			ThresholdImageOps.threshold(suppressed, binary, thresholdEdge, false);
		} else {
			ThresholdImageOps.threshold(edgeIntensity, binary, thresholdEdge, false);
		}

		hough.transform(derivX, derivY, binary);

		return hough.getLinesMerged();
	}

	@Override
	public ImageType<I> getInputType() {
		return ImageType.single(inputType);
	}

	public float getThresholdEdge() {
		return thresholdEdge;
	}

	public void setThresholdEdge( float thresholdEdge ) {
		this.thresholdEdge = thresholdEdge;
	}

	public GrayF32 getEdgeIntensity() {
		return edgeIntensity;
	}

	public GrayU8 getBinary() {
		return binary;
	}

	public HoughTransformGradient<D> getHough() {
		return hough;
	}

	public ImageGradient<I, D> getGradient() {
		return gradient;
	}

	public D getDerivX() {
		return derivX;
	}

	public D getDerivY() {
		return derivY;
	}

	public boolean isNonMaxSuppression() {
		return nonMaxSuppression;
	}

	public void setNonMaxSuppression( boolean nonMaxSuppression ) {
		this.nonMaxSuppression = nonMaxSuppression;
	}
}
