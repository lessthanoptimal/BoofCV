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

package boofcv.factory.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.flow.*;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.flow.*;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.PyramidDiscrete;
import org.jetbrains.annotations.Nullable;

/**
 * Creates implementations of {@link DenseOpticalFlow}.
 *
 * @author Peter Abeles
 */
public class FactoryDenseOpticalFlow {

	/**
	 * Compute optical flow using {@link PyramidKltTracker}.
	 *
	 * @param configKlt Configuration for KLT. If null then default values are used.
	 * @param radius Radius of square region.
	 * @param inputType Type of input image.
	 * @param derivType Type of derivative image. If null then default is used.
	 * @param <I> Input image type.
	 * @param <D> Derivative image type.
	 * @return DenseOpticalFlow
	 * @see DenseOpticalFlowKlt
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	DenseOpticalFlow<I> flowKlt( @Nullable ConfigPKlt configKlt, int radius, Class<I> inputType, Class<D> derivType ) {

		if (configKlt == null) {
			configKlt = new ConfigPKlt();
			configKlt.pyramidLevels = ConfigDiscreteLevels.levels(3);
		}

		if (derivType == null) {
			derivType = GImageDerivativeOps.getDerivativeType(inputType);
		}

		ImageType<I> imagetype = ImageType.single(inputType);
		PyramidDiscrete<I> pyramidA = FactoryPyramid.discreteGaussian(configKlt.pyramidLevels, -1, 2, true, imagetype);
		PyramidDiscrete<I> pyramidB = FactoryPyramid.discreteGaussian(configKlt.pyramidLevels, -1, 2, true, imagetype);

		PyramidKltTracker<I, D> tracker = FactoryTrackerAlg.kltPyramid(configKlt.config, inputType, derivType);
		DenseOpticalFlowKlt<I, D> flowKlt = new DenseOpticalFlowKlt<>(tracker, radius);
		ImageGradient<I, D> gradient = FactoryDerivative.sobel(inputType, derivType);

		return new FlowKlt_to_DenseOpticalFlow<>(flowKlt, gradient, pyramidA, pyramidB, inputType, derivType);
	}

	/**
	 * Creates a pyramidal block
	 *
	 * @param config Configuration for block pyramid
	 * @see boofcv.alg.flow.DenseOpticalFlowBlockPyramid
	 * @see boofcv.alg.flow.UtilDenseOpticalFlow#standardPyramid(int, int, double, double, int, int, Class)
	 */
	public static <T extends ImageGray<T>>
	DenseOpticalFlow<T> region( @Nullable ConfigOpticalFlowBlockPyramid config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigOpticalFlowBlockPyramid();

		DenseOpticalFlowBlockPyramid<T> alg;
		if (imageType == GrayU8.class)
			alg = (DenseOpticalFlowBlockPyramid)new DenseOpticalFlowBlockPyramid.U8(
					config.searchRadius, config.regionRadius, config.maxPerPixelError);
		else if (imageType == GrayF32.class)
			alg = (DenseOpticalFlowBlockPyramid)new DenseOpticalFlowBlockPyramid.F32(
					config.searchRadius, config.regionRadius, config.maxPerPixelError);
		else
			throw new IllegalArgumentException("Unsupported image type " + imageType);

		return new FlowBlock_to_DenseOpticalFlow<>(alg, config.pyramidScale, config.maxPyramidLayers, imageType);
	}

	/**
	 * The original Horn-Schunck algorithm. Only good for very small motions.
	 *
	 * @param config Configuration parameters. If null then default is used.
	 * @param imageType Type of input gray scale image
	 * @return dense optical flow
	 * @see HornSchunck
	 */
	public static <T extends ImageGray<T>, D extends ImageGray<D>>
	DenseOpticalFlow<T> hornSchunck( @Nullable ConfigHornSchunck config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigHornSchunck();

		HornSchunck<T, D> alg;
		if (imageType == GrayU8.class)
			alg = (HornSchunck)new HornSchunck_U8(config.alpha, config.numIterations);
		else if (imageType == GrayF32.class)
			alg = (HornSchunck)new HornSchunck_F32(config.alpha, config.numIterations);
		else
			throw new IllegalArgumentException("Unsupported image type " + imageType);

		return new HornSchunck_to_DenseOpticalFlow<>(alg, ImageType.single(imageType));
	}

	/**
	 * Creates an instance of {@link HornSchunckPyramid}
	 *
	 * @param config Configuration parameters. If null defaults will be used.
	 * @return Dense optical flow implementation of HornSchunckPyramid
	 * @see HornSchunckPyramid
	 */
	public static <T extends ImageGray<T>>
	DenseOpticalFlow<T> hornSchunckPyramid( @Nullable ConfigHornSchunckPyramid config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigHornSchunckPyramid();

		InterpolatePixelS<GrayF32> interpolate =
				FactoryInterpolation.createPixelS(0, 255, config.interpolation, BorderType.EXTENDED, GrayF32.class);

		HornSchunckPyramid<T> alg = new HornSchunckPyramid<>(config, interpolate);

		return new HornSchunckPyramid_to_DenseOpticalFlow<>(alg, imageType);
	}

	public static <T extends ImageGray<T>>
	DenseOpticalFlow<T> broxWarping( @Nullable ConfigBroxWarping config, Class<T> imageType ) {
		if (config == null)
			config = new ConfigBroxWarping();

		InterpolatePixelS<GrayF32> interpolate =
				FactoryInterpolation.createPixelS(0, 255, config.interpolation, BorderType.EXTENDED, GrayF32.class);

		BroxWarpingSpacial<T> alg = new BroxWarpingSpacial<>(config, interpolate);

		return new BroxWarpingSpacial_to_DenseOpticalFlow<>(alg, imageType);
	}
}
