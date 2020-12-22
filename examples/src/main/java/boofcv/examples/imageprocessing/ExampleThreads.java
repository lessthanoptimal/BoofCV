/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.imageprocessing;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * Example for turning on and off concurrent algorithms, also known as multi-threaded algorithms.
 *
 * @author Peter Abeles
 */
public class ExampleThreads {
	public static void main( String[] args ) {
		// Create a 12 mega pixel image so that we easily see the affects of threading
		GrayU8 image = new GrayU8(4000, 3000);
		GrayU8 blurred = new GrayU8(4000, 3000);

		// fill the image with random data
		ImageMiscOps.fillUniform(image, new Random(), 0, 255);

		// By default threads are turned on and it uses the maximum number of threads
		System.out.println("Default Settings");
		blur(image, blurred);

		// Let's turn off threads and see how much slower it is. The number of physical cores
		// is the primary factor in determining the amount of speed up. Hyper threads only help a little bit
		System.out.println("\nThreads are now off");
		BoofConcurrency.USE_CONCURRENT = false;
		blur(image, blurred);

		// Let's turn threading back on. You should only really turn threads on and off when you first start
		// since the behavior later on isn't formally defined, but can be determined by browsing the source code
		BoofConcurrency.USE_CONCURRENT = true;
		// We will now change the number of threads to be 2,3, and 4. Look at how the speed changes
		for (int threadCount = 2; threadCount <= 4; threadCount++) {
			System.out.println("\nThreads = " + threadCount);
			BoofConcurrency.setMaxThreads(threadCount);
			blur(image, blurred);
		}
		// if the final average time you see is faster than the default that's likely caused by the hotspot compiler
		// warming up. The first iteration is always slower.
	}

	public static void blur( GrayU8 image, GrayU8 blurred ) {
		BlurFilter<GrayU8> filter = FactoryBlurFilter.gaussian(GrayU8.class, -1, 12);

		long time0 = System.nanoTime();
		for (int i = 0; i < 10; i++) {
			filter.process(image, blurred);
		}
		long time1 = System.nanoTime();

		System.out.printf("average time %7.2f (ms)\n", (time1 - time0)*1e-7);
	}
}
