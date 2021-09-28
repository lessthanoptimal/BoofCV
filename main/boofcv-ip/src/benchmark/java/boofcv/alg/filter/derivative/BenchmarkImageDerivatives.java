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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.struct.border.ImageBorder1D_F32;
import boofcv.struct.border.ImageBorder1D_S32;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkImageDerivatives {
	@Param({"true", "false"})
	public boolean concurrent;

	@Param({"1000"})
	public int size;

	GrayU8 input_U8 = new GrayU8(size, size);
	GrayS16 derivX_S16 = new GrayS16(size, size);
	GrayS16 derivY_S16 = new GrayS16(size, size);
	ImageBorder_S32<GrayU8> borderI32 = new ImageBorder1D_S32(BorderIndex1D_Extend::new);

	GrayF32 input_F32 = new GrayF32(size, size);
	GrayF32 derivX_F32 = new GrayF32(size, size);
	GrayF32 derivY_F32 = new GrayF32(size, size);
	ImageBorder_F32 borderF32 = new ImageBorder1D_F32(BorderIndex1D_Extend::new);

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;

		input_U8.reshape(size, size);
		derivX_S16.reshape(size, size);
		derivY_S16.reshape(size, size);

		input_F32.reshape(size, size);
		derivX_F32.reshape(size, size);
		derivY_F32.reshape(size, size);

		var rand = new Random(234);
		ImageMiscOps.fillUniform(input_U8, rand, 0, 200);
		ImageMiscOps.fillUniform(input_F32, rand, 0, 200);
	}

	// @formatter:off
	@Benchmark public void prewitt_U8() { GradientPrewitt.process(input_U8,derivX_S16,derivY_S16,borderI32); }
	@Benchmark public void laplacian_U8() { DerivativeLaplacian.process(input_U8,derivX_S16,borderI32); }
	@Benchmark public void hessianDet_U8() { HessianThreeDeterminant.process(input_U8,derivX_S16,borderI32); }
	@Benchmark public void sobel_U8() { GradientSobel.process(input_U8,derivX_S16,derivY_S16,borderI32); }
	@Benchmark public void scharr_U8() { GradientScharr.process(input_U8,derivX_S16,derivY_S16,borderI32); }
	@Benchmark public void three_U8() { GradientThree.process(input_U8,derivX_S16,derivY_S16,borderI32); }
	@Benchmark public void two0_U8() { GradientTwo0.process(input_U8,derivX_S16,derivY_S16,borderI32); }
	@Benchmark public void two1_U8() { GradientTwo1.process(input_U8,derivX_S16,derivY_S16,borderI32); }

	@Benchmark public void prewitt_F32() { GradientPrewitt.process(input_F32,derivX_F32,derivY_F32,borderF32); }
	@Benchmark public void laplacian_F32() { DerivativeLaplacian.process(input_F32,derivX_F32,borderF32); }
	@Benchmark public void hessianDet_F32() { HessianThreeDeterminant.process(input_F32,derivX_F32,borderF32); }
	@Benchmark public void sobel_F32() { GradientSobel.process(input_F32,derivX_F32,derivY_F32,borderF32); }
	@Benchmark public void scharr_F32() { GradientScharr.process(input_F32,derivX_F32,derivY_F32,borderF32); }
	@Benchmark public void three_F32() { GradientThree.process(input_F32,derivX_F32,derivY_F32,borderF32); }
	@Benchmark public void two0_F32() { GradientTwo0.process(input_F32,derivX_F32,derivY_F32,borderF32); }
	@Benchmark public void two1_F32() { GradientTwo1.process(input_F32,derivX_F32,derivY_F32,borderF32); }
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImageDerivatives.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
