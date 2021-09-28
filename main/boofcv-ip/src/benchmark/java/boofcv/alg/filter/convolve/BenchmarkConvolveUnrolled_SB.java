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

import boofcv.alg.filter.convolve.noborder.*;
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
@SuppressWarnings({"UnusedDeclaration"})
public class BenchmarkConvolveUnrolled_SB extends CommonBenchmarkConvolve_SB {
//	@Param({"1","2","3","4","5"})
	@Param({"1","3","5"})
	private int radius;

	@Setup public void setup() {
		setup(radius);
	}

	// @formatter:off
	@Benchmark public void horizontal_F32() {ConvolveImageUnrolled_SB_F32_F32.horizontal(kernelF32, input_F32, out_F32);}
	@Benchmark public void horizontal_F64() {ConvolveImageUnrolled_SB_F64_F64.horizontal(kernelF64, input_F64, out_F64);}
	@Benchmark public void horizontal_U8_I8_DIV() {ConvolveImageUnrolled_SB_U8_I8_Div.horizontal(kernelI32, input_U8, out_U8, 10);}
	@Benchmark public void horizontal_U8_I16() {ConvolveImageUnrolled_SB_U8_I16.horizontal(kernelI32, input_U8, out_S16);}
	@Benchmark public void horizontal_S16_I16() {ConvolveImageUnrolled_SB_S16_I16.horizontal(kernelI32, input_S16, out_S16);}
	@Benchmark public void horizontal_S16_I16_DIV() {ConvolveImageUnrolled_SB_S16_I16_Div.horizontal(kernelI32, input_S16, out_S16, 10);}
	@Benchmark public void horizontal_S32_S32() {ConvolveImageUnrolled_SB_S32_S32.horizontal(kernelI32, input_S32, out_S32);}
	@Benchmark public void horizontal_S32_S32_DIV() {ConvolveImageUnrolled_SB_S32_S32_Div.horizontal(kernelI32, input_S32, out_S32, 10);}

	@Benchmark public void vertical_F32() {ConvolveImageUnrolled_SB_F32_F32.vertical(kernelF32, input_F32, out_F32);}
	@Benchmark public void vertical_F64() {ConvolveImageUnrolled_SB_F64_F64.vertical(kernelF64, input_F64, out_F64);}
	@Benchmark public void vertical_U8_I8_DIV() {ConvolveImageUnrolled_SB_U8_I8_Div.vertical(kernelI32, input_U8, out_U8, 10, work_I32);}
	@Benchmark public void vertical_U8_I16() {ConvolveImageUnrolled_SB_U8_I16.vertical(kernelI32, input_U8, out_S16);}
	@Benchmark public void vertical_S16_I16() {ConvolveImageUnrolled_SB_S16_I16.vertical(kernelI32, input_S16, out_S16);}
	@Benchmark public void vertical_S16_I16_DIV() {ConvolveImageUnrolled_SB_S16_I16_Div.vertical(kernelI32, input_S16, out_S16, 10, work_I32);}
	@Benchmark public void vertical_S32_S32() {ConvolveImageUnrolled_SB_S32_S32.vertical(kernelI32, input_S32, out_S32);}
	@Benchmark public void vertical_S32_S32_DIV() {ConvolveImageUnrolled_SB_S32_S32_Div.vertical(kernelI32, input_S32, out_S32, 10, work_I32);}

	@Benchmark public void convolve_F32() {ConvolveImageUnrolled_SB_F32_F32.convolve(kernel2D_F32, input_F32, out_F32);}
	@Benchmark public void convolve_F64() {ConvolveImageUnrolled_SB_F64_F64.convolve(kernel2D_F64, input_F64, out_F64);}
	@Benchmark public void convolve_U8_I8_DIV() {ConvolveImageUnrolled_SB_U8_I8_Div.convolve(kernel2D_I32, input_U8, out_U8, 10, work_I32);}
	@Benchmark public void convolve_U8_I16() {ConvolveImageUnrolled_SB_U8_I16.convolve(kernel2D_I32, input_U8, out_S16);}
	@Benchmark public void convolve_S16_I16() {ConvolveImageUnrolled_SB_S16_I16.convolve(kernel2D_I32, input_S16, out_S16);}
	@Benchmark public void convolve_S16_I16_DIV() {ConvolveImageUnrolled_SB_S16_I16_Div.convolve(kernel2D_I32, input_S16, out_S16, 10, work_I32);}
	@Benchmark public void convolve_S32_S32() {ConvolveImageUnrolled_SB_S32_S32.convolve(kernel2D_I32, input_S32, out_S32);}
	@Benchmark public void convolve_S32_S32_DIV() {ConvolveImageUnrolled_SB_S32_S32_Div.convolve(kernel2D_I32, input_S32, out_S32, 10, work_I32);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvolveUnrolled_SB.class.getSimpleName())
				.exclude(BenchmarkConvolveUnrolled_SB_MT.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
