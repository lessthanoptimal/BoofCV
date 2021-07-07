/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestInterleavedU8 extends StandardImageInterleavedTests<InterleavedU8> {

	@Override
	public InterleavedU8 createImage(int width, int height, int numBands) {
		return new InterleavedU8(width, height, numBands);
	}

	@Override
	public InterleavedU8 createImage() {
		return new InterleavedU8();
	}

	@Override
	public Number randomNumber() {
		return (byte)rand.nextInt(255);
	}

	@Override
	public Number getNumber( Number value) {
		return value.byteValue() & 0xFF;
	}


	@Test void get24() {
		InterleavedU8 img = new InterleavedU8(2,3,3);
		img.set(0,1,233,16,128);
		img.set(1,1,16,0,200);

		int expected0 = (233<<16) | (16<<8) | (128);
		int expected1 = (16<<16) | (0<<8) | (200);

		assertEquals(expected0,img.get24(0,1));
		assertEquals(expected1,img.get24(1,1));
	}

	@Test void get32() {
		InterleavedU8 img = new InterleavedU8(2,3,4);
		img.set(0,1,208,233,16,128);
		img.set(1,1,217,16,0,200);

		int expected0 = (208<<24) | (233<<16) | (16<<8) | (128);
		int expected1 = (217<<24) | (16<<16)  | (0<<8)  | (200);

		assertEquals(expected0,img.get32(0,1));
		assertEquals(expected1,img.get32(1,1));
	}

	@Test void set32() {
		InterleavedU8 img = new InterleavedU8(2,3,4);


		int expected0 = (208<<24) | (233<<16) | (16<<8) | (128);
		int expected1 = (217<<24) | (16<<16)  | (0<<8)  | (200);

		img.set32(0,1,expected0);
		img.set32(1,1,expected1);

		assertEquals(208,img.getBand(0,1,0));
		assertEquals(233,img.getBand(0,1,1));
		assertEquals(16,img.getBand(0,1,2));
		assertEquals(128,img.getBand(0,1,3));


		assertEquals(217,img.getBand(1,1,0));
		assertEquals(16,img.getBand(1,1,1));
		assertEquals(0,img.getBand(1,1,2));
		assertEquals(200,img.getBand(1,1,3));
	}

	@Test void set24() {
		InterleavedU8 img = new InterleavedU8(2,3,3);

		int expected0 = (233<<16) | (16<<8) | (128);
		int expected1 = (16<<16)  | (0<<8)  | (200);

		img.set24(0,1,expected0);
		img.set24(1,1,expected1);

		assertEquals(233,img.getBand(0,1,0));
		assertEquals(16,img.getBand(0,1,1));
		assertEquals(128,img.getBand(0,1,2));


		assertEquals(16,img.getBand(1,1,0));
		assertEquals(0,img.getBand(1,1,1));
		assertEquals(200,img.getBand(1,1,2));
	}


}
