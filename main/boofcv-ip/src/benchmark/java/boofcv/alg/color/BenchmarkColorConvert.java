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

package boofcv.alg.color;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkColorConvert {
	@Param({"true", "false"})
	public boolean concurrent;

	//	@Param({"500", "5000"})
	@Param({"800"})
	public int size;

	public Planar<GrayF32> src_F32;
	public Planar<GrayF32> dst_F32;

	public Planar<GrayU8> src_U8;
	public Planar<GrayU8> dst_U8;

	InterleavedF32 isrc_F32;
	InterleavedF32 idst_F32;
	InterleavedU8 isrc_U8;
	InterleavedU8 idst_U8;

	GrayF32 gdst_F32;
	GrayU8 gdst_U8;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);
		src_F32 = new Planar<>(GrayF32.class, size, size, 3);
		dst_F32 = new Planar<>(GrayF32.class, size, size, 3);
		src_U8 = new Planar<>(GrayU8.class, size, size, 3);
		dst_U8 = new Planar<>(GrayU8.class, size, size, 3);
		gdst_F32 = new GrayF32(size, size);
		gdst_U8 = new GrayU8(size, size);

		isrc_F32 = new InterleavedF32(size, size, 3);
		idst_F32 = new InterleavedF32(size, size, 3);
		isrc_U8 = new InterleavedU8(size, size, 3);
		idst_U8 = new InterleavedU8(size, size, 3);

		GImageMiscOps.fillUniform(isrc_F32, rand, 0, 255);
		GImageMiscOps.fillUniform(isrc_U8, rand, 0, 255);
		// copy the randomly generated values instead of creating new ones. This is done to speed up the
		// benchmark. Could use @State but then we would need to duplicate parameters
		for (int i = 0; i < 3; i++) {
			int index0 = size*size*i;
			System.arraycopy(isrc_F32.data, index0, src_F32.bands[i].data, 0, size*size);
			System.arraycopy(isrc_U8.data, index0, src_U8.bands[i].data, 0, size*size);
		}
	}

	// @formatter:off
	@Benchmark public void RGB_to_HSV_F32() {ColorHsv.rgbToHsv(src_F32, dst_F32);}
	@Benchmark public void HSV_to_RGB_F32() {ColorHsv.hsvToRgb(src_F32, dst_F32);}
	@Benchmark public void RGB_to_YUV_F32() {ColorYuv.rgbToYuv(src_F32, dst_F32);}
	@Benchmark public void YUV_to_RGB_F32() {ColorYuv.yuvToRgb(src_F32, dst_F32);}
	@Benchmark public void YUV_to_RGB_U8() {ColorYuv.yuvToRgb(src_U8, dst_U8);}
	@Benchmark public void RGB_to_GRAY_IF32() {ColorRgb.rgbToGray_Weighted(isrc_F32, gdst_F32);}
	@Benchmark public void RGB_to_GRAY_IU8() {ColorRgb.rgbToGray_Weighted(isrc_U8, gdst_U8);}
	@Benchmark public void RGB_to_GRAY_F32() {ColorRgb.rgbToGray_Weighted(src_F32, gdst_F32);}
	@Benchmark public void RGB_to_GRAY_U8() {ColorRgb.rgbToGray_Weighted(src_U8, gdst_U8);}
	@Benchmark public void RGB_to_XYZ_U8() {ColorXyz.rgbToXyz(src_U8, dst_F32);}
	@Benchmark public void RGB_to_XYZ_F32() {ColorXyz.rgbToXyz(src_F32, dst_F32);}
	@Benchmark public void RGB_to_LAB_U8() {ColorLab.rgbToLab(src_U8, dst_F32);}
	@Benchmark public void RGB_to_LAB_F32() {ColorLab.rgbToLab(src_F32, dst_F32);}
	@Benchmark public void XYZ_to_RGB_F32() {ColorXyz.xyzToRgb(src_F32, dst_F32);}
	@Benchmark public void XYZ_to_RGB_U8() {ColorXyz.xyzToRgb(src_F32, dst_U8);}
	@Benchmark public void LAB_to_RGB_F32() {ColorLab.labToRgb(src_F32, dst_F32);}
	@Benchmark public void LAB_to_RGB_U8() {ColorLab.labToRgb(src_F32, dst_U8);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkColorConvert.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
