/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.disparity;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.feature.disparity.WrapDisparitySadRect;
import boofcv.alg.feature.disparity.DisparityScoreSadRect_U8;
import boofcv.alg.feature.disparity.DisparitySelect_S32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class FactoryStereoDisparity {


	public static <T extends ImageSingleBand> StereoDisparity<T,ImageUInt8>
	regionWta( int maxDisparity,
			   int regionRadiusX, int regionRadiusY ,
			   double maxPerPixelError ,
			   int validateRtoL ,
			   double texture ,
			   Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		DisparitySelect_S32<T> select;

		if( imageType == ImageUInt8.class )
			select = (DisparitySelect_S32)FactoryStereoDisparityAlgs.
					selectDisparity_U8((int) maxError, validateRtoL, texture);
		else
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );

		DisparityScoreSadRect_U8<T> alg =
				FactoryStereoDisparityAlgs.scoreDisparitySadRect(
						maxDisparity,regionRadiusX,regionRadiusY,select);

		return new WrapDisparitySadRect<T,ImageUInt8>(alg);
	}

	public static <T extends ImageSingleBand> StereoDisparity<T,ImageFloat32>
	regionSubpixelWta( int maxDisparity,
					   int regionRadiusX, int regionRadiusY ,
					   double maxPerPixelError ,
					   int validateRtoL ,
					   double texture ,
					   Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		DisparitySelect_S32<T> select;
		if( imageType == ImageUInt8.class )
			select = (DisparitySelect_S32)FactoryStereoDisparityAlgs.
					selectDisparitySubpixel_F32((int) maxError, validateRtoL, texture);
		else
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );


		DisparityScoreSadRect_U8<T> alg =
				FactoryStereoDisparityAlgs.scoreDisparitySadRect(
						maxDisparity,regionRadiusX,regionRadiusY,select);

		return new WrapDisparitySadRect<T,ImageFloat32>(alg);
	}
}
