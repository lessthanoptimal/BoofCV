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

import boofcv.alg.feature.disparity.DisparityScoreSadRect;
import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.feature.disparity.impl.*;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

/**
 * Algorithms related to computing the disparity between two rectified stereo images.
 *
 * @author Peter Abeles
 */
public class FactoryStereoDisparityAlgs {

	public static DisparitySelect<int[],ImageUInt8> selectDisparity_S32( int maxError , int tolR2L , double texture) {
		if( maxError < 0 && tolR2L < 0  & texture <= 0 )
			return new ImplSelectRectBasicWta_S32_U8();
		else
			return new ImplSelectRectStandard_S32_U8(maxError,tolR2L,texture);
	}

	public static DisparitySelect<float[],ImageUInt8> selectDisparity_F32( int maxError , int tolR2L , double texture) {
		if( maxError < 0 && tolR2L < 0  & texture <= 0 )
			return new ImplSelectRectBasicWta_F32_U8();
		else
			return new ImplSelectRectStandard_F32_U8(maxError,tolR2L,texture);
	}

	public static DisparitySelect<int[],ImageFloat32>
	selectDisparitySubpixel_S32( int maxError , int tolR2L , double texture) {
		return new ImplSelectRectSubpixel.S32_F32(maxError,tolR2L,texture);
	}

	public static DisparitySelect<float[],ImageFloat32>
	selectDisparitySubpixel_F32( int maxError , int tolR2L , double texture) {
		return new ImplSelectRectSubpixel.F32_F32(maxError,tolR2L,texture);
	}

	public static <T extends ImageSingleBand> DisparityScoreSadRect<ImageUInt8,T>
	scoreDisparitySadRect_U8( int maxDisparity,
						   int regionRadiusX, int regionRadiusY,
						   DisparitySelect<int[],T> computeDisparity)
	{
		return new ImplDisparityScoreSadRect_U8<T>(
				maxDisparity,regionRadiusX,regionRadiusY,computeDisparity);
	}

	public static <T extends ImageSingleBand> DisparityScoreSadRect<ImageFloat32,T>
	scoreDisparitySadRect_F32( int maxDisparity,
							  int regionRadiusX, int regionRadiusY,
							  DisparitySelect<float[],T> computeDisparity)
	{
		return new ImplDisparityScoreSadRect_F32<T>(
				maxDisparity,regionRadiusX,regionRadiusY,computeDisparity);
	}
}
