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

import boofcv.alg.feature.disparity.DisparityScoreSadRect_U8;
import boofcv.alg.feature.disparity.DisparitySelect_S32;
import boofcv.alg.feature.disparity.SelectRectBasicWta_S32_U8;
import boofcv.alg.feature.disparity.SelectRectStandard_S32_U8;
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

	public static DisparityScoreSadRect_U8<ImageUInt8>
	scoreDisparitySadRect( int maxDisparity,
						   int regionRadiusX, int regionRadiusY,
						   DisparitySelect_S32<ImageUInt8> computeDisparity)
	{
		return new DisparityScoreSadRect_U8<ImageUInt8>(
				maxDisparity,regionRadiusX,regionRadiusY,computeDisparity);
	}
}
