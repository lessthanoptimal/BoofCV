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

package boofcv.disparity;

import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GConvertImage;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.struct.image.GrayF32;
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
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkStereoDisparitySparse {
	static final int width = 800;
	static final int height = 10;
	static final int min = 1;
	static final int max = 60;
	static final int radiusX = 2;
	static final int radiusY = 2;

	final GrayU8 left_U8 = new GrayU8(width, height);
	final GrayU8 right_U8 = new GrayU8(width, height);

	final GrayF32 left_F32 = new GrayF32(width, height);
	final GrayF32 right_F32 = new GrayF32(width, height);

	StereoDisparitySparse<GrayU8> sad_sub_U8;
	StereoDisparitySparse<GrayU8> sad_pix_U8;
	StereoDisparitySparse<GrayF32> sad_sub_F32;
	StereoDisparitySparse<GrayF32> sad_pix_F32;

	StereoDisparitySparse<GrayU8> ncc_sub_U8;
	StereoDisparitySparse<GrayU8> ncc_pix_U8;
	StereoDisparitySparse<GrayF32> ncc_sub_F32;
	StereoDisparitySparse<GrayF32> ncc_pix_F32;

	StereoDisparitySparse<GrayU8> census_sub_U8;
	StereoDisparitySparse<GrayU8> census_pix_U8;
	StereoDisparitySparse<GrayF32> census_sub_F32;
	StereoDisparitySparse<GrayF32> census_pix_F32;

	@Setup public void setup() {
		var rand = new Random(234234);

		GImageMiscOps.fillUniform(left_U8, rand, 0, 30);
		GImageMiscOps.fillUniform(right_U8, rand, 0, 30);
		GConvertImage.convert(left_U8, left_F32);
		GConvertImage.convert(right_U8, right_F32);

		var configBM = new ConfigDisparityBM();
		configBM.errorType = DisparityError.SAD;
		configBM.disparityMin = min;
		configBM.disparityRange = max - min + 1;
		configBM.regionRadiusX = radiusX;
		configBM.regionRadiusY = radiusY;

		configBM.subpixel = false;
		sad_pix_U8 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayU8.class);
		configBM.subpixel = true;
		sad_sub_U8 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayU8.class);

		configBM.errorType = DisparityError.NCC;
		configBM.subpixel = false;
		ncc_pix_U8 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayU8.class);
		configBM.subpixel = true;
		ncc_sub_U8 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayU8.class);

		configBM.errorType = DisparityError.CENSUS;
		configBM.subpixel = false;
		census_pix_U8 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayU8.class);
		configBM.subpixel = true;
		census_sub_U8 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayU8.class);

		configBM.subpixel = false;
		sad_pix_F32 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);
		configBM.subpixel = true;
		sad_sub_F32 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);

		configBM.errorType = DisparityError.NCC;
		configBM.subpixel = false;
		ncc_pix_F32 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);
		configBM.subpixel = true;
		ncc_sub_F32 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);

		configBM.errorType = DisparityError.CENSUS;
		configBM.subpixel = false;
		census_pix_F32 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);
		configBM.subpixel = true;
		census_sub_F32 = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);
	}

	// @formatter:off
	@Benchmark public void sad_pix_U8() {processRowI(sad_pix_U8);}
	@Benchmark public void sad_sub_U8() {processRowI(sad_sub_U8);}
	@Benchmark public void ncc_pix_U8() {processRowI(ncc_pix_U8);}
	@Benchmark public void ncc_sub_U8() {processRowI(ncc_sub_U8);}
	@Benchmark public void census_pix_U8() {processRowI(census_pix_U8);}
	@Benchmark public void census_sub_U8() {processRowI(census_sub_U8);}
	@Benchmark public void sad_pix_F32() {processRowF(sad_pix_F32);}
	@Benchmark public void sad_sub_F32() {processRowF(sad_sub_F32);}
	@Benchmark public void ncc_pix_F32() {processRowF(ncc_pix_F32);}
	@Benchmark public void ncc_sub_F32() {processRowF(ncc_sub_F32);}
	@Benchmark public void census_pix_F32() {processRowF(census_pix_F32);}
	@Benchmark public void census_sub_F32() {processRowF(census_sub_F32);}
	// @formatter:on

	private void processRowI( StereoDisparitySparse<GrayU8> alg) {
		alg.setImages(left_U8, right_U8);
		for (int x = 0; x < width; x++) {
			alg.process(x, height/2);
		}
	}

	private void processRowF(StereoDisparitySparse<GrayF32> alg) {
		alg.setImages(left_F32, right_F32);
		for (int x = 0; x < width; x++) {
			alg.process(x, height/2);
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkStereoDisparitySparse.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
