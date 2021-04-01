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

package boofcv.factory.feature.describe;

import boofcv.abst.feature.convert.ConvertTupleDesc;
import boofcv.abst.feature.describe.*;
import boofcv.abst.feature.orientation.ConfigOrientation2;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating implementations of {@link DescribePointRadiusAngle}.
 *
 * @author Peter Abeles
 */
public class FactoryDescribePoint {
	/**
	 * Factory function for creating many different types of region descriptors
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ImageGray<T>, TD extends TupleDesc<TD>>
	DescribePoint<T, TD> generic( ConfigDescribePoint config, ImageType<T> imageType ) {
		Class<T> imageClass = imageType.getImageClass();

		DescribePoint<T, TD> ret = switch (config.descriptors.type) {
			case SURF_FAST -> (DescribePoint<T, TD>)surfFast(
					config.descriptors.surfFast, config.orientation, config.radius, imageClass);
			case SURF_STABLE -> (DescribePoint<T, TD>)surfStable(
					config.descriptors.surfStability, config.orientation, config.radius, imageClass);
			default -> {
				DescribePointRadiusAngle<T, TD> radAng =
						FactoryDescribePointRadiusAngle.generic(config.descriptors, imageType);
				Class integralType = GIntegralImageOps.getIntegralType(imageType.getImageClass());
				OrientationIntegral orientationII =
						FactoryOrientation.genericIntegral(config.orientation, integralType);
				OrientationImage<T> orientation =
						FactoryOrientation.convertImage(orientationII, imageType.getImageClass());
				yield new DescribeRadiusAngle_Point<>(radAng, orientation, config.radius);
			}
		};

		// See if it's in the native format and no need to modify the descriptor
		if (config.convert.outputData == ConfigConvertTupleDesc.DataType.NATIVE)
			return ret;

		// Descriptor is going to be modified, create the converter then wrap the algorithm
		int dof = ret.createDescription().size();
		ConvertTupleDesc<?, TD> converter = FactoryConvertTupleDesc.generic(config.convert, dof, ret.getDescriptionType());
		return new DescribePointConvertTuple(ret, converter);
	}

	public static <T extends ImageGray<T>, II extends ImageGray<II>>
	DescribeSurf_Point<T, II> surfFast( @Nullable ConfigSurfDescribe.Fast configSurf,
										ConfigOrientation2 configOrientation,
										double regionRadius, Class<T> imageType ) {
		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		DescribePointSurf<II> alg = FactoryDescribeAlgs.surfSpeed(configSurf, integralType);
		OrientationIntegral<II> orientationII = FactoryOrientation.genericIntegral(configOrientation, integralType);

		return new DescribeSurf_Point<>(alg, orientationII, regionRadius, imageType);
	}

	public static <T extends ImageGray<T>, II extends ImageGray<II>>
	DescribeSurf_Point<T, II> surfStable( @Nullable ConfigSurfDescribe.Stability config,
										  ConfigOrientation2 configOrientation,
										  double regionRadius, Class<T> imageType ) {
		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		DescribePointSurf<II> alg = FactoryDescribeAlgs.surfStability(config, integralType);
		OrientationIntegral<II> orientationII = FactoryOrientation.genericIntegral(configOrientation, integralType);

		return new DescribeSurf_Point<>(alg, orientationII, regionRadius, imageType);
	}
}
