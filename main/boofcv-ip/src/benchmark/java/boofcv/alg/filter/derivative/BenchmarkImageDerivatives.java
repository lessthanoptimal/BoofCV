/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.border.ImageBorder1D_S32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=2)
public class BenchmarkImageDerivatives {
	@Param({"true","false"})
	public boolean concurrent;

//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"10000"})
	public int size;

	GrayU8 input = new GrayU8(size, size);
	GrayS16 derivX = new GrayS16(size, size);
	GrayS16 derivY = new GrayS16(size, size);
	ImageBorder_S32<GrayU8> borderI32 = new ImageBorder1D_S32(BorderIndex1D_Extend.class);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		input.reshape(size, size);
		derivX.reshape(size, size);
		derivY.reshape(size, size);

		ImageMiscOps.fillUniform(input,rand,0,200);
	}

	@Benchmark
	public void prewitt() {
		GradientPrewitt.process(input,derivX,derivY,borderI32);
	}

	@Benchmark
	public void laplacian() {
		DerivativeLaplacian.process(input,derivX,borderI32);
	}

	@Benchmark
	public void hessianDet() {
		HessianThreeDeterminant.process(input,derivX,borderI32);
	}

	@Benchmark
	public void sobel() {
		GradientSobel.process(input,derivX,derivY,borderI32);
	}

	@Benchmark
	public void three() {
		GradientThree.process(input,derivX,derivY,borderI32);
	}

	@Benchmark
	public void two0() {
		GradientTwo0.process(input,derivX,derivY,borderI32);
	}

	@Benchmark
	public void two1() {
		GradientTwo1.process(input,derivX,derivY,borderI32);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImageDerivatives.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
