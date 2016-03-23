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

package boofcv.abst.filter;

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestFilterImageReflection {

	/**
	 * Checks to see if the provided function is invoked and that it returned the correct border
	 */
	@Test
	public void basicTest2() {
		FilterImageReflection filter = new FilterImageReflection(getClass(),"methodDummy",2,3,GrayU8.class,GrayU16.class);

		GrayU8 in = new GrayU8(5,5);
		GrayU16 out = new GrayU16(5,5);
		filter.process(in,out);

		assertEquals(2,filter.getHorizontalBorder());
		assertEquals(3,filter.getVerticalBorder());
		assertTrue(GrayU8.class==filter.getInputType().getImageClass());
		assertTrue(GrayU16.class==filter.getOutputType().getImageClass());
		assertEquals(1,out.get(0,0));
    }

	/**
	 * Some filters have a parameter which specify the number of times it is invoked
	 */
	@Test
	public void basicTest3() {
		FilterImageReflection filter = new FilterImageReflection(getClass(),"methodDummy2",2,3,GrayU8.class,GrayU16.class);

		GrayU8 in = new GrayU8(5,5);
		GrayU16 out = new GrayU16(5,5);
		filter.process(in,out);

		assertEquals(2,filter.getHorizontalBorder());
		assertEquals(3,filter.getVerticalBorder());
		assertTrue(GrayU8.class==filter.getInputType().getImageClass());
		assertTrue(GrayU16.class==filter.getOutputType().getImageClass());
		assertEquals(1,out.get(0,0));
	}

	public static void methodDummy(GrayU8 imgA , GrayU16 imgB ) {
		imgB.set(0,0,1);
	}

	public static void methodDummy2(GrayU8 imgA , int numTimes , GrayU16 imgB ) {
		imgB.set(0,0,1);
	}
}
