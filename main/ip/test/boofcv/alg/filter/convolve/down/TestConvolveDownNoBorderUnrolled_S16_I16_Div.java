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
import boofcv.struct.image.GrayS16;


/**
 * @author Peter Abeles
 */
public class TestConvolveDownNoBorderUnrolled_S16_I16_Div extends StandardConvolveUnrolledTests {

	public TestConvolveDownNoBorderUnrolled_S16_I16_Div() {
		this.numUnrolled = GenerateConvolveDownNoBorderUnrolled.numUnrolled;
		this.target = ConvolveDownNoBorderUnrolled_S16_I16_Div.class;
		this.param1D = new Class<?>[]{Kernel1D_I32.class, GrayS16.class, GrayI16.class , int.class , int.class};
		this.param2D = new Class<?>[]{Kernel2D_I32.class, GrayS16.class, GrayI16.class , int.class , int.class};
	}

}
