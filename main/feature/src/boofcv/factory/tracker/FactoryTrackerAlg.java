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

package boofcv.factory.tracker;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.tracker.combined.PyramidKltForCombined;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;

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
	public static <I extends ImageGray, D extends ImageGray>
	KltTracker<I, D> klt( KltConfig config, Class<I> imageType , Class<D> derivType )
	{
		if( config == null )
			config = new KltConfig();
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(imageType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(derivType);

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
	public static <I extends ImageGray, D extends ImageGray>
	PyramidKltTracker<I, D> kltPyramid( KltConfig config,
										Class<I> imageType ,
										Class<D> derivType )
	{
		if( config == null )
			config = new KltConfig();
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(imageType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(derivType);

		KltTracker<I, D> klt = new KltTracker<>(interpInput, interpDeriv, config);
		return new PyramidKltTracker<>(klt);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @see boofcv.alg.tracker.combined.CombinedTrackerScalePoint
	 *
	 * @param detector Feature detector and describer.
	 * @param associate Association algorithm.
	 * @param kltConfig Configuration for KLT
	 * @param imageType Input image type.    @return Feature tracker
	 */
	public static <I extends ImageGray, D extends ImageGray, Desc extends TupleDesc>
	CombinedTrackerScalePoint<I,D,Desc> combined(DetectDescribePoint<I, Desc> detector,
												 AssociateDescription<Desc> associate,
												 PkltConfig kltConfig ,
												 Class<I> imageType,
												 Class<D> derivType)
	{
		if( kltConfig == null)
			kltConfig = new PkltConfig();
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		PyramidKltForCombined<I,D> klt = new PyramidKltForCombined<>(kltConfig.config,
				kltConfig.templateRadius, kltConfig.pyramidScaling, imageType, derivType);

		return new CombinedTrackerScalePoint<>(klt, detector, associate);
	}
}
