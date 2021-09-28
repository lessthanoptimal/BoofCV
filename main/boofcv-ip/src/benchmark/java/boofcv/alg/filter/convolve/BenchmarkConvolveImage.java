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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;
import boofcv.struct.image.Planar;
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
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkConvolveImage extends CommonBenchmarkConvolve_SB {
	@Param({"true", "false"})
	boolean concurrent;

//	@Param({"1", "10"})
	@Param({"5"})
	private int radius;

	static private int numBands = 2;

	static private ImageBorder_S32 border_I32 = new ImageBorder1D_S32(BorderIndex1D_Extend::new);
	static private ImageBorder_F32 border_F32 = new ImageBorder1D_F32(BorderIndex1D_Extend::new);

	static private ImageBorder_IL_F32 border_IL_F32 = new ImageBorder1D_IL_F32(BorderIndex1D_Extend::new);

	static private ImageBorder<Planar<GrayF32>> border_PL_F32 = FactoryImageBorder.generic(BorderType.EXTENDED,
			ImageType.pl(numBands, GrayF32.class));

	InterleavedF32 input_IL_F32, out_IL_F32;
	private Planar<GrayF32> input_PL_F32 = new Planar<>(GrayF32.class, width, height, numBands);
	private Planar<GrayF32> out_PL_F32 = new Planar<>(GrayF32.class, width, height, numBands);

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		setup(radius);

		Random rand = new Random(234);
		input_IL_F32 = new InterleavedF32(width, height, numBands);
		out_IL_F32 = new InterleavedF32(width, height, numBands);
		ImageMiscOps.fillUniform(input_IL_F32, rand, 0, 20);
	}

	// @formatter:off
	@Benchmark public void horizontal_U8() {ConvolveImage.horizontal(kernelI32, input_U8, out_S16, border_I32);}
	@Benchmark public void vertical_U8() {ConvolveImage.vertical(kernelI32, input_U8, out_S16, border_I32);}
	@Benchmark public void horizontal_U16() {ConvolveImage.horizontal(kernelI32, input_S16, out_S16, border_I32);}
	@Benchmark public void vertical_U16() {ConvolveImage.vertical(kernelI32, input_S16, out_S16, border_I32);}
	@Benchmark public void horizontal_F32() {ConvolveImage.horizontal(kernelF32, input_F32, out_F32, border_F32);}
	@Benchmark public void vertical_F32() {ConvolveImage.vertical(kernelF32, input_F32, out_F32, border_F32);}
	@Benchmark public void convolve_F32() {ConvolveImage.convolve(kernel2D_F32, input_F32, out_F32, border_F32);}
	@Benchmark public void horizontal_IL_F32() {ConvolveImage.horizontal(kernelF32, input_IL_F32, out_IL_F32, border_IL_F32);}
	@Benchmark public void vertical_IL_F32() {ConvolveImage.vertical(kernelF32, input_IL_F32, out_IL_F32, border_IL_F32);}
	@Benchmark public void convolve_IL_F32() {ConvolveImage.convolve(kernel2D_F32, input_IL_F32, out_IL_F32, border_IL_F32);}
	@Benchmark public void horizontal_PL_F32() {GConvolveImageOps.horizontal(kernelF32, input_PL_F32, out_PL_F32, border_PL_F32);}
	@Benchmark public void vertical_PL_F32() {GConvolveImageOps.vertical(kernelF32, input_PL_F32, out_PL_F32, border_PL_F32);}
	@Benchmark public void convolve_PL_F32() {GConvolveImageOps.convolve(kernel2D_F32, input_PL_F32, out_PL_F32, border_PL_F32);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("\\b"+BenchmarkConvolveImage.class.getSimpleName()+"\\b")
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
