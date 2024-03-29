/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImplDescribePointPixelRegion_F32 extends BoofStandardJUnit {

	GrayF32 img = new GrayF32(20,30);

	public TestImplDescribePointPixelRegion_F32() {
		GImageMiscOps.fillUniform(img, rand, 0, 30);
	}

	@Test void inner() {
		BoofTesting.checkSubImage(this, "checkInner", false, img, 4, 6, 7, 5);
		BoofTesting.checkSubImage(this, "checkInner", false, img, 7,3,4,5);
		BoofTesting.checkSubImage(this, "checkInner", false, img, 4,6,2,4);
	}

	public void checkInner(GrayF32 image , int c_x , int c_y , int w , int h ) {
		ImplDescribePointPixelRegion_F32 alg = new ImplDescribePointPixelRegion_F32(w,h);

		TupleDesc_F32 desc = new TupleDesc_F32(alg.getDescriptorLength());
		alg.setImage(image);
		alg.process(c_x,c_y,desc);

		int index = 0;
		int y0 = c_y-h/2;
		int x0 = c_x-w/2;
		for( int y = y0; y < y0+h; y++ ) {
			for( int x = x0; x < x0+w; x++ , index++ ) {
				assertEquals(image.get(x,y),desc.data[index],1e-4);
			}
		}
	}

	@Test void border() {
		BoofTesting.checkSubImage(this, "checkBorder", false, img, 0,0,5,7);
		BoofTesting.checkSubImage(this, "checkBorder", false, img, img.width-1,img.height-1,5,7);
		BoofTesting.checkSubImage(this, "checkBorder", false, img, 100, 200, 5, 7);
	}

	public void checkBorder(GrayF32 image , int c_x , int c_y , int w , int h ) {
		ImplDescribePointPixelRegion_F32 alg = new ImplDescribePointPixelRegion_F32(w,h);

		TupleDesc_F32 desc = new TupleDesc_F32(alg.getDescriptorLength());
		alg.setImage(image);
		alg.process(c_x,c_y,desc);

		int index = 0;
		int y0 = c_y-h/2;
		int x0 = c_x-w/2;
		for( int y = y0; y < y0+h; y++ ) {
			for( int x = x0; x < x0+w; x++ , index++ ) {
				if(image.isInBounds(x,y))
					assertEquals(image.get(x,y),desc.data[index],1e-4);
				else
					assertEquals(0,desc.data[index],1e-4);
			}
		}
	}
}
