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
import boofcv.alg.feature.disparity.DisparitySelectRect_S32;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class FactoryStereoDisparity {


	public static StereoDisparity<ImageUInt8,ImageUInt8>
	rectWinnerTakeAll( int maxDisparity,
					   int regionRadiusX, int regionRadiusY ,
					   double maxPerPixelError ,
					   int validateRtoL ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		DisparitySelectRect_S32<ImageUInt8> select =
				FactoryStereoDisparityAlgs.selectDisparity_U8((int)maxError,validateRtoL);

		DisparityScoreSadRect_U8<ImageUInt8> alg =
				FactoryStereoDisparityAlgs.scoreDisparitySadRect(
						maxDisparity,regionRadiusX,regionRadiusY,select);

		return new WrapDisparitySadRect(alg);
	}
}
