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

package boofcv.alg.descriptor;

import boofcv.struct.feature.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDescriptorDistance extends BoofStandardJUnit {
	@Test void euclidean_F64() {
		var a = new TupleDesc_F64(5);
		var b = new TupleDesc_F64(5);

		a.data =new double[]{1,2,3,4,5};
		b.data =new double[]{2,-1,7,-8,10};

		assertEquals(13.964, DescriptorDistance.euclidean(a, b), 1e-2);
	}

	@Test void euclideanSq_F64() {
		var a = new TupleDesc_F64(5);
		var b = new TupleDesc_F64(5);

		a.data =new double[]{1,2,3,4,5};
		b.data =new double[]{2,-1,7,-8,10};

		assertEquals(195, DescriptorDistance.euclideanSq(a, b), 1e-4);
	}

	@Test void euclideanSq_F32() {
		var a = new TupleDesc_F32(5);
		var b = new TupleDesc_F32(5);

		a.data =new float[]{1,2,3,4,5};
		b.data =new float[]{2,-1,7,-8,10};

		assertEquals(195, DescriptorDistance.euclideanSq(a, b), 1e-4);
	}

	@Test void euclideanSq_U8() {
		var a = new TupleDesc_U8(5);
		var b = new TupleDesc_U8(5);

		a.data =new byte[]{1,120,3,4,5};
		b.data =new byte[]{2,(byte)200,7,9,10};

		assertEquals(6467, DescriptorDistance.euclideanSq(a, b), 1e-4);
	}

	@Test void euclideanSq_S8() {
		var a = new TupleDesc_S8(5);
		var b = new TupleDesc_S8(5);

		a.data =new byte[]{1,-5,-45,5,0};
		b.data =new byte[]{2,100,-93,9,-6};

		assertEquals(13382, DescriptorDistance.euclideanSq(a, b), 1e-4);
	}


	@Test void correlation() {
		var a = new TupleDesc_F64(5);
		var b = new TupleDesc_F64(5);

		a.data =new double[]{1,2,3,4,5};
		b.data =new double[]{2,-1,7,-8,10};

		assertEquals(39, DescriptorDistance.correlation(a, b), 1e-2);
	}

	@Test void ncc() {
		var a = new NccFeature(5);
		var b = new NccFeature(5);

		a.sigma = 12;
		b.sigma = 7;
		a.data =new double[]{1,2,3,4,5};
		b.data =new double[]{2,-1,7,-8,10};

		assertEquals(0.46429/5.0,DescriptorDistance.ncc(a, b),1e-2);
	}

	/**
	 * When a correctly computed NCC descriptor is compared against itself the distance should be one
	 */
	@Test void ncc_self_distance() {
		var a = new NccFeature(5);
		for(int i = 0; i < a.data.length; i++ )
			a.data[i] = i*i;

		double mean = 0;
		for(int i = 0; i < a.data.length; i++ )
			mean += a.data[i];
		mean /= a.size();

		double sigma = 0;
		for(int i = 0; i < a.data.length; i++ ) {
			double d = a.data[i] -= mean;
			sigma += d*d;
		}
		sigma /= a.size();

		a.mean = mean;
		a.sigma = Math.sqrt(sigma);

		assertEquals(1,DescriptorDistance.ncc(a, a),1e-2);
	}

	@Test void sad_U8() {
		var a = new TupleDesc_U8(5);
		var b = new TupleDesc_U8(5);

		a.data =new byte[]{1,2,3,4,(byte)200};
		b.data =new byte[]{(byte)245,2,6,3,6};

		assertEquals(442, DescriptorDistance.sad(a, b), 1e-2);
	}

	@Test void sad_S8() {
		var a = new TupleDesc_S8(5);
		var b = new TupleDesc_S8(5);

		a.data =new byte[]{1,2,3,4,5};
		b.data =new byte[]{-2,2,-3,3,6};

		assertEquals(11, DescriptorDistance.sad(a, b), 1e-2);
	}

	@Test void sad_F32() {
		var a = new TupleDesc_F32(5);
		var b = new TupleDesc_F32(5);

		a.data =new float[]{ 0.1f ,2     ,3 ,-4.9f ,5};
		b.data =new float[]{-1    ,45.5f ,6 ,3     ,6.01f};

		assertEquals(56.51,DescriptorDistance.sad(a, b),1e-2);
	}

	@Test void sad_F64() {
		var a = new TupleDesc_F64(5);
		var b = new TupleDesc_F64(5);

		a.data =new double[]{ 0.1 ,2    ,3 ,-4.9 ,5};
		b.data =new double[]{-1   ,45.5 ,6 ,3   ,6.01};

		assertEquals(56.51,DescriptorDistance.sad(a, b),1e-2);
	}

	@Test void hamming_I32() {
		var a = new TupleDesc_B(512);
		var b = new TupleDesc_B(512);

		for( int numTries = 0; numTries < 20; numTries++ ) {
			for(int i = 0; i < a.data.length; i++ ) {
				a.data[i] = rand.nextInt();
				b.data[i] = rand.nextInt();
			}

			assertEquals(hamming(a,b),DescriptorDistance.hamming(a, b),1e-4);
		}
	}

	@Test void hamming_int() {
		assertEquals(0,DescriptorDistance.hamming(0));
		assertEquals(1,DescriptorDistance.hamming(0x0800));
		assertEquals(1,DescriptorDistance.hamming(0x0001));
		assertEquals(2,DescriptorDistance.hamming(0x0101));
		assertEquals(4,DescriptorDistance.hamming(0x000F));
		assertEquals(8,DescriptorDistance.hamming(0xF000000F));
	}

	private int hamming( TupleDesc_B a, TupleDesc_B b) {
		int ret = 0;
		for(int i = 0; i < a.data.length; i++ ) {
			ret += hamming(a.data[i],b.data[i]);
		}
		return ret;
	}

	public static int hamming( int a , int b ) {
		int distance = 0;

		// see which bits are different
		int val = a ^ b;

		// brute force hamming distance
		if( (val & 0x00000001) != 0)
			distance++;
		if( (val & 0x00000002) != 0)
			distance++;
		if( (val & 0x00000004) != 0)
			distance++;
		if( (val & 0x00000008) != 0)
			distance++;

		if( (val & 0x00000010) != 0)
			distance++;
		if( (val & 0x00000020) != 0)
			distance++;
		if( (val & 0x00000040) != 0)
			distance++;
		if( (val & 0x00000080) != 0)
			distance++;

		if( (val & 0x00000100) != 0)
			distance++;
		if( (val & 0x00000200) != 0)
			distance++;
		if( (val & 0x00000400) != 0)
			distance++;
		if( (val & 0x00000800) != 0)
			distance++;

		if( (val & 0x00001000) != 0)
			distance++;
		if( (val & 0x00002000) != 0)
			distance++;
		if( (val & 0x00004000) != 0)
			distance++;
		if( (val & 0x00008000) != 0)
			distance++;


		if( (val & 0x00010000) != 0)
			distance++;
		if( (val & 0x00020000) != 0)
			distance++;
		if( (val & 0x00040000) != 0)
			distance++;
		if( (val & 0x00080000) != 0)
			distance++;

		if( (val & 0x00100000) != 0)
			distance++;
		if( (val & 0x00200000) != 0)
			distance++;
		if( (val & 0x00400000) != 0)
			distance++;
		if( (val & 0x00800000) != 0)
			distance++;

		if( (val & 0x01000000) != 0)
			distance++;
		if( (val & 0x02000000) != 0)
			distance++;
		if( (val & 0x04000000) != 0)
			distance++;
		if( (val & 0x08000000) != 0)
			distance++;

		if( (val & 0x10000000) != 0)
			distance++;
		if( (val & 0x20000000) != 0)
			distance++;
		if( (val & 0x40000000) != 0)
			distance++;
		if( (val & 0x80000000) != 0)
			distance++;

		return distance;
	}
}
