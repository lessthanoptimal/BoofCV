/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import java.lang.reflect.Method;


/**
 * @author Peter Abeles
 */
public class TestImageGradient_Reflection {


	int width = 20;
	int height = 30;

	/**
	 * See if it throws an exception or not
	 */
	@Test
	public void testNoException() throws NoSuchMethodException {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 derivX = new ImageFloat32(width,height);
		ImageFloat32 derivY = new ImageFloat32(width,height);

		Method m = GradientSobel.class.getMethod("process",ImageFloat32.class,ImageFloat32.class,ImageFloat32.class, ImageBorder_F32.class);

		ImageGradient_Reflection<ImageFloat32,ImageFloat32> alg = new ImageGradient_Reflection<ImageFloat32,ImageFloat32>(m);

		alg.process(input,derivX,derivY);
	}
}
