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

import boofcv.alg.filter.convolve.noborder.ImplConvolveBox;
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
public class BenchmarkConvolveBox extends CommonBenchmarkConvolve_SB {
	@Param({"1", "4"})
	public int radius;

	@Setup public void setup() {setup(radius);}

	@Benchmark public void Vertical_U8_I16() {
		ImplConvolveBox.vertical(input_U8, out_S16, radius, null);
	}

	@Benchmark public void Vertical_U8_I32() {
		ImplConvolveBox.vertical(input_U8, out_S32, radius, null);
	}

	@Benchmark public void Vertical_S16_I16() {
		ImplConvolveBox.vertical(input_U8, out_S16, radius, null);
	}

	@Benchmark public void Vertical_F32_F32() {
		ImplConvolveBox.vertical(input_F32, out_F32, radius, null);
	}

	@Benchmark public void Vertical_Alt_F32_F32() {
		ConvolveBoxAlt.vertical(input_F32, out_F32, radius, false);
	}

	@Benchmark public void Horizontal_U8_I16() {
		ImplConvolveBox.horizontal(input_U8, out_S16, radius);
	}

	@Benchmark public void Horizontal_U8_I32() {
		ImplConvolveBox.horizontal(input_U8, out_S32, radius);
	}

	@Benchmark public void Horizontal_S16_I16() {
		ImplConvolveBox.horizontal(out_S16, out_S16, radius);
	}

	@Benchmark public void Horizontal_F32_F32() {
		ImplConvolveBox.horizontal(input_F32, out_F32, radius);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvolveBox.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
