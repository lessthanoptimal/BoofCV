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

package gecv.alg.filter.binary.impl;

import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestImplBinaryBlobLabeling {

	Random rand = new Random(123);

	public static byte[] TEST1 = new byte[]
			{0,0,0,0,0,0,0,1,0,0,0,1,1,
			 0,0,0,0,0,0,0,1,0,0,0,1,1,
			 0,0,0,0,0,0,0,1,0,0,1,1,0,
			 0,0,0,0,0,0,0,0,1,1,1,1,0,
			 0,0,1,0,0,0,0,0,1,1,1,0,0,
			 0,0,1,0,0,0,1,1,1,1,1,0,0,
			 1,1,1,1,1,1,1,1,1,1,0,0,0,
			 0,0,0,1,1,1,1,1,0,0,0,0,0};


	public static int[] EXPECTED4 = new int[]
			{0,0,0,0,0,0,0,1,0,0,0,2,2,
			 0,0,0,0,0,0,0,1,0,0,0,2,2,
			 0,0,0,0,0,0,0,1,0,0,3,3,0,
			 0,0,0,0,0,0,0,0,4,4,4,4,0,
			 0,0,5,0,0,0,0,0,4,4,4,0,0,
			 0,0,5,0,0,0,6,6,6,6,6,0,0,
			 7,7,7,7,7,7,7,7,7,7,0,0,0,
			 0,0,0,7,7,7,7,7,0,0,0,0,0};

	public static int[] EXPECTED8 = new int[]
			{0,0,0,0,0,0,0,1,0,0,0,2,2,
			 0,0,0,0,0,0,0,1,0,0,0,2,2,
			 0,0,0,0,0,0,0,1,0,0,2,2,0,
			 0,0,0,0,0,0,0,0,1,2,2,2,0,
			 0,0,3,0,0,0,0,0,2,2,2,0,0,
			 0,0,3,0,0,0,4,4,4,4,4,0,0,
			 5,5,5,5,5,5,5,5,5,5,0,0,0,
			 0,0,0,5,5,5,5,5,0,0,0,0,0};

	public static int[] BLOBS8 = new int[]
			{0,0,0,0,0,0,0,1,0,0,0,1,1,
			 0,0,0,0,0,0,0,1,0,0,0,1,1,
			 0,0,0,0,0,0,0,1,0,0,1,1,0,
			 0,0,0,0,0,0,0,0,1,1,1,1,0,
			 0,0,1,0,0,0,0,0,1,1,1,0,0,
			 0,0,1,0,0,0,1,1,1,1,1,0,0,
			 1,1,1,1,1,1,1,1,1,1,0,0,0,
			 0,0,0,1,1,1,1,1,0,0,0,0,0};

	public static int[] BLOBS4 = new int[]
			{0,0,0,0,0,0,0,1,0,0,0,2,2,
			 0,0,0,0,0,0,0,1,0,0,0,2,2,
			 0,0,0,0,0,0,0,1,0,0,2,2,0,
			 0,0,0,0,0,0,0,0,2,2,2,2,0,
			 0,0,2,0,0,0,0,0,2,2,2,0,0,
			 0,0,2,0,0,0,2,2,2,2,2,0,0,
			 2,2,2,2,2,2,2,2,2,2,0,0,0,
			 0,0,0,2,2,2,2,2,0,0,0,0,0};

	@Test
	public void quickLabelBlobs8_All() {
		ImageUInt8 input = new ImageUInt8(13,8);
		input.data = TEST1;
		ImageSInt32 found = new ImageSInt32(13,8);
		ImageSInt32 expected = new ImageSInt32(13,8);
		expected.data = EXPECTED8;

		GecvTesting.checkSubImage(this,"checkQuickLabelBlobs8_Naive",true,input,found,expected);
		GecvTesting.checkSubImage(this,"checkQuickLabelBlobs8",true,input,found,expected);
	}

	public void checkQuickLabelBlobs8_Naive(ImageUInt8 input, ImageSInt32 found,
											ImageSInt32 expected)
	{
		int coexist[][] = new int[10][10];
		randomizeCoexist(coexist,rand);

		int numFount = ImplBinaryBlobLabeling.quickLabelBlobs8_Naive(input,found,coexist);
		assertEquals(5,numFount);

		GecvTesting.assertEquals(expected,found,0);
	}

	public void checkQuickLabelBlobs8(ImageUInt8 input, ImageSInt32 found,
									  ImageSInt32 expected)
	{
		int coexist[][] = new int[10][10];
		randomizeCoexist(coexist,rand);

		int numFount = ImplBinaryBlobLabeling.quickLabelBlobs8_Naive(input,found,coexist);
		assertEquals(5,numFount);

		GecvTesting.assertEquals(expected,found,0);
	}

	@Test
	public void quickLabelBlobs4_All() {
		ImageUInt8 input = new ImageUInt8(13,8);
		input.data = TEST1;
		ImageSInt32 found = new ImageSInt32(13,8);
		ImageSInt32 expected = new ImageSInt32(13,8);
		expected.data = EXPECTED4;

		GecvTesting.checkSubImage(this,"checkQuickLabelBlobs4_Naive",true,input,found,expected);
		GecvTesting.checkSubImage(this,"checkQuickLabelBlobs4",true,input,found,expected);
	}

	public void checkQuickLabelBlobs4_Naive(ImageUInt8 input, ImageSInt32 found,
											ImageSInt32 expected)
	{
		int coexist[][] = new int[10][10];
		randomizeCoexist(coexist,rand);

		int numFount = ImplBinaryBlobLabeling.quickLabelBlobs4_Naive(input,found,coexist);
		assertEquals(7,numFount);

		GecvTesting.assertEquals(expected,found,0);
	}

	public void checkQuickLabelBlobs4(ImageUInt8 input, ImageSInt32 found,
									  ImageSInt32 expected)
	{
		int coexist[][] = new int[10][10];
		randomizeCoexist(coexist,rand);

		int numFount = ImplBinaryBlobLabeling.quickLabelBlobs4_Naive(input,found,coexist);
		assertEquals(7,numFount);

		GecvTesting.assertEquals(expected,found,0);
	}

	@Test
	public void relabelBlobs() {
		ImageSInt32 input = new ImageSInt32(4,5);
		input.set(0,0,1);
		input.set(1,1,2);
		input.set(2,1,3);

		int convert[]={0,2,3,4};

		ImplBinaryBlobLabeling.relabelBlobs(input,convert);

		assertEquals(0,input.get(0,1));
		assertEquals(2,input.get(0,0));
		assertEquals(3,input.get(1,1));
		assertEquals(4,input.get(2,1));
	}

	@Test
	public void optimizeCoexistTable() {
		ImageUInt8 input = new ImageUInt8(13,8);
		input.data = TEST1;
		ImageSInt32 found = new ImageSInt32(13,8);

		int coexist[][] = new int[10][10];
		randomizeCoexist(coexist,rand);
		int numBlobs = ImplBinaryBlobLabeling.quickLabelBlobs8_Naive(input,found,coexist);
//		BasicImageIO.print(found);
		ImplBinaryBlobLabeling.optimizeCoexistTable(coexist,numBlobs);

		// there should be only one cluster in this test image for 8-connect
		List<Integer> ids = new ArrayList<Integer>();

		for( int y = 1; y < numBlobs; y++ ) {
			for( int x = 1; x < numBlobs; x++ ) {
				int val = coexist[y][x];
				if( !ids.contains(val) )
					ids.add(val);
			}
		}

		// zero would be included, so subtract one
		assertEquals(1,ids.size()-1);
	}

	@Test
	public void minimizeBlobID() {

		int coexist[][] = new int[5][5];

		coexist[1][1] = 2;
		coexist[2][2] = 2;
		coexist[3][3] = 2;
		coexist[4][4] = 5;

		int table[] = new int[5];

		int newTotal = ImplBinaryBlobLabeling.minimizeBlobID(coexist,4,table);

		assertEquals(2,newTotal);
		assertEquals(0,table[0]);
		assertEquals(1,table[1]);
		assertEquals(1,table[2]);
		assertEquals(1,table[3]);
		assertEquals(2,table[4]);
	}

	public static void randomizeCoexist( int coexist[][] , Random rand ) {
		int N = coexist.length;
		for( int i = 0; i < N; i++ ) {
			for( int j = 0; j < N; j++ ) {
				coexist[i][j] = rand.nextInt(N);
			}
		}
	}
}
