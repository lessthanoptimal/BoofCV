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

package boofcv.alg.filter.binary;

import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestLinearContourLabelChang2004 {

	public static byte[] TEST1 = new byte[]
				   {0,0,0,0,0,0,0,1,0,0,0,1,1,
					0,0,0,0,0,0,0,1,0,0,0,1,1,
					0,0,0,0,0,0,0,1,0,0,1,1,0,
					0,0,0,0,0,0,0,0,1,1,1,1,0,
					0,0,1,0,0,0,0,0,1,1,1,0,0,
					0,0,1,0,0,0,1,1,1,1,1,0,0,
					1,1,1,1,1,1,1,1,1,1,0,0,0,
					0,0,0,1,1,1,1,1,0,0,0,0,0};

	public static byte[] TEST2 = new byte[]
				   {0,0,1,0,0,0,0,1,0,0,0,0,0,
					0,1,0,1,0,0,1,0,0,1,0,0,0,
					0,0,1,0,0,1,0,1,0,1,1,1,0,
					0,0,0,0,1,0,0,0,1,1,1,1,0,
					0,0,1,0,1,0,0,0,1,0,0,0,0,
					0,0,0,0,1,0,1,1,1,0,1,1,0,
					1,1,1,0,0,1,0,0,1,0,0,1,0,
					0,0,0,1,1,1,1,1,0,0,0,0,0};


	@Test
	public void test1() {
		ImageUInt8 input = new ImageUInt8(13,8);
		input.data = TEST1;

		ImageSInt32 labeled = new ImageSInt32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004();
		alg.process(input,labeled);

		assertEquals(1,alg.getContours().size);
		// TODO check the contour
	}

	@Test
	public void test2() {
		ImageUInt8 input = new ImageUInt8(13,8);
		input.data = TEST2;

		ImageSInt32 labeled = new ImageSInt32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004();
		alg.process(input,labeled);

		assertEquals(4,alg.getContours().size);
		// TODO check the contour
	}

	/**
	 * Creates a list of every pixel with the specified label that is on the contour.  Removes duplicate points
	 * in the found contour.  Sees if the two lists are equivalent.
	 */
	private void checkContour() {

	}
}
