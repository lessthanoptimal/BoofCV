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

package boofcv.core.image;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.InterleavedF32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestInterleavedImageOps {

	@Test
	public void split2() {
		InterleavedF32 interleaved = new InterleavedF32(2,4,2);
		for( int i = 0; i < interleaved.data.length; i++ ) {
			interleaved.data[i] = i+1;
		}
		GrayF32 a = new GrayF32(2,4);
		GrayF32 b = new GrayF32(2,4);

		InterleavedImageOps.split2(interleaved,a,b);

		for( int y = 0; y < interleaved.height; y++ ) {
			for( int x = 0; x < interleaved.width; x++ ) {
				assertEquals(interleaved.getBand(x,y,0),a.get(x,y),1e-8);
				assertEquals(interleaved.getBand(x,y,1),b.get(x,y),1e-8);
			}
		}
	}

	@Test
	public void merge2() {
		GrayF32 a = new GrayF32(2,4);
		GrayF32 b = new GrayF32(2,4);

		for( int i = 0; i < a.data.length; i++ ) {
			a.data[i] = i*2+1;
			b.data[i] = i*2+2;
		}

		InterleavedF32 interleaved = new InterleavedF32(2,4,2);

		InterleavedImageOps.merge2(a, b, interleaved);

		for( int y = 0; y < interleaved.height; y++ ) {
			for( int x = 0; x < interleaved.width; x++ ) {
				assertEquals(a.get(x,y),interleaved.getBand(x,y,0),1e-8);
				assertEquals(b.get(x,y),interleaved.getBand(x,y,1),1e-8);
			}
		}
	}

}
