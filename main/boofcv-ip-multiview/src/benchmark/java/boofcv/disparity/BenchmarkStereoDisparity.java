/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.disparity.StereoDisparity;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GConvertImage;
import boofcv.factory.disparity.*;
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
public class BenchmarkStereoDisparity {
	@Param({"true", "false"})
	public boolean concurrent = false;

	static final int width = 800;
	static final int height = 600;
	static final int min = 0;
	static final int max = 20;
	static final int radiusX = 2;
	static final int radiusY = 2;

	final GrayU8 left = new GrayU8(width, height);
	final GrayU8 right = new GrayU8(width, height);

	final GrayF32 left_F32 = new GrayF32(width, height);
	final GrayF32 right_F32 = new GrayF32(width, height);

	StereoDisparity<GrayU8, GrayU8> blockSad_U8;     // simplest algorithm
	StereoDisparity<GrayF32, GrayU8> blockSad_F32;   // see how processing a float image changes it
	StereoDisparity<GrayU8, GrayF32> blockSad_U8_Sub;    // now with sub-pixel accuracy
	StereoDisparity<GrayU8, GrayF32> blockFive;
	StereoDisparity<GrayU8, GrayF32> blockFiveCensus;
	StereoDisparity<GrayU8, GrayF32> sgm;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		var rand = new Random(234234);

		GImageMiscOps.fillUniform(left, rand, 0, 30);
		GImageMiscOps.fillUniform(right, rand, 0, 30);
		GConvertImage.convert(left, left_F32);
		GConvertImage.convert(right, right_F32);

		var configBM = new ConfigDisparityBM();
		configBM.errorType = DisparityError.SAD;
		configBM.disparityMin = min;
		configBM.disparityRange = max - min + 1;
		configBM.regionRadiusX = radiusX;
		configBM.regionRadiusY = radiusY;
		configBM.subpixel = false;
		blockSad_U8 = FactoryStereoDisparity.blockMatch(configBM, GrayU8.class, GrayU8.class);
		blockSad_F32 = FactoryStereoDisparity.blockMatch(configBM, GrayF32.class, GrayU8.class);
		configBM.subpixel = true;
		blockSad_U8_Sub = FactoryStereoDisparity.blockMatch(configBM, GrayU8.class, GrayF32.class);

		var configBm5 = new ConfigDisparityBMBest5();
		configBm5.errorType = DisparityError.SAD;
		configBm5.disparityMin = min;
		configBm5.disparityRange = max - min + 1;
		configBm5.regionRadiusX = radiusX;
		configBm5.regionRadiusY = radiusY;
		configBm5.subpixel = true;
		blockFive = FactoryStereoDisparity.blockMatchBest5(configBm5, GrayU8.class, GrayF32.class);
		configBm5.errorType = DisparityError.CENSUS;
		blockFiveCensus = FactoryStereoDisparity.blockMatchBest5(configBm5, GrayU8.class, GrayF32.class);

		var configSgm = new ConfigDisparitySGM();
		configSgm.errorType = DisparitySgmError.ABSOLUTE_DIFFERENCE;
		configSgm.disparityMin = min;
		configSgm.disparityRange = max - min + 1;
		configSgm.configBlockMatch.radiusX = radiusX;
		configSgm.configBlockMatch.radiusY = radiusY;
		configSgm.subpixel = true;
		configSgm.useBlocks = true;
		configSgm.paths = ConfigDisparitySGM.Paths.P4;
		sgm = FactoryStereoDisparity.sgm(configSgm, GrayU8.class, GrayF32.class);
	}

	// @formatter:off
	@Benchmark public void BlockSad_U8() {blockSad_U8.process(left, right);}
	@Benchmark public void BlockSad_F32() {blockSad_F32.process(left_F32, right_F32);}
	@Benchmark public void BlockSad_U8_Sub() {blockSad_U8_Sub.process(left, right);}
	@Benchmark public void BlockFiveSad() {blockFive.process(left, right);}
	@Benchmark public void BlockFiveCensus() {blockFiveCensus.process(left, right);}
	@Benchmark public void SGM_SAD() {sgm.process(left, right);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkStereoDisparity.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
