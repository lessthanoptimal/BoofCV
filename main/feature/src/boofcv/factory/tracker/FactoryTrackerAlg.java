/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.combined.CombinedTrackerScalePoint;
import boofcv.alg.tracker.combined.PyramidKltForCombined;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;

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
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	KltTracker<I, D> klt( KltConfig config, Class<I> imageType , Class<D> derivType )
	{
		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(imageType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(derivType);

		return new KltTracker<I, D>(interpInput, interpDeriv, config);
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
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	PyramidKltTracker<I, D> kltPyramid( KltConfig config,
										Class<I> imageType ,
										Class<D> derivType )
	{
		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(imageType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(derivType);

		KltTracker<I, D> klt = new KltTracker<I, D>(interpInput, interpDeriv, config);
		return new PyramidKltTracker<I, D>(klt);
	}

	/**
	 * Creates a tracker that is a hybrid between KLT and Detect-Describe-Associate (DDA) trackers.
	 *
	 * @see boofcv.alg.tracker.combined.CombinedTrackerScalePoint
	 *
	 * @param detector Feature detector and describer.
	 * @param associate Association algorithm.
	 * @param featureRadiusKlt KLT feature radius
	 * @param pyramidScalingKlt KLT pyramid configuration
	 * @param imageType Input image type.
	 * @param <I> Input image type.
	 * @param <D> Derivative image type.
	 * @param <Desc> Feature description type.
	 * @return Feature tracker
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand, Desc extends TupleDesc>
	CombinedTrackerScalePoint<I,D,Desc> combined( DetectDescribePoint<I,Desc> detector ,
												  AssociateDescription<Desc> associate ,
												  int featureRadiusKlt,
												  int[] pyramidScalingKlt ,
												  Class<I> imageType ,
												  Class<D> derivType )
	{
		KltConfig configKlt =  KltConfig.createDefault();

		PyramidKltForCombined<I,D> klt = new PyramidKltForCombined<I, D>(configKlt,
				featureRadiusKlt,pyramidScalingKlt,imageType,derivType);

		return new CombinedTrackerScalePoint<I, D, Desc>(klt,detector,associate);
	}
}
