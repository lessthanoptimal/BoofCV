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

package boofcv.alg.background.moving;

import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F32;

class TestBackgroundMovingGaussian_SB_MT extends GenericBackgroundMovingThreadsChecks {
	public TestBackgroundMovingGaussian_SB_MT() {
		imageTypes.add(ImageType.single(GrayU8.class));
		imageTypes.add(ImageType.single(GrayF32.class));
	}

	@Override public <T extends ImageBase<T>> BackgroundModelMoving<T, Homography2D_F32>
	create( boolean singleThread, ImageType<T> imageType ) {
		var transform = new PointTransformHomography_F32();
		BackgroundMovingGaussian alg;
		if (singleThread)
			alg = new BackgroundMovingGaussian_SB(0.05f, 16, transform, InterpolationType.BILINEAR, imageType.getImageClass());
		else
			alg = new BackgroundMovingGaussian_SB_MT(0.05f, 16, transform, InterpolationType.BILINEAR, imageType.getImageClass());
		alg.setInitialVariance(12);
		return alg;
	}
}

