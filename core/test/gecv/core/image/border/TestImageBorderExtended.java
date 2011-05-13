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

package gecv.core.image.border;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.core.image.UtilImageFloat32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestImageBorderExtended {

	Random rand = new Random(234);

	int width = 20;
	int height = 25;

	@Test
	public void test_I8() {
		ImageUInt8 img = new ImageUInt8(width,height);
		BasicDrawing_I8.randomize(img,rand);

		ImageBorder_I foo = ImageBorderExtended.wrap(img);

		assertEquals(img.get(1,1),foo.get(1,1));
		assertEquals(img.get(0,0),foo.get(0,0));
		assertEquals(img.get(width-1,height-1),foo.get(width-1,height-1));

		assertEquals(img.get(0,0),foo.get(-1,0));
		assertEquals(img.get(0,0),foo.get(-5,0));
		assertEquals(img.get(0,0),foo.get(0,-1));
		assertEquals(img.get(0,0),foo.get(0,-5));
		assertEquals(img.get(1,0),foo.get(1,-1));
		assertEquals(img.get(0,1),foo.get(-1,1));
		assertEquals(img.get(width-1,height-1),foo.get(width,height));
		assertEquals(img.get(width-1,height-1),foo.get(width+2,height));
		assertEquals(img.get(width-1,height-1),foo.get(width,height+2));
		assertEquals(img.get(width-1,5),foo.get(width+1,5));
		assertEquals(img.get(5,height-1),foo.get(5,height+1));
	}

	@Test
	public void test_F32() {
		ImageFloat32 img = new ImageFloat32(width,height);
		UtilImageFloat32.randomize(img,rand,0,5);

		ImageBorder_F32 foo = ImageBorderExtended.wrap(img);

		assertEquals(img.get(1,1),foo.get(1,1),1e-4f);
		assertEquals(img.get(0,0),foo.get(0,0),1e-4f);
		assertEquals(img.get(width-1,height-1),foo.get(width-1,height-1),1e-4f);

		assertEquals(img.get(0,0),foo.get(-1,0),1e-4f);
		assertEquals(img.get(0,0),foo.get(-5,0),1e-4f);
		assertEquals(img.get(0,0),foo.get(0,-1),1e-4f);
		assertEquals(img.get(0,0),foo.get(0,-5),1e-4f);
		assertEquals(img.get(1,0),foo.get(1,-1),1e-4f);
		assertEquals(img.get(0,1),foo.get(-1,1),1e-4f);
		assertEquals(img.get(width-1,height-1),foo.get(width,height),1e-4f);
		assertEquals(img.get(width-1,height-1),foo.get(width+2,height),1e-4f);
		assertEquals(img.get(width-1,height-1),foo.get(width,height+2),1e-4f);
		assertEquals(img.get(width-1,5),foo.get(width+1,5),1e-4f);
		assertEquals(img.get(5,height-1),foo.get(5,height+1),1e-4f);
	}
}
