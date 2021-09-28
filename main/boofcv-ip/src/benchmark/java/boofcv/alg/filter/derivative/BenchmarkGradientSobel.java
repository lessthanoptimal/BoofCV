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

package boofcv.alg.filter.derivative;

import boofcv.alg.filter.derivative.impl.GradientSobel_Naive;
import boofcv.alg.filter.derivative.impl.GradientSobel_Outer;
import boofcv.alg.filter.derivative.impl.GradientSobel_UnrolledOuter;
import boofcv.concurrency.BoofConcurrency;
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
public class BenchmarkGradientSobel extends CommonBenchmarkDerivative {
	@Param({"true", "false"})
	public boolean concurrent;

	@Setup @Override public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		super.setup();
	}

	// @formatter:off
	@Benchmark public void Sobel_I8() {GradientSobel.process(imgI8, dx_I16, dy_I16, borderI32);}
	@Benchmark public void Sobel_F32() {GradientSobel.process(imgF32, dx_F32, dy_F32, borderF32);}
	@Benchmark public void Naive_I8() {GradientSobel_Naive.process(imgI8, dx_I16, dy_I16);}
	@Benchmark public void Naive_F32() {GradientSobel_Naive.process(imgF32, dx_F32, dy_F32);}
	@Benchmark public void Outer_I8() {GradientSobel_Outer.process(imgI8, dx_I16, dy_I16);}
	@Benchmark public void Outer_F32() {GradientSobel_Outer.process(imgF32, dx_F32, dy_F32);}
	@Benchmark public void OuterSub_I8() {GradientSobel_Outer.process_sub(imgI8, dx_I16, dy_I16);}
	@Benchmark public void OuterUnrolled_I8() {GradientSobel_UnrolledOuter.process_I8(imgI8, dx_I16, dy_I16);}
	@Benchmark public void OuterUnrolled_F32() {GradientSobel_UnrolledOuter.process_F32(imgF32, dx_F32, dy_F32);}
	@Benchmark public void OuterUnrolledSub_F32() {GradientSobel_UnrolledOuter.process_F32_sub(imgF32, dx_F32, dy_F32);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkGradientSobel.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
