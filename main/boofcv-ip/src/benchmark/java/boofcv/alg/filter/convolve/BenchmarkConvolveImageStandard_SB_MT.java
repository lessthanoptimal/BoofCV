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

import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard_SB_MT;
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
public class BenchmarkConvolveImageStandard_SB_MT extends CommonBenchmarkConvolve_SB {
//	@Param({"1", "10"})
	@Param({"5"})
	private int radius;

	@Setup public void setup() {
		setup(radius);
	}

	// @formatter:off
	@Benchmark public void horizontal_F32() {ConvolveImageStandard_SB_MT.horizontal(kernelF32, input_F32, out_F32);}
	@Benchmark public void horizontal_U8_I8_DIV() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_U8, out_U8, 10);}
	@Benchmark public void horizontal_U8_I16() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_U8, out_S16);}
	@Benchmark public void horizontal_U8_I32() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_U8, out_S32);}
	@Benchmark public void horizontal_U16_I16() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_U16, out_S16);}
	@Benchmark public void horizontal_U16_I16_DIV() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_U16, out_S16, 10);}
	@Benchmark public void horizontal_S16_I16() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_S16, out_S16);}
	@Benchmark public void horizontal_S16_I16_DIV() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_S16, out_S16, 10);}
	@Benchmark public void horizontal_S32_I32() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_S32, out_S32);}
	@Benchmark public void horizontal_S32_I32_DIV() {ConvolveImageStandard_SB_MT.horizontal(kernelI32, input_S32, out_S32, 10);}
	@Benchmark public void vertical_F32() {ConvolveImageStandard_SB_MT.vertical(kernelF32, input_F32, out_F32);}
	@Benchmark public void vertical_U8_I8_DIV() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_U8, out_U8, 10, work_I32);}
	@Benchmark public void vertical_U8_I16() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_U8, out_S16);}
	@Benchmark public void vertical_U8_I32() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_U8, out_S32);}
	@Benchmark public void vertical_U16_I16() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_U16, out_S16);}
	@Benchmark public void vertical_U16_I32_DIV() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_U16, out_S16, 10, work_I32);}
	@Benchmark public void vertical_S16_I16() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_S16, out_S16);}
	@Benchmark public void vertical_S16_I16_DIV() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_S16, out_S16, 10, work_I32);}
	@Benchmark public void vertical_S32_I16_DIV() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_S32, out_S16, 10, work_I32);}
	@Benchmark public void vertical_S32_S32() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_S32, out_S32);}
	@Benchmark public void vertical_S32_S32_DIV() {ConvolveImageStandard_SB_MT.vertical(kernelI32, input_S32, out_S32, 10, work_I32);}
	@Benchmark public void convolve2D_F32() {ConvolveImageStandard_SB_MT.convolve(kernel2D_F32, input_F32, out_F32);}
	@Benchmark public void convolve2D_U8_I16() {ConvolveImageStandard_SB_MT.convolve(kernel2D_I32, input_U8, out_S16);}
	@Benchmark public void convolve2D_U8_I32() {ConvolveImageStandard_SB_MT.convolve(kernel2D_I32, input_U8, out_S32);}
	@Benchmark public void convolve2D_U8_I8_DIV() {ConvolveImageStandard_SB_MT.convolve(kernel2D_I32, input_U8, out_U8, 10, work_I32);}
	@Benchmark public void convolve2D_U16_I16() {ConvolveImageStandard_SB_MT.convolve(kernel2D_I32, input_U16, out_S16);}
	@Benchmark public void convolve2D_U16_I16_DIV() {ConvolveImageStandard_SB_MT.convolve(kernel2D_I32, input_U16, out_S16, 10, work_I32);}
	@Benchmark public void convolve2D_S16_I16() {ConvolveImageStandard_SB_MT.convolve(kernel2D_I32, input_S16, out_S16);}
	@Benchmark public void convolve2D_S16_I16_DIV() {ConvolveImageStandard_SB_MT.convolve(kernel2D_I32, input_S16, out_S16, 10, work_I32);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvolveImageStandard_SB_MT.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
