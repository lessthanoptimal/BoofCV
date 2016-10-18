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

package boofcv.alg.filter.binary;

import boofcv.alg.filter.binary.impl.CompareToBinaryNaive;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBinaryImageOps {

	Random rand = new Random(234);

	public static byte[] TEST = new byte[]
			{0,0,0,1,1,1,0,0,0,0,0,0,0,
			 0,0,1,1,0,1,1,0,0,0,0,0,0,
			 0,0,1,1,1,1,0,0,0,0,1,1,0,
			 0,0,0,1,1,0,0,0,1,1,1,1,1,
			 0,0,0,0,0,1,0,0,1,1,1,1,0,
			 0,0,0,0,0,1,0,0,0,1,1,0,0,
			 0,0,0,0,0,0,0,0,0,0,0,0,0,
			 0,0,0,0,0,0,0,0,0,0,0,0,0};

	public static int[] EXPECTED8 = new int[]
			{0,0,0,1,1,1,0,0,0,0,0,0,0,
			 0,0,1,1,0,1,1,0,0,0,0,0,0,
			 0,0,1,1,1,1,0,0,0,0,2,2,0,
			 0,0,0,1,1,0,0,0,2,2,2,2,2,
			 0,0,0,0,0,1,0,0,2,2,2,2,0,
			 0,0,0,0,0,1,0,0,0,2,2,0,0,
			 0,0,0,0,0,0,0,0,0,0,0,0,0,
			 0,0,0,0,0,0,0,0,0,0,0,0,0};

	// this is designed to require multiple references in label ancestry to be saved, since otherwise
	// islands will be formed
	public static byte[] TEST2 = new byte[]
			{0,0,0,0,0,0,0,0,0,0,0,0,0,
			 0,0,0,0,0,1,1,1,1,1,1,1,0,
			 0,0,0,1,1,1,1,1,0,0,0,1,0,
			 0,1,1,1,1,1,0,0,0,1,1,1,0,
			 0,0,0,0,0,0,0,1,1,1,1,0,0,
			 0,0,0,0,0,0,0,0,0,0,0,0,0,
			 0,0,0,0,0,0,0,0,0,0,0,0,0,
			 0,0,0,0,0,0,0,0,0,0,0,0,0};

	@Test
	public void logicAnd() {
		GrayU8 image0 = new GrayU8(5,6);
		GrayU8 image1 = new GrayU8(5,6);

		image0.set(0,0,1);
		image0.set(1,1,1);
		image1.set(0,0,0);
		image1.set(1,1,1);

		GrayU8 out = BinaryImageOps.logicAnd(image0,image1,null);
		
		assertEquals(0, out.get(0, 0));
		assertEquals(1,out.get(1,1));
		assertEquals(0,out.get(0,1));
	}
	@Test
	public void logicOr() {
		GrayU8 image0 = new GrayU8(5,6);
		GrayU8 image1 = new GrayU8(5,6);

		image0.set(0,0,1);
		image0.set(1,1,1);
		image1.set(0,0,0);
		image1.set(1,1,1);

		GrayU8 out = BinaryImageOps.logicOr(image0, image1, null);

		assertEquals(1,out.get(0,0));
		assertEquals(1,out.get(1,1));
		assertEquals(0, out.get(0, 1));
	}

	@Test
	public void logicXor() {
		GrayU8 image0 = new GrayU8(5,6);
		GrayU8 image1 = new GrayU8(5,6);

		image0.set(0,0,1);
		image0.set(1,1,1);
		image1.set(0,0,0);
		image1.set(1,1,1);

		GrayU8 out = BinaryImageOps.logicXor(image0, image1, null);

		assertEquals(1,out.get(0,0));
		assertEquals(0,out.get(1,1));
		assertEquals(0, out.get(0, 1));
	}

	@Test
	public void invert() {
		GrayU8 input = new GrayU8(5,6);
		GrayU8 expected = new GrayU8(5,6);

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				boolean a = rand.nextBoolean();
				if( a ) {
					input.set(x,y,1);
					expected.set(x,y,0);
				} else {
					input.set(x,y,0);
					expected.set(x,y,1);
				}
			}
		}

		GrayU8 found = BinaryImageOps.invert(input, null);
		BoofTesting.assertEquals(found,expected,0);
	}
	
	@Test
	public void compareToNaive() {
		CompareToBinaryNaive tests = new CompareToBinaryNaive(true,BinaryImageOps.class);
		tests.performTests(7);
	}

	/**
	 * Checks to see if multiple calls are supported correctly
	 */
	@Test
	public void checkMultipleCalls() throws InvocationTargetException, IllegalAccessException {
		Method methods[] = BinaryImageOps.class.getMethods();

		int numTimes = 0;
		for( Method m : methods ) {
			if( m.getName().contains("erode") || m.getName().contains("dilate")) {
				checkMultipleCalls(m);
				numTimes++;
			}
		}

		assertEquals(4,numTimes);
	}

	public void checkMultipleCalls( Method m ) throws InvocationTargetException, IllegalAccessException {
		GrayU8 input = new GrayU8(10,12);
		ImageMiscOps.fillUniform(input, rand, 0, 1);

		GrayU8 tmp = new GrayU8(10,12);

		GrayU8 found = new GrayU8(10,12);
		GrayU8 expected = new GrayU8(10,12);

		for( int numTimes = 2; numTimes <= 3; numTimes++ ) {
			m.invoke(null, input, numTimes, found);

			expected.setTo(input);
			for( int i = 0; i < numTimes; i++ ) {
				m.invoke(null, expected, 1, tmp);

				GrayU8 a = tmp; tmp = expected; expected = a;
			}

			BoofTesting.assertEquals(found,expected,0);
		}
	}

	/**
	 * Very crude and not exhaustive check of contour
	 */
	@Test
	public void contour() {
		GrayU8 input = new GrayU8(10,12);
		ImageMiscOps.fillRectangle(input,1,2,3,4,5);
		input.set(9,11,1);

		GrayS32 output = new GrayS32(10,12);
		GrayS32 expected = new GrayS32(10,12);
		ImageMiscOps.fillRectangle(expected,1,2,3,4,5);
		expected.set(9,11,2);

		List<Contour> found = BinaryImageOps.contour(input, ConnectRule.FOUR,output);

		assertEquals(2,found.size());
		BoofTesting.assertEquals(expected,output,0);
	}

	@Test
	public void relabel() {
		GrayS32 input = new GrayS32(4,5);
		input.set(0,0,1);
		input.set(1,1,2);
		input.set(2,1,3);

		int convert[]={0,2,3,4};

		BinaryImageOps.relabel(input,convert);

		assertEquals(0,input.get(0,1));
		assertEquals(2,input.get(0,0));
		assertEquals(3,input.get(1,1));
		assertEquals(4,input.get(2,1));
	}

	@Test
	public void labelToBinary() {
		GrayU8 expected = new GrayU8(13,8);
		expected.data = TEST;
		GrayU8 found = new GrayU8(13,8);
		GrayS32 input = new GrayS32(13,8);
		input.data = EXPECTED8;

		BinaryImageOps.labelToBinary(input,found);

		BoofTesting.assertEquals(expected,found,0);
	}

	@Test
	public void labelToBinary_array_boolean() {
		GrayU8 expected = new GrayU8(13,8);
		expected.data = TEST;
		GrayU8 found = new GrayU8(13,8);
		GrayS32 input = new GrayS32(13,8);
		input.data = EXPECTED8;

		boolean selected[] = new boolean[]{false,false,true};

		BinaryImageOps.labelToBinary(input,found,selected);

		for( int i = 0; i < 8; i++ ) {
			for( int j = 0; j < 13; j++ ) {
				if( input.get(j,i) == 2 ) {
					assertEquals(1,found.get(j,i));
				} else {
					assertEquals(0,found.get(j,i));
				}
			}
		}
	}

	@Test
	public void labelToBinary_individual_indexes() {
		GrayU8 expected = new GrayU8(13,8);
		expected.data = TEST;
		GrayU8 found = new GrayU8(13,8);
		GrayS32 input = new GrayS32(13,8);
		input.data = EXPECTED8;

		BinaryImageOps.labelToBinary(input, found, 3,2);

		for( int i = 0; i < 8; i++ ) {
			for( int j = 0; j < 13; j++ ) {
				if( input.get(j,i) == 2 ) {
					assertEquals(1,found.get(j,i));
				} else {
					assertEquals(0,found.get(j,i));
				}
			}
		}
	}

	@Test
	public void labelToClusters() {
		FastQueue<Point2D_I32> queue = new FastQueue<>(16, Point2D_I32.class, true);
		GrayS32 labels = new GrayS32(4,4);
		labels.data = new int[]{
				1,2,3,4,
				5,0,2,2,
				3,4,4,4,
				0,0,0,0};

		List<List<Point2D_I32>> ret = BinaryImageOps.labelToClusters(labels,5,queue);

		assertEquals(5,ret.size());

		assertEquals(1,ret.get(0).size());
		assertEquals(3,ret.get(1).size());
		assertEquals(2,ret.get(2).size());
		assertEquals(4,ret.get(3).size());
		assertEquals(1,ret.get(4).size());
	}
}
