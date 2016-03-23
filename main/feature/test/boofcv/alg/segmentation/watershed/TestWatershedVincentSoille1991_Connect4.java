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

package boofcv.alg.segmentation.watershed;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestWatershedVincentSoille1991_Connect4 {

	@Test
	public void basic() {
		GrayU8 image = new GrayU8(300,320);
		ImageMiscOps.fill(image, 255);
		image.set(10, 15, 10);
		image.set(100, 200, 50);
		image.set(200, 250, 20);

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image);

		// one region for each minimum plus the watersheds
		assertEquals(4,alg.getTotalRegions());
	}

	@Test
	public void example0() {
		GrayU8 image = new GrayU8(3,4);
		image.data = new byte[]
				{1,5,1,
				 1,5,1,
				 1,5,1,
				 1,5,1};

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image);

		assertEquals(3,alg.getTotalRegions());
		GrayS32 found = alg.getOutput();

		int a = found.get(0,0);
		int b = found.get(2,0);

		assertTrue(a>0);
		assertTrue(b>0);
		assertTrue(a!=b);

		for( int y = 0; y < image.height; y++ ) {
			assertEquals(a,found.get(0,y));
			assertEquals(0,found.get(1,y));
			assertEquals(b,found.get(2,y));
		}
	}

	@Test
	public void example1() {
		GrayU8 image = new GrayU8(4,4);
		image.data = new byte[]
				{1,5,5,1,
				 1,5,5,1,
				 1,5,5,1,
				 1,5,5,1};

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image);

		assertEquals(3,alg.getTotalRegions());
		GrayS32 found = alg.getOutput();

		int a = found.get(0,0);
		int b = found.get(3,0);

		assertTrue(a>0);
		assertTrue(b>0);
		assertTrue(a!=b);

		for( int y = 0; y < image.height; y++ ) {
			assertEquals(a,found.get(0,y));
			assertEquals(a,found.get(1,y));
			assertEquals(b,found.get(2,y));
			assertEquals(b,found.get(3,y));
		}
	}

	@Test
	public void example2() {
		GrayU8 image = new GrayU8(5,4);
		image.data = new byte[]
				{5,5,5,5,5,
				 5,1,5,1,5,
				 5,5,5,5,5,
				 5,5,5,5,5};

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image);

		GrayS32 found = alg.getOutput();

		assertEquals(3,alg.getTotalRegions());
		int a = found.get(0,0);
		int b = found.get(4,0);

		assertTrue(a>0);
		assertTrue(b>0);
		assertTrue(a!=b);

		for( int y = 0; y < image.height; y++ ) {
			assertEquals(a,found.get(0,y));
			assertEquals(a,found.get(1,y));
			assertEquals(0,found.get(2,y));
			assertEquals(b,found.get(3,y));
			assertEquals(b,found.get(4,y));
		}
	}

	@Test
	public void example4() {
		GrayU8 image = new GrayU8(5,4);
		image.data = new byte[]
				{5,5,5,5,5,
				 5,1,4,2,5,
				 5,5,5,5,5,
				 5,5,5,4,5};

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image);

		GrayS32 found = alg.getOutput();

//		found.print();

		assertEquals(4,alg.getTotalRegions());
		int expected[] = new int[]{
				1,1,0,2,2,
				1,1,0,2,2,
				1,1,0,0,0,
				0,0,3,3,3};

		int index = 0;
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				assertEquals(expected[index++],found.get(x,y));
			}
		}
	}

	@Test
	public void example5() {
		GrayU8 image = new GrayU8(5,4);
		image.data = new byte[] {
				1,1,1,5,5,
				1,1,1,5,5,
				0,1,1,5,5,
				1,1,1,5,5};

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image);

		GrayS32 found = alg.getOutput();

//		found.print();

		assertEquals(2,alg.getTotalRegions());

		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				assertEquals(1,found.get(x,y));
			}
		}
	}

	@Test
	public void exampleSeeds0() {
		GrayU8 image = new GrayU8(4,4);
		image.data = new byte[] {
				1,5,5,1,
				1,5,5,1,
				1,5,5,1,
				1,5,5,1};

		GrayS32 seed = new GrayS32(4,4);
		seed.data = new int[]{
				0,0,0,0,
				1,0,0,0,
				0,0,0,0,
				0,0,0,0};

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image,seed);
		GrayS32 found = alg.getOutput();

//		found.print();

		// the whole thing should be filled with 1
		for( int y = 0; y < image.height; y++ ) {
			for (int x = 0; x < image.width; x++) {
				assertEquals(1,found.get(x,y));
			}
		}
	}

	@Test
	public void exampleSeeds1() {
		GrayU8 image = new GrayU8(4,4);
		image.data = new byte[] {
				1,5,5,1,
				1,5,5,1,
				1,5,5,1,
				1,5,5,1};

		// seed from a value which isn't a local minimum
		GrayS32 seed = new GrayS32(4,4);
		seed.data = new int[]{
				0,0,0,0,
				0,1,0,0,
				0,0,0,0,
				0,0,0,0};

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image,seed);
		GrayS32 found = alg.getOutput();

//		found.print();

		// the whole thing should be filled with 1
		for( int y = 0; y < image.height; y++ ) {
			for (int x = 0; x < image.width; x++) {
				assertEquals(1,found.get(x,y));
			}
		}
	}

	@Test
	public void exampleSeeds2() {
		GrayU8 image = new GrayU8(4,4);
		image.data = new byte[] {
				5,5,5,5,
				5,5,5,5,
				5,5,5,5,
				5,5,5,5};

		// try multiple seeds
		GrayS32 seed = new GrayS32(4,4);
		seed.data = new int[]{
				1,0,0,0,
				0,0,0,0,
				0,0,0,0,
				0,0,0,2};

		int expected[] = new int[]{
				1,1,1,0,
				1,1,0,2,
				1,0,2,2,
				0,2,2,2};

		WatershedVincentSoille1991 alg = new WatershedVincentSoille1991.Connect4();

		alg.process(image,seed);
		GrayS32 found = alg.getOutput();

//		found.print();

		int index = 0;
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				assertEquals(expected[index++],found.get(x,y));
			}
		}
	}
}
