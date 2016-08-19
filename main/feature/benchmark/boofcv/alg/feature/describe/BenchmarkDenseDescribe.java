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

package boofcv.alg.feature.describe;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkDenseDescribe {

	int width = 640;
	int height = 480;

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);

	GrayF32 gray = new GrayF32(width, height);

	public BenchmarkDenseDescribe() {
		GImageMiscOps.fillUniform( gray , rand , 0 , 200);
	}

	public class HoGFast extends PerformerBase {

		DescribeImageDense<GrayF32, TupleDesc_F64> alg;

		public HoGFast() {
			ConfigDenseHoG config = new ConfigDenseHoG();
			config.fastVariant = true;
			alg = FactoryDescribeImageDense.hog(config, ImageType.single(GrayF32.class));
		}

		@Override
		public void process() {
			alg.process(gray);
		}
	}

	public class HoG extends PerformerBase {
		DescribeImageDense<GrayF32, TupleDesc_F64> alg =
				FactoryDescribeImageDense.hog(null, ImageType.single(GrayF32.class));

		@Override
		public void process() {
			alg.process(gray);
		}
	}

	public class SURF_FAST extends PerformerBase {
		DescribeImageDense<GrayF32, TupleDesc_F64> alg =
				FactoryDescribeImageDense.surfFast(null, GrayF32.class);

		@Override
		public void process() {
			alg.process(gray);
		}
	}

	public class SURF_STABLE extends PerformerBase {
		DescribeImageDense<GrayF32, TupleDesc_F64> alg =
				FactoryDescribeImageDense.surfStable(null, GrayF32.class);

		@Override
		public void process() {
			alg.process(gray);
		}
	}

	public class SIFT extends PerformerBase {
		DescribeImageDense<GrayF32, TupleDesc_F64> alg =
				FactoryDescribeImageDense.sift(null, GrayF32.class);

		@Override
		public void process() {
			alg.process(gray);
		}
	}


	public void perform() {
		System.out.println("=========  Profile Image Size " + width + " x " + height + " ========== ");
		System.out.println();

		ProfileOperation.printOpsPerSec(new HoGFast(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new HoG(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new BenchmarkDenseDescribe.SURF_FAST(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new BenchmarkDenseDescribe.SURF_STABLE(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new BenchmarkDenseDescribe.SIFT(), TEST_TIME);
	}

	public static void main(String[] args) {
		BenchmarkDenseDescribe benchmark = new BenchmarkDenseDescribe();

		benchmark.perform();
	}
}
