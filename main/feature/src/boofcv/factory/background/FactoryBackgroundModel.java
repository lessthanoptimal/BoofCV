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

package boofcv.factory.background;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.alg.background.moving.*;
import boofcv.alg.background.stationary.*;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;

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
	public static <T extends ImageBase>
	BackgroundStationaryBasic<T> stationaryBasic( ConfigBackgroundBasic config , ImageType<T> imageType ) {

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
	public static <T extends ImageBase, Motion extends InvertibleTransform<Motion>>
	BackgroundMovingBasic<T,Motion> movingBasic(ConfigBackgroundBasic config ,
												Point2Transform2Model_F32<Motion> transform, ImageType<T> imageType ) {

		config.checkValidity();

		switch( imageType.getFamily() ) {
			case GRAY:
				return new BackgroundMovingBasic_SB(config.learnRate,config.threshold,
						transform,config.interpolation,imageType.getImageClass());

			case PLANAR:
				return new BackgroundMovingBasic_PL(config.learnRate,config.threshold,
						transform,config.interpolation,imageType);

			case INTERLEAVED:
				return new BackgroundMovingBasic_IL(config.learnRate,config.threshold,
						transform,config.interpolation,imageType);
		}

		throw new IllegalArgumentException("Unknown image type");
	}

	/**
	 * Creates an instance of {@link BackgroundStationaryGaussian}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase>
	BackgroundStationaryGaussian<T> stationaryGaussian( ConfigBackgroundGaussian config , ImageType<T> imageType ) {

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

		return ret;
	}

	/**
	 * Creates an instance of {@link BackgroundMovingGaussian}.
	 *
	 * @param config Configures the background model
	 * @param imageType Type of input image
	 * @return new instance of the background model
	 */
	public static <T extends ImageBase,Motion extends InvertibleTransform<Motion>>
	BackgroundMovingGaussian<T,Motion> movingGaussian( ConfigBackgroundGaussian config ,
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

		return ret;
	}
}
