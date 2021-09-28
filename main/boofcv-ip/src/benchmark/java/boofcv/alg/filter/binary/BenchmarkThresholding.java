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

package boofcv.alg.filter.binary;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.ConvertImage;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkThresholding {
	@Param({"true", "false"})
	public boolean concurrent;

	//	@Param({"5","50"})
	@Param("50")
	public int region;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	GrayU8 inputU8 = new GrayU8(size, size);
	GrayF32 inputF32 = new GrayF32(size, size);
	GrayU8 output = new GrayU8(size, size);

	// declare algorithms here so that they can recycle memory
	InputToBinary<GrayU8> globalOtsuU8;
	InputToBinary<GrayU8> globalEntropyU8;

	InputToBinary<GrayU8> localOtsuU8;
	InputToBinary<GrayU8> localMeanU8;
	InputToBinary<GrayU8> localGaussianU8;
	InputToBinary<GrayF32> localNiblackF32;
	InputToBinary<GrayF32> localSauvolaF32;
	InputToBinary<GrayF32> localWolfF32;
	InputToBinary<GrayF32> localNickF32;

	InputToBinary<GrayU8> blockMeanU8;
	InputToBinary<GrayU8> blockMinMaxU8;
	InputToBinary<GrayU8> blockOtsuU8;

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		inputU8.reshape(size, size);
		inputF32.reshape(size, size);
		output.reshape(size, size);

		ImageMiscOps.fillUniform(inputU8, rand, 0, 200);
		ConvertImage.convert(inputU8, inputF32);

		ConfigLength configLength = ConfigLength.fixed(region);

		globalOtsuU8 = FactoryThresholdBinary.globalOtsu(0, 255, 1.0, true, GrayU8.class);
		globalEntropyU8 = FactoryThresholdBinary.globalEntropy(0, 255, 1.0, true, GrayU8.class);

		localOtsuU8 = FactoryThresholdBinary.localOtsu(configLength, 1.0, true, true, 0.1, GrayU8.class);
		localMeanU8 = FactoryThresholdBinary.localMean(configLength, 1.0, true, GrayU8.class);
		localGaussianU8 = FactoryThresholdBinary.localGaussian(configLength, 1.0, true, GrayU8.class);
		localNiblackF32 = FactoryThresholdBinary.localNiblack(configLength, true, 0.3f, GrayF32.class);
		localSauvolaF32 = FactoryThresholdBinary.localSauvola(configLength, true, 0.3f, GrayF32.class);
		localWolfF32 = FactoryThresholdBinary.localWolf(configLength, true, 0.3f, GrayF32.class);
		localNickF32 = FactoryThresholdBinary.localNick(configLength, true, -0.15f, GrayF32.class);

		blockMeanU8 = FactoryThresholdBinary.blockMean(configLength, 1.0, true, true, GrayU8.class);
		blockMinMaxU8 = FactoryThresholdBinary.blockMinMax(configLength, 1.0, true, true, 5, GrayU8.class);
		blockOtsuU8 = FactoryThresholdBinary.blockOtsu(configLength, 1.0, true, true, true, 0.1, GrayU8.class);
	}

	// @formatter:off
	@Benchmark public void globalThreshold() {ThresholdImageOps.threshold(inputU8, output, 100, true);}
	@Benchmark public void globalOtsu() {globalOtsuU8.process(inputU8, output);}
	@Benchmark public void globalEntropy() {globalEntropyU8.process(inputU8, output);}
	@Benchmark public void localOtsu() {localOtsuU8.process(inputU8, output);}
	@Benchmark public void localMean() {localMeanU8.process(inputU8, output);}
	@Benchmark public void localGaussian() {localGaussianU8.process(inputU8, output);}
	@Benchmark public void localNiblack() {localNiblackF32.process(inputF32, output);}
	@Benchmark public void localSauvola() {localSauvolaF32.process(inputF32, output);}
	@Benchmark public void localWolf() {localWolfF32.process(inputF32, output);}
	@Benchmark public void localNick() {localNickF32.process(inputF32, output);}
	@Benchmark public void blockMean() {blockMeanU8.process(inputU8, output);}
	@Benchmark public void blockMinMax() {blockMinMaxU8.process(inputU8, output);}
	@Benchmark public void blockOtsu() {blockOtsuU8.process(inputU8, output);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkThresholding.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
