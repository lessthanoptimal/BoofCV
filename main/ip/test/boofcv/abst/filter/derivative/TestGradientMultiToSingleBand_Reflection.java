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

package boofcv.abst.filter.derivative;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGradientMultiToSingleBand_Reflection {

	/**
	 * Pass in a simple method and see if it is invoked correctly
	 */
	@Test
	public void expected() {

		try {
			Method m = getClass().getMethod("helper",Planar.class,Planar.class,GrayF32.class,GrayF32.class);

			GradientMultiToSingleBand_Reflection alg = new GradientMultiToSingleBand_Reflection(
					m, ImageType.pl(3,GrayF32.class),GrayF32.class);

			assertTrue(alg.getInputType().getFamily() == ImageType.Family.PLANAR );
			assertTrue(alg.getOutputType() == GrayF32.class);

			Planar<GrayF32> inX = new Planar<>(GrayF32.class,10,12,3);
			Planar<GrayF32> inY = new Planar<>(GrayF32.class,10,12,3);
			GrayF32 outX = new GrayF32(10,12);
			GrayF32 outY = new GrayF32(10,12);

			alg.process(inX,inY,outX,outY);
			assertEquals(2,outX.data[5],1e-4f);
			assertEquals(3,outY.data[5],1e-4f);

		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static void helper(Planar<GrayF32> inX , Planar<GrayF32> inY , GrayF32 outX , GrayF32 outY )
	{
		outX.data[5] = 2;
		outY.data[5] = 3;
	}
}