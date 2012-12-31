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

package boofcv.abst.filter;

import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestFilterImageReflection {

	/**
	 * Checks to see if the provided function is invoked and that it returned the correct border
	 */
    @Test
    public void basicTest() {
		FilterImageReflection filter = new FilterImageReflection(getClass(),"methodDummy",2,3,ImageUInt8.class,ImageUInt16.class);

		ImageUInt8 in = new ImageUInt8(5,5);
		ImageUInt16 out = new ImageUInt16(5,5);
		filter.process(in,out);

		assertEquals(2,filter.getHorizontalBorder());
		assertEquals(3,filter.getVerticalBorder());
		assertTrue(ImageUInt8.class==filter.getInputType());
		assertEquals(1,out.get(0,0));
    }

	public static void methodDummy( ImageUInt8 imgA , ImageUInt16 imgB ) {
		imgB.set(0,0,1);
	}
}
