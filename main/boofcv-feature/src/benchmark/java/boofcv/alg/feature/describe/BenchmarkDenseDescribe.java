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

package boofcv.alg.feature.describe;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkDenseDescribe {

	@Param({"true", "false"})
	boolean concurrent;

	@Param({"800"})
	int size = 0;

	GrayF32 gray = new GrayF32(1, 1);
	DescribeImageDense<GrayF32, TupleDesc_F64> alg;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;

		Random rand = new Random(234234);
		gray.reshape(size, size);
		GImageMiscOps.fillUniform(gray, rand, 0, 200);
	}

	@Benchmark public void HoG_fast() {
		ConfigDenseHoG config = new ConfigDenseHoG();
		config.fastVariant = true;
		alg = FactoryDescribeImageDense.hog(config, ImageType.single(GrayF32.class));
		alg.process(gray);
	}

	@Benchmark public void HoG() {
		alg = FactoryDescribeImageDense.hog(null, ImageType.single(GrayF32.class));
		alg.process(gray);
	}

	@Benchmark public void SURF_fast() {
		alg = FactoryDescribeImageDense.surfFast(null, GrayF32.class);
		alg.process(gray);
	}

	@Benchmark public void SURF_stable() {
		alg = FactoryDescribeImageDense.surfStable(null, GrayF32.class);
		alg.process(gray);
	}

	@Benchmark public void SIFT() {
		alg = FactoryDescribeImageDense.sift(null, GrayF32.class);
		alg.process(gray);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkDenseDescribe.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
