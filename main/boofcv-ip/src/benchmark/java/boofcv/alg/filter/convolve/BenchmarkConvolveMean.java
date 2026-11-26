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

import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
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
public class BenchmarkConvolveMean extends CommonBenchmarkConvolve_SB {
	@Param({"true", "false"})
	boolean concurrent;

	@Param({"10"})
	public int radius;

	ImageBorder_S32<GrayU8> borderU8 = (ImageBorder_S32<GrayU8>)FactoryImageBorder.single(BorderType.EXTENDED, GrayU8.class);
	ImageBorder_F32 borderF32 = (ImageBorder_F32)FactoryImageBorder.single(BorderType.EXTENDED, GrayF32.class);

	@Setup public void setup() {
		// large enough for ConvolveImageMean to use threads
		width = 1200;
		height = 1000;
		BoofConcurrency.USE_CONCURRENT = concurrent;
		setup(radius);
	}

	@Benchmark public void vertical_U8() {
		ConvolveImageMean.vertical(input_U8, out_U8, radius, radius*2+1);
	}

	@Benchmark public void vertical_S16() {
		ConvolveImageMean.vertical(input_S16, out_S16, radius, radius*2+1);
	}

	@Benchmark public void vertical_U16() {
		ConvolveImageMean.vertical(input_U16, out_S16, radius, radius*2+1);
	}

	@Benchmark public void vertical_F32() {
		ConvolveImageMean.vertical(input_F32, out_F32, radius, radius*2+1);
	}

	@Benchmark public void vertical_F64() {
		ConvolveImageMean.vertical(input_F64, out_F64, radius, radius*2+1);
	}

	@Benchmark public void horizontal_U8() {
		ConvolveImageMean.horizontal(input_U8, out_U8, radius, radius*2+1);
	}

	@Benchmark public void horizontal_S16() {
		ConvolveImageMean.horizontal(input_S16, out_S16, radius, radius*2+1);
	}

	@Benchmark public void horizontal_U16() {
		ConvolveImageMean.horizontal(input_U16, out_S16, radius, radius*2+1);
	}

	@Benchmark public void horizontal_F32() {
		ConvolveImageMean.horizontal(input_F32, out_F32, radius, radius*2+1);
	}

	@Benchmark public void horizontal_F64() {
		ConvolveImageMean.horizontal(input_F64, out_F64, radius, radius*2+1);
	}

	@Benchmark public void horizontalBorder_U8() {
		ConvolveImageMean.horizontal(input_U8, out_U8, radius, radius*2+1, borderU8);
	}

	@Benchmark public void verticalBorder_U8() {
		ConvolveImageMean.vertical(input_U8, out_U8, radius, radius*2+1, borderU8);
	}

	@Benchmark public void horizontalBorder_F32() {
		ConvolveImageMean.horizontal(input_F32, out_F32, radius, radius*2+1, borderF32);
	}

	@Benchmark public void verticalBorder_F32() {
		ConvolveImageMean.vertical(input_F32, out_F32, radius, radius*2+1, borderF32);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvolveMean.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
