/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background.moving;

import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.homography.Homography2D_F32;

/**
 * @author Peter Abeles
 */
public class TestBackgroundMovingGaussian_MS extends GenericBackgroundMovingGaussianChecks
{
	public TestBackgroundMovingGaussian_MS() {
		imageTypes.add(ImageType.ms(2, ImageUInt8.class));
		imageTypes.add(ImageType.ms(3, ImageUInt8.class));
		imageTypes.add(ImageType.ms(3, ImageFloat32.class));
	}

	@Override
	public <T extends ImageBase> BackgroundModelMoving<T, Homography2D_F32>
	create(ImageType<T> imageType) {
		PointTransformHomography_F32 transform = new PointTransformHomography_F32();
		BackgroundMovingGaussian_MS alg =
				new BackgroundMovingGaussian_MS(0.05f,16,transform, TypeInterpolate.BILINEAR,imageType);
		alg.setInitialVariance(12);
		return alg;
	}
}