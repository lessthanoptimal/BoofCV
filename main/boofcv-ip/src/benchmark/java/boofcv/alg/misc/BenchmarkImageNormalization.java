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

package boofcv.alg.misc;

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
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
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkImageNormalization {
	@Param({"true", "false"})
	public boolean concurrent;

	@Param({"1000"})
	public int size;

	GrayF32 imgA_F32 = new GrayF32(size, size);
	GrayF32 imgB_F32 = new GrayF32(size, size);

	NormalizeParameters param = new NormalizeParameters(5, 2.1);

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		var rand = new Random(2345);

		imgA_F32.reshape(size, size);
		imgB_F32.reshape(size, size);

		GImageMiscOps.fillUniform(imgA_F32, rand, 0, 200);
	}

	// @formatter:off
	@Benchmark public void apply() {ImageNormalization.apply(imgA_F32, param, imgB_F32);}
	@Benchmark public void maxAbsOfOne() {ImageNormalization.maxAbsOfOne(imgA_F32, imgB_F32, param);}
	@Benchmark public void zeroMeanMaxOne() {ImageNormalization.zeroMeanMaxOne(imgA_F32, imgB_F32, param);}
	@Benchmark public void zeroMeanStdOne() {ImageNormalization.zeroMeanStdOne(imgA_F32, imgB_F32, param);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImageNormalization.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
