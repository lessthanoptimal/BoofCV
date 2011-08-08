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

package gecv.alg.transform.ii;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.misc.ImageTestingOps;
import gecv.struct.ImageRectangle;
import gecv.struct.image.ImageFloat32;

import java.util.Random;


/**
 *
 * @author Peter Abeles
 */
public class BenchmarkIntegralImage {
	static int width = 640;
	static int height = 480;
	static long TEST_TIME = 1000;

	static ImageFloat32 input = new ImageFloat32(width,height);
	static ImageFloat32 integral = new ImageFloat32(width,height);

	static ImageFloat32 output = new ImageFloat32(width,height);


	public static class ComputeIntegral extends PerformerBase {
		@Override
		public void process() {
			IntegralImageOps.transform(input,integral);
		}
	}

	public static class DerivXX extends PerformerBase {
		@Override
		public void process() {
			DerivativeIntegralImage.derivXX(integral,output,9);
		}
	}

	public static class GenericDerivXX extends PerformerBase {

		ImageRectangle blocks[];
		int scales[];

		public GenericDerivXX() {
			blocks = new ImageRectangle[2];
			scales = new int[]{1,-3};
			blocks[0] = new ImageRectangle(-5,-3,4,2);
			blocks[1] = new ImageRectangle(-2,-3,1,2);

		}

		@Override
		public void process() {
			IntegralImageOps.convolve(integral,blocks,scales,output);
		}
	}


	public static void main(String args[]) {

		Random rand = new Random(234);
		ImageTestingOps.randomize(input, rand, 0, 100);
		IntegralImageOps.transform(input,integral);

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new ComputeIntegral(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new DerivXX(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new GenericDerivXX(), TEST_TIME);
	}
}
