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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.HoughTransformBinary;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineParametric2D_F32;

import java.util.List;

/**
 * Converts {@link HoughTransformBinary} into {@link DetectLine}
 *
 * @author Peter Abeles
 * @see HoughTransformBinary
 * @see boofcv.alg.feature.detect.line.HoughParametersPolar
 */
@SuppressWarnings({"NullAway.Init"})
public class HoughBinary_to_DetectLine<I extends ImageGray<I>, D extends ImageGray<D>> implements DetectLine<I> {
	HoughTransformBinary hough;
	// binary image with lines marked in it
	GrayU8 binary = new GrayU8(1, 1);

	// Compute a binary image from the input
	InputToBinary<I> binarization;

	// Minimum edge intensity to be used when computing edge mask
	public float thresholdEdge = 20.0f;
	// If non-maximum suppression is used when computing edge mask
	public boolean nonMaxSuppression = true;

	// computes image gradient
	ImageGradient<I, D> gradient;
	D derivX, derivY;

	// storage for gradient intensity image
	GrayF32 edgeIntensity = new GrayF32(1, 1);
	GrayF32 suppressed = new GrayF32(1, 1);

	public HoughBinary_to_DetectLine( HoughTransformBinary hough, InputToBinary<I> thresholder ) {
		this.hough = hough;
		this.binarization = thresholder;
	}

	public HoughBinary_to_DetectLine( HoughTransformBinary hough, ImageGradient<I, D> gradient ) {
		this.hough = hough;
		this.gradient = gradient;
		this.derivX = gradient.getDerivativeType().createImage(1, 1);
		this.derivY = gradient.getDerivativeType().createImage(1, 1);
	}

	@Override
	public List<LineParametric2D_F32> detect( I input ) {

		if (isEdge()) {
			gradient.process(input, derivX, derivY);
			GGradientToEdgeFeatures.intensityAbs(derivX, derivY, edgeIntensity);
			if (nonMaxSuppression) {
				GGradientToEdgeFeatures.nonMaxSuppressionCrude4(edgeIntensity, derivX, derivY, suppressed);
				ThresholdImageOps.threshold(suppressed, binary, thresholdEdge, false);
			} else {
				ThresholdImageOps.threshold(edgeIntensity, binary, thresholdEdge, false);
			}
		} else {
			binarization.process(input, binary);
		}

		hough.transform(binary);
		return hough.getLinesMerged();
	}

	@Override
	public ImageType<I> getInputType() {
		return binarization.getInputType();
	}

	/**
	 * true if the edge is being used to compute a binary image
	 */
	public boolean isEdge() {
		return derivX != null;
	}

	public HoughTransformBinary getHough() {
		return hough;
	}

	public InputToBinary<I> getBinarization() {
		return binarization;
	}

	public GrayU8 getBinary() {
		return binary;
	}
}
