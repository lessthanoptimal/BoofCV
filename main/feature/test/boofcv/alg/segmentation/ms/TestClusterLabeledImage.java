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

package boofcv.alg.segmentation.ms;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestClusterLabeledImage {

	int case0[] = new int[] {
			0,0,1,0,1,
			0,0,0,1,1,
			1,2,1,2,1,
			1,2,1,1,2,
			1,1,2,2,2};

	ConnectRule rules[] = new ConnectRule[]{ConnectRule.FOUR,ConnectRule.EIGHT};

	Random rand = new Random(123);

	GrowQueue_I32 counts = new GrowQueue_I32();

	/**
	 * Uniform image given different values.  Should produce an output image of all zeros.
	 */
	@Test
	public void uniform() {

		GrayS32 input = new GrayS32(5,7);
		GrayS32 output = new GrayS32(5,7);

		for( int value = 0; value < 3; value++ ) {
			GImageMiscOps.fill(input,value);

			for( int i = 0; i < rules.length; i++ ) {
				GImageMiscOps.fillUniform(output, rand, 0, 1000);

				ClusterLabeledImage alg = new ClusterLabeledImage(rules[i]);

				alg.process(input,output,counts);

				assertEquals(1,counts.size);
				assertEquals(5*7,counts.get(0));

				for( int index = 0; index < output.data.length; index++ )
					assertEquals(0,output.data[index]);
			}
		}
	}

	/**
	 * Same color used on separate islands.  Each island should have its own color on output
	 */
	@Test
	public void sameColorIslands() {
		GrayS32 input = new GrayS32(5,5);

		input.data = new int[]{
				1,1,0,0,0,
				1,1,0,0,0,
				0,0,0,0,0,
				0,0,1,1,1,
				0,0,1,1,1};

		int expected[] = new int[] {
				0,0,1,1,1,
				0,0,1,1,1,
				1,1,1,1,1,
				1,1,2,2,2,
				1,1,2,2,2};

		for( int i = 0; i < rules.length; i++ ) {
			GrayS32 output = new GrayS32(5,5);

			ClusterLabeledImage alg = new ClusterLabeledImage(rules[i]);
			alg.process(input, output, counts);

			int convert[] = new int[3];
			convert[0] = output.get(0,0);
			convert[1] = output.get(2,0);
			convert[2] = output.get(2,4);

			assertEquals(3,counts.size);
			assertEquals(4,counts.get(convert[0]));
			assertEquals(15,counts.get(convert[1]));
			assertEquals(6,counts.get(convert[2]));

			for( int j = 0; j < output.data.length; j++ ) {
				assertEquals(convert[expected[j]],output.data[j]);
			}
		}
	}

	@Test
	public void case0_connect4() {

		GrayS32 input = new GrayS32(5,5);
		input.data = case0;

		int expected[] = new int[] {
				0,0,2,3,4,
				0,0,0,4,4,
				5,6,1,8,4,
				5,6,1,1,7,
				5,5,7,7,7};

		GrayS32 output = new GrayS32(5,5);

		ClusterLabeledImage alg = new ClusterLabeledImage(ConnectRule.FOUR);
		alg.process(input,output,counts);

		int convert[] = new int[9];
		convert[0] = output.get(0,0);
		convert[1] = output.get(2,2);
		convert[2] = output.get(2,0);
		convert[3] = output.get(3,0);
		convert[4] = output.get(4,0);
		convert[5] = output.get(0,2);
		convert[6] = output.get(1,2);
		convert[7] = output.get(4,3);
		convert[8] = output.get(3,2);

		assertEquals(convert.length,counts.size);
		int sum = 0;
		for( int i = 0; i < counts.size; i++ )
			sum += counts.get(i);
		assertEquals(25,sum);

		for( int j = 0; j < output.data.length; j++ ) {
			assertEquals(convert[expected[j]],output.data[j]);
		}
	}

	@Test
	public void case0_connect8() {

		GrayS32 input = new GrayS32(5,5);
		input.data = case0;

		int expected[] = case0;

		GrayS32 output = new GrayS32(5,5);

		ClusterLabeledImage alg = new ClusterLabeledImage(ConnectRule.EIGHT);
		alg.process(input,output,counts);

		int convert[] = new int[3];
		convert[0] = output.get(0,0);
		convert[1] = output.get(0,2);
		convert[2] = output.get(1,2);

		assertEquals(convert.length,counts.size);
		int sum = 0;
		for( int i = 0; i < counts.size; i++ )
			sum += counts.get(i);
		assertEquals(25,sum);

		for( int j = 0; j < output.data.length; j++ ) {
			assertEquals(convert[expected[j]],output.data[j]);
		}
	}
}
