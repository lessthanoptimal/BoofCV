/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.convolve.noborder.ConvolveImageUnrolled_IL;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

import static boofcv.misc.BoofMiscOps.checkTrue;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
@SuppressWarnings({"UnusedDeclaration"})
public class BenchmarkConvolveUnrolled_IL extends CommonBenchmarkConvolve_IL {
	//	@Param({"1", "2", "3"})
	@Param({"3"})
	private int radius;

	//	@Param({"2", "3", "4"})
	@Param({"3"})
	private int bands;

	@Setup public void setup() {
		setup(bands, radius);
	}

	// @formatter:off
	@Benchmark public void horizontal_F32() {checkTrue(ConvolveImageUnrolled_IL.horizontal(kernelF32, input_F32, out_F32));}
	@Benchmark public void vertical_F32() {checkTrue(ConvolveImageUnrolled_IL.vertical(kernelF32, input_F32, out_F32));}
	@Benchmark public void convolve2D_F32() {checkTrue(ConvolveImageUnrolled_IL.convolve(kernel2D_F32, input_F32, out_F32));}
	@Benchmark public void horizontal_U8_I16() {checkTrue(ConvolveImageUnrolled_IL.horizontal(kernelI32, input_U8, out_S16));}
	@Benchmark public void vertical_U8_I16() {checkTrue(ConvolveImageUnrolled_IL.vertical(kernelI32, input_U8, out_S16));}
	@Benchmark public void convolve2D_U8_I16() {checkTrue(ConvolveImageUnrolled_IL.convolve(kernel2D_I32, input_U8, out_S16));}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvolveUnrolled_IL.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
