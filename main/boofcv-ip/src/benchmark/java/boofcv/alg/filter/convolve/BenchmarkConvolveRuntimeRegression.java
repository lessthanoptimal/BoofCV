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

import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard_IL;
import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard_SB;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_I32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pabeles.concurrency.GrowArray;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Runtime regression coverage for standard and normalized convolution across image types, kernel shapes, and kernel
 * widths requested by the convolution runtime-regression bounty.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
@SuppressWarnings({"UnusedDeclaration", "NullAway.Init"})
public class BenchmarkConvolveRuntimeRegression {
	private static final int WIDTH = 320;
	private static final int HEIGHT = 240;
	private static final int NUM_BANDS = 3;

	@Param({"STANDARD", "NORMALIZED"})
	public String convolution;

	@Param({
			"GRAY_U8", "GRAY_S16", "GRAY_U16", "GRAY_S32", "GRAY_F32", "GRAY_F64",
			"INTERLEAVED_U8", "INTERLEAVED_S16", "INTERLEAVED_U16", "INTERLEAVED_S32",
			"INTERLEAVED_F32", "INTERLEAVED_F64"})
	public String imageType;

	@Param({"3", "5", "7", "21"})
	public int kernelWidth;

	private Kernel1D_F32 kernelF32;
	private Kernel1D_F64 kernelF64;
	private Kernel1D_S32 kernelI32;
	private Kernel2D_F32 kernel2D_F32;
	private Kernel2D_F64 kernel2D_F64;
	private Kernel2D_S32 kernel2D_I32;

	private final GrowArray<DogArray_I32> workI32 = new GrowArray<>(DogArray_I32::new);

	private GrayU8 grayU8;
	private GrayS16 grayS16;
	private GrayU16 grayU16;
	private GrayS32 grayS32;
	private GrayF32 grayF32;
	private GrayF64 grayF64;

	private GrayU8 outGrayU8;
	private GrayS16 outGrayS16;
	private GrayS32 outGrayS32;
	private GrayF32 outGrayF32;
	private GrayF64 outGrayF64;

	private InterleavedU8 interleavedU8;
	private InterleavedS16 interleavedS16;
	private InterleavedU16 interleavedU16;
	private InterleavedS32 interleavedS32;
	private InterleavedF32 interleavedF32;
	private InterleavedF64 interleavedF64;

	private InterleavedU8 outInterleavedU8;
	private InterleavedS16 outInterleavedS16;
	private InterleavedS32 outInterleavedS32;
	private InterleavedF32 outInterleavedF32;
	private InterleavedF64 outInterleavedF64;

	private Runnable horizontal;
	private Runnable vertical;
	private Runnable convolve2D;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = false;

		kernelF32 = createKernel1D_F32(kernelWidth);
		kernelF64 = createKernel1D_F64(kernelWidth);
		kernelI32 = createKernel1D_S32(kernelWidth);
		kernel2D_F32 = createKernel2D_F32(kernelWidth);
		kernel2D_F64 = createKernel2D_F64(kernelWidth);
		kernel2D_I32 = createKernel2D_S32(kernelWidth);

