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

package boofcv.alg.feature.detect.extract;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.extract.WrapperNonMaximumBlock;
import boofcv.abst.feature.detect.extract.WrapperNonMaximumNaive;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkExtractors {

	static int imgWidth = 640;
	static int imgHeight = 480;
	static int windowRadius = 2;
	static float threshold = 1.0f;
	static long TEST_TIME = 1000;


	static GrayF32 intensity;
	static QueueCorner corners;

	static Random rand = new Random(33456);


	public static class NM implements Performer {
		NonMaxSuppression alg;
		String name;

		public NM(String name , NonMaxSuppression alg) {
			this.alg = alg;
			this.name = name;
			alg.setThresholdMaximum(threshold);
			alg.setSearchRadius(windowRadius);
		}

		@Override
		public void process() {
			corners.reset();
			alg.process(intensity, null,null,corners,corners);
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public static void main(String args[]) {
		intensity = new GrayF32(imgWidth, imgHeight);
		corners = new QueueCorner(imgWidth * imgHeight);

		// have about 1/20 the image below threshold
		ImageMiscOps.fillUniform(intensity, rand, 0, threshold * 20.0f);

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ThresholdCornerExtractor algThresh = new ThresholdCornerExtractor();
		NonMaxBlockStrict algBlockStrict = new NonMaxBlockStrict.Max();
		NonMaxBlockStrict algBlockStrictMinMax = new NonMaxBlockStrict.MinMax();
		NonMaxExtractorNaive algNaiveStrict = new NonMaxExtractorNaive(true);
		NonMaxBlockRelaxed algBlockRelaxed = new NonMaxBlockRelaxed.Max();
		NonMaxExtractorNaive algNaiveRelaxed = new NonMaxExtractorNaive(true);


		for ( int radius = 1; radius < 20; radius += 1) {
			System.out.println("Radius: " + radius);
			System.out.println();
			windowRadius = radius;

			NM alg2 = new NM("Block Strict",new WrapperNonMaximumBlock(algBlockStrict));
			NM alg3 = new NM("Block Strict MinMax",new WrapperNonMaximumBlock(algBlockStrictMinMax));
			NM alg4 = new NM("Naive Strict",new WrapperNonMaximumNaive(algNaiveStrict));
			NM alg5 = new NM("Block Relaxed",new WrapperNonMaximumBlock(algBlockRelaxed));
			NM alg6 = new NM("Naive Relaxed",new WrapperNonMaximumNaive(algNaiveRelaxed));

			ProfileOperation.printOpsPerSec(alg2, TEST_TIME);
			ProfileOperation.printOpsPerSec(alg3, TEST_TIME);
//			ProfileOperation.printOpsPerSec(alg4, TEST_TIME);
//			ProfileOperation.printOpsPerSec(alg5, TEST_TIME);
//			ProfileOperation.printOpsPerSec(alg6, TEST_TIME);
//			ProfileOperation.printOpsPerSec(alg1, TEST_TIME);
		}

	}
}
