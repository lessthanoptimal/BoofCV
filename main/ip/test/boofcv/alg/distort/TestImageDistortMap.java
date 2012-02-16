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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.affine.Affine2D_F32;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestImageDistortMap {

	@Test
	public void compare() {

		Affine2D_F32 affine = new Affine2D_F32(1,2,3,4,5,6);
		PixelTransformAffine_F32 tran = new PixelTransformAffine_F32(affine);

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		ImageBorder<ImageFloat32> border = FactoryImageBorder.value(ImageFloat32.class,1);

//		ImageDistortCache
	}
}
