/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.enhance;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.DogArray_I32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pabeles.concurrency.GrowArray;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks related to functions inside of ConvertImage
 *
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkEnhanceImageOps {
	@Param({"true", "false"})
	public boolean concurrent;

	@Param({"1000"})
	public int size;

	GrayU8 inputU8 = new GrayU8(size, size);
	GrayU8 outputU8 = new GrayU8(size, size);

	GrowArray<DogArray_I32> workArrays = new GrowArray<>(DogArray_I32::new);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		inputU8.reshape(size, size);
		outputU8.reshape(size, size);


		GImageMiscOps.fillUniform(inputU8, rand, 0, 200);
	}

	@Benchmark
	public void equalizeLocal_U8() {
		EnhanceImageOps.equalizeLocal(inputU8, 10, outputU8, 255, workArrays);
	}

	@Benchmark
	public void applyTransform_U8() {
		workArrays.reset();
		int[] histogram = BoofMiscOps.checkDeclare(workArrays.grow(), 256, false);
		int[] transform = BoofMiscOps.checkDeclare(workArrays.grow(), 256, false);
		ImageStatistics.histogram(inputU8, 0, histogram);
		EnhanceImageOps.equalize(histogram, transform);
		EnhanceImageOps.applyTransform(inputU8, transform, outputU8);
	}

	@Benchmark public void sharpen4() {EnhanceImageOps.sharpen4(inputU8, outputU8);}

	@Benchmark public void sharpen8() {EnhanceImageOps.sharpen8(inputU8, outputU8);}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkEnhanceImageOps.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
