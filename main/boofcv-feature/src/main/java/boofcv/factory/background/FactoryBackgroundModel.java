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

package boofcv.factory.background;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.alg.background.moving.*;
import boofcv.alg.background.stationary.*;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating implementations of {@link BackgroundModelStationary} and {@link boofcv.alg.background.BackgroundModelMoving}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FactoryBackgroundModel {

	/**
	 * Creates an instance of {@link BackgroundMovingBasic}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase<T>>
	BackgroundStationaryBasic<T> stationaryBasic( ConfigBackgroundBasic config, ImageType<T> imageType ) {

		config.checkValidity();

		if (BoofConcurrency.isUseConcurrent()) {
			// Profiling showed SB_MT to be the same or slower than single thread
			return switch (imageType.getFamily()) {
				case GRAY -> new BackgroundStationaryBasic_SB(config.learnRate, config.threshold, imageType.getImageClass());
				case PLANAR -> new BackgroundStationaryBasic_PL_MT(config.learnRate, config.threshold, imageType);
				case INTERLEAVED -> new BackgroundStationaryBasic_IL_MT(config.learnRate, config.threshold, imageType);
			};
		} else {
			return switch (imageType.getFamily()) {
				case GRAY -> new BackgroundStationaryBasic_SB(config.learnRate, config.threshold, imageType.getImageClass());
				case PLANAR -> new BackgroundStationaryBasic_PL(config.learnRate, config.threshold, imageType);
				case INTERLEAVED -> new BackgroundStationaryBasic_IL(config.learnRate, config.threshold, imageType);
			};
		}
	}

	/**
	 * Creates an instance of {@link BackgroundMovingBasic}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase<T>, Motion extends InvertibleTransform<Motion>>
	BackgroundMovingBasic<T, Motion> movingBasic( ConfigBackgroundBasic config,
												  Point2Transform2Model_F32<Motion> transform, ImageType<T> imageType ) {

		config.checkValidity();

		BackgroundMovingBasic<T, Motion> ret;
		if (BoofConcurrency.isUseConcurrent()) {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundMovingBasic_SB_MT(config.learnRate, config.threshold,
						transform, config.interpolation, imageType.getImageClass());
				case PLANAR -> new BackgroundMovingBasic_PL_MT(config.learnRate, config.threshold,
						transform, config.interpolation, imageType);
				case INTERLEAVED -> new BackgroundMovingBasic_IL_MT(config.learnRate, config.threshold,
						transform, config.interpolation, imageType);
			};
		} else {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundMovingBasic_SB(config.learnRate, config.threshold,
						transform, config.interpolation, imageType.getImageClass());
				case PLANAR -> new BackgroundMovingBasic_PL(config.learnRate, config.threshold,
						transform, config.interpolation, imageType);
				case INTERLEAVED -> new BackgroundMovingBasic_IL(config.learnRate, config.threshold,
						transform, config.interpolation, imageType);
			};
		}

		ret.setUnknownValue(config.unknownValue);
		return ret;
	}

	/**
	 * Creates an instance of {@link BackgroundStationaryGaussian}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase<T>>
	BackgroundStationaryGaussian<T> stationaryGaussian( ConfigBackgroundGaussian config, ImageType<T> imageType ) {
		config.checkValidity();

		BackgroundStationaryGaussian<T> ret;
		if (BoofConcurrency.isUseConcurrent()) {
			// Profiling showed SB_MT was found to be about the same as single thread
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundStationaryGaussian_SB(config.learnRate, config.threshold, imageType.getImageClass());
				case PLANAR -> new BackgroundStationaryGaussian_PL_MT(config.learnRate, config.threshold, imageType);
				case INTERLEAVED -> new BackgroundStationaryGaussian_IL_MT(config.learnRate, config.threshold, imageType);
			};
		} else {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundStationaryGaussian_SB(config.learnRate, config.threshold, imageType.getImageClass());
				case PLANAR -> new BackgroundStationaryGaussian_PL(config.learnRate, config.threshold, imageType);
				case INTERLEAVED -> new BackgroundStationaryGaussian_IL(config.learnRate, config.threshold, imageType);
			};
		}

		ret.setInitialVariance(config.initialVariance);
		ret.setMinimumDifference(config.minimumDifference);
		ret.setUnknownValue(config.unknownValue);

		return ret;
	}

	/**
	 * Creates an instance of {@link BackgroundMovingGaussian}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase<T>, Motion extends InvertibleTransform<Motion>>
	BackgroundMovingGaussian<T, Motion> movingGaussian( ConfigBackgroundGaussian config,
														Point2Transform2Model_F32<Motion> transform,
														ImageType<T> imageType ) {

		config.checkValidity();

		BackgroundMovingGaussian<T, Motion> ret;

		if (BoofConcurrency.isUseConcurrent()) {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundMovingGaussian_SB_MT(config.learnRate, config.threshold,
						transform, config.interpolation, imageType.getImageClass());
				case PLANAR -> new BackgroundMovingGaussian_PL_MT(config.learnRate, config.threshold,
						transform, config.interpolation, imageType);
				case INTERLEAVED -> new BackgroundMovingGaussian_IL_MT(config.learnRate, config.threshold,
						transform, config.interpolation, imageType);
			};
		} else {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundMovingGaussian_SB(config.learnRate, config.threshold,
						transform, config.interpolation, imageType.getImageClass());
				case PLANAR -> new BackgroundMovingGaussian_PL(config.learnRate, config.threshold,
						transform, config.interpolation, imageType);
				case INTERLEAVED -> new BackgroundMovingGaussian_IL(config.learnRate, config.threshold,
						transform, config.interpolation, imageType);
			};
		}

		ret.setInitialVariance(config.initialVariance);
		ret.setMinimumDifference(config.minimumDifference);
		ret.setUnknownValue(config.unknownValue);

		return ret;
	}

	/**
	 * Creates an instance of {@link BackgroundStationaryGmm}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase<T>>
	BackgroundStationaryGmm<T> stationaryGmm( @Nullable ConfigBackgroundGmm config, ImageType<T> imageType ) {

		if (config == null)
			config = new ConfigBackgroundGmm();
		else
			config.checkValidity();

		BackgroundStationaryGmm<T> ret;
		if (BoofConcurrency.isUseConcurrent()) {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundStationaryGmm_SB_MT(config.learningPeriod, config.decayCoefient,
						config.numberOfGaussian, imageType);
				case PLANAR, INTERLEAVED -> new BackgroundStationaryGmm_MB_MT(config.learningPeriod, config.decayCoefient,
						config.numberOfGaussian, imageType);
			};
		} else {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundStationaryGmm_SB(config.learningPeriod, config.decayCoefient,
						config.numberOfGaussian, imageType);
				case PLANAR, INTERLEAVED -> new BackgroundStationaryGmm_MB(config.learningPeriod, config.decayCoefient,
						config.numberOfGaussian, imageType);
			};
		}

		ret.setInitialVariance(config.initialVariance);
		ret.setMaxDistance(config.maxDistance);
		ret.setSignificantWeight(config.significantWeight);
		ret.setUnknownValue(config.unknownValue);

		return ret;
	}

	/**
	 * Creates an instance of {@link BackgroundMovingGmm}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase<T>, Motion extends InvertibleTransform<Motion>>
	BackgroundMovingGmm<T, Motion> movingGmm( @Nullable ConfigBackgroundGmm config,
											  Point2Transform2Model_F32<Motion> transform,
											  ImageType<T> imageType ) {
		if (config == null)
			config = new ConfigBackgroundGmm();
		else
			config.checkValidity();

		BackgroundMovingGmm<T, Motion> ret;

		if (BoofConcurrency.isUseConcurrent()) {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundMovingGmm_SB_MT(config.learningPeriod, config.decayCoefient,
						config.numberOfGaussian, transform, imageType);
				case PLANAR, INTERLEAVED -> new BackgroundMovingGmm_MB_MT(config.learningPeriod, config.decayCoefient,
						config.numberOfGaussian, transform, imageType);
			};
		} else {
			ret = switch (imageType.getFamily()) {
				case GRAY -> new BackgroundMovingGmm_SB(config.learningPeriod, config.decayCoefient,
						config.numberOfGaussian, transform, imageType);
				case PLANAR, INTERLEAVED -> new BackgroundMovingGmm_MB(config.learningPeriod, config.decayCoefient,
						config.numberOfGaussian, transform, imageType);
			};
		}

		ret.setInitialVariance(config.initialVariance);
		ret.setMaxDistance(config.maxDistance);
		ret.setSignificantWeight(config.significantWeight);
		ret.setUnknownValue(config.unknownValue);

		return ret;
	}
}
