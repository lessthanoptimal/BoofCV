/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.misc;

import gecv.struct.ImageRectangle;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestGecvMiscOps {
	@Test
	public void boundRectangleInside() {
		ImageUInt8 image = new ImageUInt8(20,25);

		checkBound(-2,-3,5,6,0,0,5,6,image);
		checkBound(16,15,22,26,16,15,20,25,image);
		checkBound(0,0,20,25,0,0,20,25,image);
	}

	private void checkBound( int x0, int y0, int x1 , int y1,
							 int ex0, int ey0, int ex1, int ey1 ,
							 ImageBase image )
	{
		ImageRectangle a = new ImageRectangle(x0,y0,x1,y1);
		GecvMiscOps.boundRectangleInside(image,a);
		assertEquals(ex0,a.x0);
		assertEquals(ey0,a.y0);
		assertEquals(ex1,a.x1);
		assertEquals(ey1,a.y1);

	}
}
