/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Factory for creating implementations of {@link BackgroundModelStationary} and {@link boofcv.alg.background.BackgroundModelMoving}
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryBackgroundModel {

	/**
	 * Creates an instance of {@link BackgroundMovingBasic}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase<T>>
	BackgroundStationaryBasic<T> stationaryBasic(@Nonnull ConfigBackgroundBasic config , ImageType<T> imageType ) {

		config.checkValidity();

		switch( imageType.getFamily() ) {
			case GRAY:
				return new BackgroundStationaryBasic_SB(config.learnRate,config.threshold,imageType.getImageClass());

			case PLANAR:
				return new BackgroundStationaryBasic_PL(config.learnRate,config.threshold,imageType);

			case INTERLEAVED:
				return new BackgroundStationaryBasic_IL(config.learnRate,config.threshold,imageType);
		}

		throw new IllegalArgumentException("Unknown image type");
	}

	/**
	 * Creates an instance of {@link BackgroundMovingBasic}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase<T>, Motion extends InvertibleTransform<Motion>>
	BackgroundMovingBasic<T,Motion> movingBasic(@Nonnull ConfigBackgroundBasic config ,
												Point2Transform2Model_F32<Motion> transform, ImageType<T> imageType ) {

		config.checkValidity();

		BackgroundMovingBasic<T,Motion> ret;

		switch( imageType.getFamily() ) {
			case GRAY:
				ret= new BackgroundMovingBasic_SB(config.learnRate,config.threshold,
						transform,config.interpolation,imageType.getImageClass());
				break;

			case PLANAR:
				ret= new BackgroundMovingBasic_PL(config.learnRate,config.threshold,
						transform,config.interpolation,imageType);
				break;

			case INTERLEAVED:
				ret= new BackgroundMovingBasic_IL(config.learnRate,config.threshold,
						transform,config.interpolation,imageType);
				break;

			default:
				throw new IllegalArgumentException("Unknown image type");
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
	BackgroundStationaryGaussian<T> stationaryGaussian(@Nonnull ConfigBackgroundGaussian config , ImageType<T> imageType ) {

		config.checkValidity();

		BackgroundStationaryGaussian<T> ret;

		switch( imageType.getFamily() ) {
			case GRAY:
				ret = new BackgroundStationaryGaussian_SB(config.learnRate,config.threshold,imageType.getImageClass());
				break;

			case PLANAR:
				ret =  new BackgroundStationaryGaussian_PL(config.learnRate,config.threshold,imageType);
				break;

			case INTERLEAVED:
				ret =  new BackgroundStationaryGaussian_IL(config.learnRate,config.threshold,imageType);
				break;

			default:
				throw new IllegalArgumentException("Unknown image type");
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
	public static <T extends ImageBase<T>,Motion extends InvertibleTransform<Motion>>
	BackgroundMovingGaussian<T,Motion> movingGaussian( @Nonnull ConfigBackgroundGaussian config ,
													   Point2Transform2Model_F32<Motion> transform,
													   ImageType<T> imageType ) {

		config.checkValidity();

		BackgroundMovingGaussian<T,Motion> ret;

		switch( imageType.getFamily() ) {
			case GRAY:
				ret = new BackgroundMovingGaussian_SB(config.learnRate,config.threshold,
						transform,config.interpolation,imageType.getImageClass());
				break;

			case PLANAR:
				ret =  new BackgroundMovingGaussian_PL(config.learnRate,config.threshold,
						transform,config.interpolation,imageType);
				break;

			case INTERLEAVED:
				ret =  new BackgroundMovingGaussian_IL(config.learnRate,config.threshold,
						transform,config.interpolation,imageType);
				break;

			default:
				throw new IllegalArgumentException("Unknown image type");
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
	BackgroundStationaryGmm<T> stationaryGmm(@Nullable ConfigBackgroundGmm config , ImageType<T> imageType ) {

		if( config == null )
			config = new ConfigBackgroundGmm();
		else
			config.checkValidity();

		BackgroundStationaryGmm<T> ret;

		switch( imageType.getFamily() ) {
			case GRAY:
				ret = new BackgroundStationaryGmm_SB(config.learningPeriod,config.decayCoefient,
						config.numberOfGaussian,imageType);
				break;

			case PLANAR:
			case INTERLEAVED:
				ret =  new BackgroundStationaryGmm_MB(config.learningPeriod,config.decayCoefient,
						config.numberOfGaussian,imageType);
				break;

			default:
				throw new IllegalArgumentException("Unknown image type");
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
	public static <T extends ImageBase<T>,Motion extends InvertibleTransform<Motion>>
	BackgroundMovingGmm<T,Motion> movingGmm(@Nullable ConfigBackgroundGmm config ,
											Point2Transform2Model_F32<Motion> transform ,
											ImageType<T> imageType )
	{
		if( config == null )
			config = new ConfigBackgroundGmm();
		else
			config.checkValidity();

		BackgroundMovingGmm<T,Motion> ret;

		switch( imageType.getFamily() ) {
			case GRAY:
				ret = new BackgroundMovingGmm_SB(config.learningPeriod,config.decayCoefient,
						config.numberOfGaussian,transform,imageType);
				break;

			case PLANAR:
			case INTERLEAVED:
				ret = new BackgroundMovingGmm_MB(config.learningPeriod,config.decayCoefient,
						config.numberOfGaussian,transform,imageType);
				break;

			default:
				throw new IllegalArgumentException("Unknown image type");
		}

		ret.setInitialVariance(config.initialVariance);
		ret.setMaxDistance(config.maxDistance);
		ret.setSignificantWeight(config.significantWeight);
		ret.setUnknownValue(config.unknownValue);

		return ret;
	}
}
