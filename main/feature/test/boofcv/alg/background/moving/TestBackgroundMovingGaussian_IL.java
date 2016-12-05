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

package boofcv.alg.background.moving;

import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;
import boofcv.struct.image.InterleavedU8;
import georegression.struct.homography.Homography2D_F32;

/**
 * @author Peter Abeles
 */
public class TestBackgroundMovingGaussian_IL extends GenericBackgroundMovingGaussianChecks
{
	public TestBackgroundMovingGaussian_IL() {
		imageTypes.add(ImageType.il(2, InterleavedU8.class));
		imageTypes.add(ImageType.il(3, InterleavedU8.class));
		imageTypes.add(ImageType.il(3, InterleavedF32.class));
	}

	@Override
	public <T extends ImageBase> BackgroundModelMoving<T, Homography2D_F32>
	create(ImageType<T> imageType) {
		PointTransformHomography_F32 transform = new PointTransformHomography_F32();
		BackgroundMovingGaussian_IL alg =
				new BackgroundMovingGaussian_IL(0.05f,16,transform, InterpolationType.BILINEAR,imageType);
		alg.setInitialVariance(12);
		return alg;
	}
}
