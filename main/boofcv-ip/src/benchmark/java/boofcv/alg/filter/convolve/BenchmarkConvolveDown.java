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

import boofcv.alg.filter.convolve.down.ConvolveDownNoBorderStandard;
import boofcv.alg.filter.convolve.down.ConvolveDownNoBorderUnrolled_F32_F32;
import boofcv.alg.filter.convolve.down.ConvolveDownNoBorderUnrolled_U8_I16;
import boofcv.alg.filter.convolve.down.ConvolveDownNoBorderUnrolled_U8_I8_Div;
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
public class BenchmarkConvolveDown extends CommonBenchmarkConvolve_SB {
	static int skip = 2;

	@Param({"2"})
	public int radius;

	@Setup public void setup() {setupSkip(radius, skip);}

	@Benchmark public void HorizontalStandard_F32() {
		ConvolveDownNoBorderStandard.horizontal(kernelF32, input_F32, out_F32, skip);
	}

	@Benchmark public void HorizontalUnrolled_F32() {
		if (!ConvolveDownNoBorderUnrolled_F32_F32.horizontal(kernelF32, input_F32, out_F32, skip))
			throw new RuntimeException();
	}

	@Benchmark public void VerticalStandard_F32() {
		ConvolveDownNoBorderStandard.vertical(kernelF32, input_F32, out_F32, skip);
	}

	@Benchmark public void VerticalUnrolled_F32() {
		if (!ConvolveDownNoBorderUnrolled_F32_F32.vertical(kernelF32, input_F32, out_F32, skip))
			throw new RuntimeException();
	}

	@Benchmark public void Convolve2DStandard_F32() {
		ConvolveDownNoBorderStandard.convolve(kernel2D_F32, input_F32, out_F32, skip);
	}

	@Benchmark public void Convolve2DUnrolled_F32() {
		if (!ConvolveDownNoBorderUnrolled_F32_F32.convolve(kernel2D_F32, input_F32, out_F32, skip))
			throw new RuntimeException();
	}

	@Benchmark public void VerticalStandard_U8_I16() {
		ConvolveDownNoBorderStandard.vertical(kernelI32, input_U8, out_S16, skip);
	}

	@Benchmark public void VerticalUnrolled_U8_I16() {
		if (!ConvolveDownNoBorderUnrolled_U8_I16.vertical(kernelI32, input_U8, out_S16, skip))
			throw new RuntimeException();
	}

	@Benchmark public void VerticalStandard_U8_I8_Div() {
		ConvolveDownNoBorderStandard.vertical(kernelI32, input_U8, out_U8, skip, 10);
	}

	@Benchmark public void VerticalUnrolled_U8_I8_Div() {
		if (!ConvolveDownNoBorderUnrolled_U8_I8_Div.vertical(kernelI32, input_U8, out_U8, skip, 10))
			throw new RuntimeException();
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
