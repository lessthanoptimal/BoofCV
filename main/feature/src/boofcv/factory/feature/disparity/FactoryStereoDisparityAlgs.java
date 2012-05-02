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

import boofcv.alg.feature.disparity.*;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

/**
 * Algorithms related to computing the disparity between two rectified stereo images.
 *
 * @author Peter Abeles
 */
public class FactoryStereoDisparityAlgs {

	public static DisparitySelect_S32<ImageUInt8> selectDisparity_U8( int maxError , int tolR2L , double texture) {
		if( maxError < 0 && tolR2L < 0  & texture <= 0 )
			return new SelectRectBasicWta_S32_U8();
		else
			return new SelectRectStandard_S32_U8(maxError,tolR2L,texture);
	}

	public static DisparitySelect_S32<ImageFloat32> selectDisparitySubpixel_F32( int maxError , int tolR2L , double texture) {
		return new SelectRectSubpixel_S32_F32(maxError,tolR2L,texture);
	}

	public static <T extends ImageSingleBand> DisparityScoreSadRect_U8<T>
	scoreDisparitySadRect( int maxDisparity,
						   int regionRadiusX, int regionRadiusY,
						   DisparitySelect_S32<T> computeDisparity)
	{
		return new DisparityScoreSadRect_U8<T>(
				maxDisparity,regionRadiusX,regionRadiusY,computeDisparity);
	}
}
