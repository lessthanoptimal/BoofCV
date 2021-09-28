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

package boofcv.alg.filter.blur;

import boofcv.alg.filter.blur.impl.ImplMedianHistogramInner;
import boofcv.alg.filter.blur.impl.ImplMedianHistogramInnerNaive;
import boofcv.alg.filter.blur.impl.ImplMedianSortNaive;
import boofcv.alg.filter.convolve.CommonBenchmarkConvolve_SB;
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
public class BenchmarkMedianFilter extends CommonBenchmarkConvolve_SB {
//	@Param({"1", "4"})
	@Param({"4"})
	public int radius;

	@Setup public void setup() {setup(radius);}

	@Benchmark public void BlurImageOps_I8() {
		BlurImageOps.median(input_U8, out_U8, radius, radius, work_I32);
	}

	@Benchmark public void BlurImageOps_F32() {
		BlurImageOps.median(input_F32, out_F32, radius, radius, work_F32);
	}

	@Benchmark public void HistogramNaive_I8() {
		ImplMedianHistogramInnerNaive.process(input_U8, out_U8, radius, radius, null, null);
	}

	@Benchmark public void Histogram_I8() {
		ImplMedianHistogramInner.process(input_U8, out_U8, radius, radius, work_I32);
	}

	@Benchmark public void SortNaive_I8() {
		ImplMedianSortNaive.process(input_U8, out_U8, radius, radius, work_I32);
	}

	@Benchmark public void SortNaive_F32() {
		ImplMedianSortNaive.process(input_F32, out_F32, radius, radius, work_F32);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkBlurImageOps.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