		createImages();
		configureOperations();
	}

	@Benchmark public void horizontal() {
		horizontal.run();
	}

	@Benchmark public void vertical() {
		vertical.run();
	}

	@Benchmark public void convolve2D() {
		convolve2D.run();
	}

	private void configureOperations() {
		boolean standard = convolution.equals("STANDARD");
		int divisor1D = kernelWidth;
		int divisor2D = kernelWidth*kernelWidth;

		switch (imageType) {
			case "GRAY_U8" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_SB.horizontal(kernelI32, grayU8, outGrayU8, divisor1D);
					vertical = () -> ConvolveImageStandard_SB.vertical(kernelI32, grayU8, outGrayU8, divisor1D, workI32);
					convolve2D = () -> ConvolveImageStandard_SB.convolve(kernel2D_I32, grayU8, outGrayU8, divisor2D, workI32);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelI32, grayU8, outGrayU8);
					vertical = () -> ConvolveImageNormalized.vertical(kernelI32, grayU8, outGrayU8);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_I32, grayU8, outGrayU8);
				}
			}
			case "GRAY_S16" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_SB.horizontal(kernelI32, grayS16, outGrayS16, divisor1D);
					vertical = () -> ConvolveImageStandard_SB.vertical(kernelI32, grayS16, outGrayS16, divisor1D, workI32);
					convolve2D = () -> ConvolveImageStandard_SB.convolve(kernel2D_I32, grayS16, outGrayS16, divisor2D, workI32);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelI32, grayS16, outGrayS16);
					vertical = () -> ConvolveImageNormalized.vertical(kernelI32, grayS16, outGrayS16);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_I32, grayS16, outGrayS16);
				}
			}
			case "GRAY_U16" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_SB.horizontal(kernelI32, grayU16, outGrayS16, divisor1D);
					vertical = () -> ConvolveImageStandard_SB.vertical(kernelI32, grayU16, outGrayS16, divisor1D, workI32);
					convolve2D = () -> ConvolveImageStandard_SB.convolve(kernel2D_I32, grayU16, outGrayS16, divisor2D, workI32);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelI32, grayU16, outGrayS16);
					vertical = () -> ConvolveImageNormalized.vertical(kernelI32, grayU16, outGrayS16);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_I32, grayU16, outGrayS16);
				}
			}
			case "GRAY_S32" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_SB.horizontal(kernelI32, grayS32, outGrayS32, divisor1D);
					vertical = () -> ConvolveImageStandard_SB.vertical(kernelI32, grayS32, outGrayS32, divisor1D, workI32);
					convolve2D = () -> ConvolveImageStandard_SB.convolve(kernel2D_I32, grayS32, outGrayS32, divisor2D, workI32);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelI32, grayS32, outGrayS32);
					vertical = () -> ConvolveImageNormalized.vertical(kernelI32, grayS32, outGrayS32);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_I32, grayS32, outGrayS32);
				}
			}
			case "GRAY_F32" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_SB.horizontal(kernelF32, grayF32, outGrayF32);
					vertical = () -> ConvolveImageStandard_SB.vertical(kernelF32, grayF32, outGrayF32);
					convolve2D = () -> ConvolveImageStandard_SB.convolve(kernel2D_F32, grayF32, outGrayF32);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelF32, grayF32, outGrayF32);
					vertical = () -> ConvolveImageNormalized.vertical(kernelF32, grayF32, outGrayF32);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_F32, grayF32, outGrayF32);
				}
			}
			case "GRAY_F64" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_SB.horizontal(kernelF64, grayF64, outGrayF64);
					vertical = () -> ConvolveImageStandard_SB.vertical(kernelF64, grayF64, outGrayF64);
					convolve2D = () -> ConvolveImageStandard_SB.convolve(kernel2D_F64, grayF64, outGrayF64);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelF64, grayF64, outGrayF64);
					vertical = () -> ConvolveImageNormalized.vertical(kernelF64, grayF64, outGrayF64);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_F64, grayF64, outGrayF64);
				}
			}
			case "INTERLEAVED_U8" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_IL.horizontal(kernelI32, interleavedU8, outInterleavedU8, divisor1D);
					vertical = () -> ConvolveImageStandard_IL.vertical(kernelI32, interleavedU8, outInterleavedU8, divisor1D);
					convolve2D = () -> ConvolveImageStandard_IL.convolve(kernel2D_I32, interleavedU8, outInterleavedU8, divisor2D);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelI32, interleavedU8, outInterleavedU8);
					vertical = () -> ConvolveImageNormalized.vertical(kernelI32, interleavedU8, outInterleavedU8);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_I32, interleavedU8, outInterleavedU8);
				}
			}
			case "INTERLEAVED_S16" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_IL.horizontal(kernelI32, interleavedS16, outInterleavedS16, divisor1D);
					vertical = () -> ConvolveImageStandard_IL.vertical(kernelI32, interleavedS16, outInterleavedS16, divisor1D);
					convolve2D = () -> ConvolveImageStandard_IL.convolve(kernel2D_I32, interleavedS16, outInterleavedS16, divisor2D);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelI32, interleavedS16, outInterleavedS16);
					vertical = () -> ConvolveImageNormalized.vertical(kernelI32, interleavedS16, outInterleavedS16);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_I32, interleavedS16, outInterleavedS16);
				}
			}
			case "INTERLEAVED_U16" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_IL.horizontal(kernelI32, interleavedU16, outInterleavedS16, divisor1D);
					vertical = () -> ConvolveImageStandard_IL.vertical(kernelI32, interleavedU16, outInterleavedS16, divisor1D);
					convolve2D = () -> ConvolveImageStandard_IL.convolve(kernel2D_I32, interleavedU16, outInterleavedS16, divisor2D);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelI32, interleavedU16, outInterleavedS16);
					vertical = () -> ConvolveImageNormalized.vertical(kernelI32, interleavedU16, outInterleavedS16);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_I32, interleavedU16, outInterleavedS16);
				}
			}
			case "INTERLEAVED_S32" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_IL.horizontal(kernelI32, interleavedS32, outInterleavedS32, divisor1D);
					vertical = () -> ConvolveImageStandard_IL.vertical(kernelI32, interleavedS32, outInterleavedS32, divisor1D);
					convolve2D = () -> ConvolveImageStandard_IL.convolve(kernel2D_I32, interleavedS32, outInterleavedS32, divisor2D);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelI32, interleavedS32, outInterleavedS32);
					vertical = () -> ConvolveImageNormalized.vertical(kernelI32, interleavedS32, outInterleavedS32);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_I32, interleavedS32, outInterleavedS32);
				}
			}
			case "INTERLEAVED_F32" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_IL.horizontal(kernelF32, interleavedF32, outInterleavedF32);
					vertical = () -> ConvolveImageStandard_IL.vertical(kernelF32, interleavedF32, outInterleavedF32);
					convolve2D = () -> ConvolveImageStandard_IL.convolve(kernel2D_F32, interleavedF32, outInterleavedF32);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelF32, interleavedF32, outInterleavedF32);
					vertical = () -> ConvolveImageNormalized.vertical(kernelF32, interleavedF32, outInterleavedF32);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_F32, interleavedF32, outInterleavedF32);
				}
			}
			case "INTERLEAVED_F64" -> {
				if (standard) {
					horizontal = () -> ConvolveImageStandard_IL.horizontal(kernelF64, interleavedF64, outInterleavedF64);
					vertical = () -> ConvolveImageStandard_IL.vertical(kernelF64, interleavedF64, outInterleavedF64);
					convolve2D = () -> ConvolveImageStandard_IL.convolve(kernel2D_F64, interleavedF64, outInterleavedF64);
				} else {
					horizontal = () -> ConvolveImageNormalized.horizontal(kernelF64, interleavedF64, outInterleavedF64);
					vertical = () -> ConvolveImageNormalized.vertical(kernelF64, interleavedF64, outInterleavedF64);
					convolve2D = () -> ConvolveImageNormalized.convolve(kernel2D_F64, interleavedF64, outInterleavedF64);
				}
			}
			default -> throw new IllegalArgumentException("Unknown image type: " + imageType);
		}
	}

	private void createImages() {
		var rand = new Random(234);

		grayU8 = new GrayU8(WIDTH, HEIGHT);
		grayS16 = new GrayS16(WIDTH, HEIGHT);
		grayU16 = new GrayU16(WIDTH, HEIGHT);
		grayS32 = new GrayS32(WIDTH, HEIGHT);
		grayF32 = new GrayF32(WIDTH, HEIGHT);
		grayF64 = new GrayF64(WIDTH, HEIGHT);
		outGrayU8 = new GrayU8(WIDTH, HEIGHT);
		outGrayS16 = new GrayS16(WIDTH, HEIGHT);
		outGrayS32 = new GrayS32(WIDTH, HEIGHT);
		outGrayF32 = new GrayF32(WIDTH, HEIGHT);
		outGrayF64 = new GrayF64(WIDTH, HEIGHT);

		interleavedU8 = new InterleavedU8(WIDTH, HEIGHT, NUM_BANDS);
		interleavedS16 = new InterleavedS16(WIDTH, HEIGHT, NUM_BANDS);
		interleavedU16 = new InterleavedU16(WIDTH, HEIGHT, NUM_BANDS);
		interleavedS32 = new InterleavedS32(WIDTH, HEIGHT, NUM_BANDS);
		interleavedF32 = new InterleavedF32(WIDTH, HEIGHT, NUM_BANDS);
		interleavedF64 = new InterleavedF64(WIDTH, HEIGHT, NUM_BANDS);
		outInterleavedU8 = new InterleavedU8(WIDTH, HEIGHT, NUM_BANDS);
		outInterleavedS16 = new InterleavedS16(WIDTH, HEIGHT, NUM_BANDS);
		outInterleavedS32 = new InterleavedS32(WIDTH, HEIGHT, NUM_BANDS);
		outInterleavedF32 = new InterleavedF32(WIDTH, HEIGHT, NUM_BANDS);
		outInterleavedF64 = new InterleavedF64(WIDTH, HEIGHT, NUM_BANDS);

		ImageMiscOps.fillUniform(grayU8, rand, 0, 200);
		ImageMiscOps.fillUniform(grayS16, rand, -100, 100);
		fillUnsigned(grayU16.data, rand);
		ImageMiscOps.fillUniform(grayS32, rand, -100, 100);
		ImageMiscOps.fillUniform(grayF32, rand, -1.0f, 1.0f);
		ImageMiscOps.fillUniform(grayF64, rand, -1.0, 1.0);

		ImageMiscOps.fillUniform(interleavedU8, rand, 0, 200);
		ImageMiscOps.fillUniform(interleavedS16, rand, -100, 100);
		fillUnsigned(interleavedU16.data, rand);
		ImageMiscOps.fillUniform(interleavedS32, rand, -100, 100);
		ImageMiscOps.fillUniform(interleavedF32, rand, -1.0f, 1.0f);
		ImageMiscOps.fillUniform(interleavedF64, rand, -1.0, 1.0);
	}

	private static void fillUnsigned( short[] data, Random rand ) {
		for (int i = 0; i < data.length; i++) {
			data[i] = (short)rand.nextInt(200);
		}
	}

	private static Kernel1D_F32 createKernel1D_F32( int width ) {
		float[] data = new float[width];
		Arrays.fill(data, 1.0f/width);
		return new Kernel1D_F32(width, data);
	}

	private static Kernel1D_F64 createKernel1D_F64( int width ) {
		double[] data = new double[width];
		Arrays.fill(data, 1.0/width);
		return new Kernel1D_F64(width, data);
	}

	private static Kernel1D_S32 createKernel1D_S32( int width ) {
		int[] data = new int[width];
		Arrays.fill(data, 1);
		return new Kernel1D_S32(width, data);
	}

	private static Kernel2D_F32 createKernel2D_F32( int width ) {
		float[] data = new float[width*width];
		Arrays.fill(data, 1.0f/(width*width));
		return new Kernel2D_F32(width, data);
	}

	private static Kernel2D_F64 createKernel2D_F64( int width ) {
		double[] data = new double[width*width];
		Arrays.fill(data, 1.0/(width*width));
		return new Kernel2D_F64(width, data);
	}

	private static Kernel2D_S32 createKernel2D_S32( int width ) {
		int[] data = new int[width*width];
		Arrays.fill(data, 1);
		return new Kernel2D_S32(width, data);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvolveRuntimeRegression.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
