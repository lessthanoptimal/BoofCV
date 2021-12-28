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

package boofcv.factory.tracker;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.tracker.ConfigTrackerHybrid;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.hybrid.HybridTrackerScalePoint;
import boofcv.alg.tracker.hybrid.PyramidKltForHybrid;
import boofcv.alg.tracker.klt.ConfigKlt;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating feature trackers algorithms.
 *
 * @author Peter Abeles
 */
public class FactoryTrackerAlg {

	/**
	 * Creates a {@link KltTracker}.
	 *
	 * NOTE: The pyramid's structure is determined by the input pyramid that is processed.
	 *
	 * @param config KLT configuration
	 * @param imageType Type of input image
	 * @param derivType Type of image derivative
	 * @param <I> Input image type.
	 * @param <D> Derivative image type.
	 * @return Tracker
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	KltTracker<I, D> klt( @Nullable ConfigKlt config, Class<I> imageType, Class<D> derivType ) {
		if (config == null)
			config = new ConfigKlt();
		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		InterpolateRectangle<I> interpInput = FactoryInterpolation.bilinearRectangle(imageType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.bilinearRectangle(derivType);

		return new KltTracker<>(interpInput, interpDeriv, config);
	}

	/**
	 * Creates a {@link PyramidKltTracker}.
	 *
	 * NOTE: The pyramid's structure is determined by the input pyramid that is processed.
	 *
	 * @param config KLT configuration
	 * @param imageType Type of input image
	 * @param derivType Type of image derivative
	 * @param <I> Input image type.
	 * @param <D> Derivative image type.
	 * @return Tracker
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	PyramidKltTracker<I, D> kltPyramid( @Nullable ConfigKlt config,
										Class<I> imageType,
										Class<D> derivType ) {
		if (config == null)
			config = new ConfigKlt();
		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		InterpolateRectangle<I> interpInput = FactoryInterpolation.bilinearRectangle(imageType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.bilinearRectangle(derivType);

		KltTracker<I, D> klt = new KltTracker<>(interpInput, interpDeriv, config);
		return new PyramidKltTracker<>(klt);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @param detector Feature detector and describer.
	 * @param associate Association algorithm.
	 * @param kltConfig Configuration for KLT
	 * @param imageType Input image type.  @return Feature tracker
	 * @see HybridTrackerScalePoint
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>, Desc extends TupleDesc<Desc>>
	HybridTrackerScalePoint<I, D, Desc> hybrid( DetectDescribePoint<I, Desc> detector,
												AssociateDescription2D<Desc> associate,
												int tooCloseRadius,
												@Nullable ConfigPKlt kltConfig,
												@Nullable ConfigTrackerHybrid configHybrid,
												Class<I> imageType,
												@Nullable Class<D> derivType ) {
		if (configHybrid == null)
			configHybrid = new ConfigTrackerHybrid();
		if (kltConfig == null)
			kltConfig = new ConfigPKlt();
		if (derivType == null)
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		PyramidKltForHybrid<I, D> klt = new PyramidKltForHybrid<>(kltConfig.config,
				kltConfig.templateRadius, imageType, derivType);

		var tracker = new HybridTrackerScalePoint<>(klt, detector, associate, tooCloseRadius);
		tracker.maxInactiveTracks = configHybrid.maxInactiveTracks;
		return tracker;
	}
}
