/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.alg.filter.binary.impl.ImplBinaryBlobLabeling;
import boofcv.alg.filter.binary.impl.TestImplBinaryBlobLabeling;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
	public void compareToNaive() {
		CompareToBinaryNaive tests = new CompareToBinaryNaive(BinaryImageOps.class);
		tests.performTests(7);
	}

	@Test
	public void labelBlobs8() {
		checkLabelBlobs(TEST,EXPECTED8,2,true);
		checkLabelBlobs(TestImplBinaryBlobLabeling.TEST1,TestImplBinaryBlobLabeling.BLOBS8,1,true);
		checkLabelBlobs(TEST2,null,1,true);
	}

	@Test
	public void labelBlobs4() {

		int EXPECTED4[] = EXPECTED8.clone();
		EXPECTED4[4*13+5] = 3;
		EXPECTED4[5*13+5] = 3;

		checkLabelBlobs(TEST,EXPECTED4,3,false);
		checkLabelBlobs(TestImplBinaryBlobLabeling.TEST1,TestImplBinaryBlobLabeling.BLOBS4,2,false);
		checkLabelBlobs(TEST2,null,1,false);
	}

	private void checkLabelBlobs( byte[] inputData , int[] expectedData, int numExpected ,
								  boolean rule8) {
		ImageUInt8 input = new ImageUInt8(13,8);
		input.data = inputData;
		ImageSInt32 found = new ImageSInt32(13,8);
		ImageSInt32 expected = new ImageSInt32(13,8);
		expected.data = expectedData;

		// randomize output data to simulate using the same output multiple times
		GeneralizedImageOps.randomize(found,rand,0,20);

		int numFount = rule8 ? BinaryImageOps.labelBlobs8(input,found) : BinaryImageOps.labelBlobs4(input,found);
		assertEquals(numExpected,numFount);

		if( expectedData != null )
			BoofTesting.assertEquals(expected,found,0);
	}

	@Test
	public void relabel() {
		ImageSInt32 input = new ImageSInt32(4,5);
		input.set(0,0,1);
		input.set(1,1,2);
		input.set(2,1,3);
		ImageSInt32 found = input.clone();

		int convert[]={0,2,3,4};

		ImplBinaryBlobLabeling.relabelBlobs(input,convert);
		BinaryImageOps.relabel(found,convert);

		BoofTesting.assertEquals(input,found,0);
	}

	@Test
	public void labelToBinary() {
		ImageUInt8 expected = new ImageUInt8(13,8);
		expected.data = TEST;
		ImageUInt8 found = new ImageUInt8(13,8);
		ImageSInt32 input = new ImageSInt32(13,8);
		input.data = EXPECTED8;

		BinaryImageOps.labelToBinary(input,found);

		BoofTesting.assertEquals(expected,found,0);
	}

	@Test
	public void labelToBinary_selective() {
		ImageUInt8 expected = new ImageUInt8(13,8);
		expected.data = TEST;
		ImageUInt8 found = new ImageUInt8(13,8);
		ImageSInt32 input = new ImageSInt32(13,8);
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
	public void labelToClusters() {
		FastQueue<Point2D_I32> queue = new FastQueue<Point2D_I32>(16,Point2D_I32.class,true);
		ImageSInt32 labels = new ImageSInt32(4,4);
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

	@Test
	public void labelEdgeCluster4() {
		ImageUInt8 binary = new ImageUInt8(30,30);
		ImageSInt32 labels = new ImageSInt32(30,30);
		ImageUInt8 expected = new ImageUInt8(30,30);
		
		// create a random image and find clusters
		GeneralizedImageOps.randomize(binary,rand,0,1);
		// make sure there are some islands
		for( int i = 0; i < 30; i++ ) {
			binary.set(15,i,0);
			binary.set(i,15,0);
		}
		int numLabels = BinaryImageOps.labelBlobs4(binary,labels);
		
		// extract edges from binary
		BinaryImageOps.edge4(binary,expected);
		// find edge using labeled
		List<List<Point2D_I32>> list = BinaryImageOps.labelEdgeCluster4(labels,numLabels,null);
		
		// makes ure its edges only
		int total = 0;
		for( List<Point2D_I32> l : list ) {
			total += l.size();
			for( Point2D_I32 p : l ) {
				assertTrue(expected.get(p.x,p.y) == 1);
			}
		}

		// should be the same number
		assertEquals(PixelMath.sum(expected),total);
	}
}
