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

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
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
public class BenchmarkHessianSobel extends CommonBenchmarkDerivative {

	static GrayF32 tempA_F32 = new GrayF32(width, height);
	static GrayF32 tempB_F32 = new GrayF32(width, height);
	static GrayS16 tempA_I16 = new GrayS16(width, height);
	static GrayS16 tempB_I16 = new GrayS16(width, height);

	@Param({"true", "false"})
	boolean concurrent;

	@Setup @Override public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		super.setup();
	}

	@Benchmark public void Hessian_I8() {HessianSobel.process(imgI8, dx_I16, dy_I16, dxy_I16, borderI32);}

	@Benchmark public void Hessian_F32() {HessianSobel.process(imgF32, dx_F32, dy_F32, dxy_F32, borderF32);}

	@Benchmark public void HessianFromDeriv_I8() {
		GradientSobel.process(imgI8, tempA_I16, tempB_I16, borderI32);
		HessianFromGradient.hessianSobel(tempA_I16, tempB_I16, dx_I16, dy_I16, dxy_I16, borderI32);
	}

	@Benchmark public void HessianFromDeriv_F32() {
		GradientSobel.process(imgF32, tempA_F32, tempB_F32, borderF32);
		HessianFromGradient.hessianSobel(tempA_F32, tempB_F32, dx_F32, dy_F32, dxy_F32, borderF32);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkHessianSobel.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
