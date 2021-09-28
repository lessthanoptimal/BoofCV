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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.down.ConvolveDownNormalizedNaive;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkConvolveDownNormalized extends CommonBenchmarkConvolve_SB {
	static int skip = 2;

//	@Param({"2", "10"})
	@Param({"5"})
	public int radius;

	@Setup public void setup() {setupSkip(radius, skip);}

	@Benchmark public void Horizontal_Naive_F32() {
		ConvolveDownNormalizedNaive.horizontal(kernelF32, input_F32, out_F32, skip);
	}

	@Benchmark public void Horizontal_F32() {
		ConvolveImageDownNormalized.horizontal(kernelF32, input_F32, out_F32, skip);
	}

	@Benchmark public void Vertical_Naive_F32() {
		ConvolveDownNormalizedNaive.vertical(kernelF32, input_F32, out_F32, skip);
	}

	@Benchmark public void Vertical_F32() {
		ConvolveImageDownNormalized.vertical(kernelF32, input_F32, out_F32, skip);
	}

	@Benchmark public void Convolve_Naive_F32() {
		ConvolveDownNormalizedNaive.convolve(kernel2D_F32, input_F32, out_F32, skip);
	}

	@Benchmark public void Convolve_F32() {
		ConvolveImageDownNormalized.convolve(kernel2D_F32, input_F32, out_F32, skip);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvolveDown.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
