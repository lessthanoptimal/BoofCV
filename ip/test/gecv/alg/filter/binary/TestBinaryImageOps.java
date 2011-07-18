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

package gecv.alg.filter.binary;

import gecv.alg.filter.binary.impl.CompareToBinaryNaive;
import gecv.alg.filter.binary.impl.ImplBinaryBlobLabeling;
import gecv.alg.filter.binary.impl.TestImplBinaryBlobLabeling;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;

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

	@Test
	public void compareToNaive() {
		CompareToBinaryNaive tests = new CompareToBinaryNaive(BinaryImageOps.class);
		tests.performTests(7);
	}

	@Test
	public void labelBlobs8() {
		checkLabelBlobs(TEST,EXPECTED8,2,true);
		checkLabelBlobs(TestImplBinaryBlobLabeling.TEST1,TestImplBinaryBlobLabeling.BLOBS8,1,true);
	}

	@Test
	public void labelBlobs4() {

		int EXPECTED4[] = EXPECTED8.clone();
		EXPECTED4[4*13+5] = 3;
		EXPECTED4[5*13+5] = 3;

//		checkLabelBlobs(TEST,EXPECTED4,3,false);
		checkLabelBlobs(TestImplBinaryBlobLabeling.TEST1,TestImplBinaryBlobLabeling.BLOBS4,2,false);
	}

	private void checkLabelBlobs( byte[] inputData , int[] expectedData, int numExpected ,
								   boolean rule8) {
		ImageUInt8 input = new ImageUInt8(13,8);
		input.data = inputData;
		ImageSInt32 found = new ImageSInt32(13,8);
		ImageSInt32 expected = new ImageSInt32(13,8);
		expected.data = expectedData;

		int maxConnect[] = new int[20];

		int numFount = rule8 ? BinaryImageOps.labelBlobs8(input,found,maxConnect) : BinaryImageOps.labelBlobs4(input,found,maxConnect);
		assertEquals(numExpected,numFount);

		GecvTesting.assertEquals(expected,found,0);
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

		GecvTesting.assertEquals(input,found,0);
	}

	@Test
	public void labelToBinary() {
		ImageUInt8 expected = new ImageUInt8(13,8);
		expected.data = TEST;
		ImageUInt8 found = new ImageUInt8(13,8);
		ImageSInt32 input = new ImageSInt32(13,8);
		input.data = EXPECTED8;

		BinaryImageOps.labelToBinary(input,found);

		GecvTesting.assertEquals(expected,found,0);
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
}
