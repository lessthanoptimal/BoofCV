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

package boofcv.alg.filter.convolve.down;

import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayI16;
import boofcv.struct.image.GrayU8;


/**
 * @author Peter Abeles
 */
public class TestConvolveDownNoBorderUnrolled_U8_S16 extends StandardConvolveUnrolledTests {

	public TestConvolveDownNoBorderUnrolled_U8_S16() {
		this.numUnrolled = GenerateConvolveDownNoBorderUnrolled.numUnrolled;
		this.target = ConvolveDownNoBorderUnrolled_U8_I16.class;
		this.param1D = new Class<?>[]{Kernel1D_I32.class, GrayU8.class, GrayI16.class , int.class };
		this.param2D = new Class<?>[]{Kernel2D_I32.class, GrayU8.class, GrayI16.class , int.class };
	}
}
