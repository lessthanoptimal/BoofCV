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

package boofcv;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import java.util.Random;


/**
 * <P>
 * Test to see if abstracting a class parameter with generic will slow down the code because
 * it invisibly does a lot of type casting.
 * </p>
 *
 * <p>
 * The answer in Java 1.6 on a Q6600 process appears to be, no it does not.
 * </p>
 *
 * @author Peter Abeles
 */
public class AreGenericsSlower {

	static Random rand = new Random(234);

	static int width = 640;
	static int height = 480;
	static long TEST_TIME = 1000;

	GrayF32 image = new GrayF32(width,height);

	static int r = 2;

	public class RawAlg extends PerformerBase
	{
		RawType alg = new RawType();

		@Override
		public void process() {
			alg.setImage(image);
			alg.process();
		}
	}

	public class GenericsAlg extends PerformerBase
	{
		Derived alg = new Derived();

		@Override
		public void process() {
			alg.setImage(image);
			alg.process();
		}
	}

	private static class RawType
	{
		GrayF32 image;

		public void setImage(GrayF32 image) {
			this.image = image;
		}

		public float process() {
			float total = 0;

			for( int y = r; y < image.height-r; y++ ) {
				for( int x = r; x < image.width-r; x++ ) {
					float conv = 0;
					float polarity = x%2==0 ? 1 : -1;

					for( int i = -r; i < r; i++ ) {
						int index = image.startIndex + (y+i)*image.stride;
						for( int j = -r; j < r; j++ ,index++) {
							conv += polarity*image.data[index];
						}
					}
					total += conv;
				}
			}

			return total;
		}
	}

	private static class Derived extends BaseGeneric<GrayF32>
	{

		@Override
		public float process() {
			float total = 0;

			for( int y = r; y < image.height-r; y++ ) {
				for( int x = r; x < image.width-r; x++ ) {
					float conv = 0;
					float polarity = x%2==0 ? 1 : -1;

					for( int i = -r; i < r; i++ ) {
						int index = image.startIndex + (y+i)*image.stride;
						for( int j = -r; j < r; j++ ,index++) {
							conv += polarity*image.data[index];
						}
					}
					total += conv;
				}
			}

			return total;
		}
	}

	private static abstract class BaseGeneric <T extends ImageGray>
	{
		protected T image;

		public void setImage(T image) {
			this.image = image;
		}

		public abstract float process();
	}

	public void performTest() {
		ImageMiscOps.fillUniform(image,rand,0,100);

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new RawAlg(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new GenericsAlg(), TEST_TIME);
	}

	public static void main( String args[] ) {
		AreGenericsSlower alg = new AreGenericsSlower();

		alg.performTest();
	}

}
