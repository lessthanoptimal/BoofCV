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

package boofcv.alg.filter.misc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestAverageDownSampleOps {

	Random rand = new Random(234);

	/**
	 * Down sample with just two inputs.  Compare to results from raw implementation.
	 */
	@Test
	public void down_2inputs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Class input[] = new Class[]{GrayU8.class, GrayU16.class,GrayF32.class, GrayF64.class};
		Class middle[] = new Class[]{GrayF32.class, GrayF32.class,GrayF32.class, GrayF64.class};

		for (int i = 0; i < input.length; i++) {
			ImageGray in = GeneralizedImageOps.createSingleBand(input[i],17,14);
			ImageGray mid = GeneralizedImageOps.createSingleBand(middle[i],3,14);
			ImageGray found = GeneralizedImageOps.createSingleBand(input[i],3,4);
			ImageGray expected = GeneralizedImageOps.createSingleBand(input[i],3,4);

			GImageMiscOps.fillUniform(in,rand,0,100);

			Method horizontal = ImplAverageDownSample.class.getDeclaredMethod("horizontal",input[i],middle[i]);
			Method vertical = BoofTesting.findMethod(ImplAverageDownSample.class,"vertical",middle[i],input[i]);

			horizontal.invoke(null,in,mid);
			vertical.invoke(null,mid,expected);

			AverageDownSampleOps.down(in,found);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}
}
