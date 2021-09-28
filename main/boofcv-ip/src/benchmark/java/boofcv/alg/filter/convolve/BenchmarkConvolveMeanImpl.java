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

import boofcv.alg.filter.convolve.noborder.ImplConvolveMean;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pabeles.concurrency.GrowArray;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
@SuppressWarnings({"UnusedDeclaration"})
public class BenchmarkConvolveMeanImpl extends CommonBenchmarkConvolve_SB {
//	@Param({"1", "10"})
	@Param({"5"})
	public int radius;

	GrowArray<DogArray_I32> work_I32 = new GrowArray<>(DogArray_I32::new);
	GrowArray<DogArray_F32> work_F32 = new GrowArray<>(DogArray_F32::new);
	GrowArray<DogArray_F64> work_F64 = new GrowArray<>(DogArray_F64::new);
	
	@Setup public void setup() {setup(radius);}

	@Benchmark public void vertical_U8() {
		ImplConvolveMean.vertical(input_U8, out_U8, radius, radius*2+1, work_I32);
	}

	@Benchmark public void vertical_S16() {
		ImplConvolveMean.vertical(input_S16, out_S16, radius, radius*2+1, work_I32);
	}

	@Benchmark public void vertical_U16() {
		ImplConvolveMean.vertical(input_U16, out_S16, radius, radius*2+1, work_I32);
	}

	@Benchmark public void vertical_F32() {
		ImplConvolveMean.vertical(input_F32, out_F32, radius, radius*2+1, work_F32);
	}

	@Benchmark public void vertical_F64() {
		ImplConvolveMean.vertical(input_F64, out_F64, radius, radius*2+1, work_F64);
	}

	@Benchmark public void horizontal_U8() {
		ImplConvolveMean.horizontal(input_U8, out_U8, radius, radius*2+1);
	}

	@Benchmark public void horizontal_S16() {
		ImplConvolveMean.horizontal(input_S16, out_S16, radius, radius*2+1);
	}

	@Benchmark public void horizontal_U16() {
		ImplConvolveMean.horizontal(input_U16, out_S16, radius, radius*2+1);
	}

	@Benchmark public void horizontal_F32() {
		ImplConvolveMean.horizontal(input_F32, out_F32, radius, radius*2+1);
	}

	@Benchmark public void horizontal_F64() {
		ImplConvolveMean.horizontal(input_F64, out_F64, radius, radius*2+1);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvolveMeanImpl.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
