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

package boofcv.abst.filter.derivative;

import boofcv.alg.filter.derivative.HessianFromGradient;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * @author Peter Abeles
 */
public class TestImageHessian_Reflection extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	/**
	 * See if it throws an exception or not
	 */
	@Test void testNoException() throws NoSuchMethodException {
		GrayF32 derivX = new GrayF32(width, height);
		GrayF32 derivY = new GrayF32(width, height);
		GrayF32 derivXX = new GrayF32(width, height);
		GrayF32 derivYY = new GrayF32(width, height);
		GrayF32 derivXY = new GrayF32(width, height);

		Method m = HessianFromGradient.class.getMethod("hessianSobel", GrayF32.class, GrayF32.class, GrayF32.class, GrayF32.class, GrayF32.class, ImageBorder_F32.class);

		ImageHessian_Reflection<GrayF32> alg = new ImageHessian_Reflection<>(m);

		alg.process(derivX, derivY, derivXX, derivXY, derivYY);
	}
}
