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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
public class BenchmarkBinaryBlobLabeling {

	static final long TEST_TIME = 1000;

	static int imgWidth = 640;
	static int imgHeight = 480;

	static GrayU8 original = new GrayU8(imgWidth, imgHeight);
	static GrayU8 input = new GrayU8(imgWidth, imgHeight);
	static GrayS32 output = new GrayS32(imgWidth, imgHeight);

	public static class NewAlg8 extends PerformerBase {

		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.EIGHT);

		@Override
		public void process() {
			alg.process(input,output);
		}
	}

	public static class NewAlg4 extends PerformerBase {

		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.FOUR);

		@Override
		public void process() {
			alg.process(input,output);
//			System.out.println("new 4 = "+alg.getContours().size);
		}
	}

	public static void main(String args[]) {
		System.out.println("=========  Profile Image Size "+ imgWidth +" x "+ imgHeight  +" ==========");

		Random rand = new Random(234);
		ImageMiscOps.fillUniform(original, rand, 0, 2);

		for( int y = 0; y < original.height; y++ ) {
			for( int x = 0; x < original.width; x++ ) {
				if( x == 0 || y == 0 || x == original.width-1 || y == original.height-1 )
					original.unsafe_set(x,y,0);
			}
		}

		input.setTo(original);

		ProfileOperation.printOpsPerSec(new NewAlg8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NewAlg4(), TEST_TIME);

	}
}
