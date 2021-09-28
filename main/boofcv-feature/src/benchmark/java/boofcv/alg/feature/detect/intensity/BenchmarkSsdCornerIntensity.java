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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
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
public class BenchmarkSsdCornerIntensity {
	@Param({"true", "false"})
	public boolean concurrent = false;

	@Param({"false", "true"})
	public boolean weighted = true;

	//	@Param({"500","5000"})
	public int size = 800;

	//	@Param({"2","5","20"})
	@Param({"2", "20"})
	public int radius = 5;

	GrayF32 derivX_F32 = new GrayF32(1, 1);
	GrayF32 derivY_F32 = new GrayF32(1, 1);
	GrayS16 derivX_S16 = new GrayS16(1, 1);
	GrayS16 derivY_S16 = new GrayS16(1, 1);

	GrayF32 intensity = new GrayF32(1, 1);

	GradientCornerIntensity<GrayS16> shitomasi_S16;
	GradientCornerIntensity<GrayF32> shitomasi_F32;
	GradientCornerIntensity<GrayS16> harris_S16;
	GradientCornerIntensity<GrayF32> harris_F32;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;

		derivX_F32.reshape(size, size);
		derivY_F32.reshape(size, size);
		derivX_S16.reshape(size, size);
		derivY_S16.reshape(size, size);
		intensity.reshape(size, size);

		Random rand = new Random(234);
		ImageMiscOps.fillUniform(derivX_F32, rand, 0, 200);
		ImageMiscOps.fillUniform(derivY_F32, rand, 0, 200);
		ImageMiscOps.fillUniform(derivX_S16, rand, 0, 200);
		ImageMiscOps.fillUniform(derivY_S16, rand, 0, 200);

		shitomasi_S16 = FactoryIntensityPointAlg.shiTomasi(radius, weighted, GrayS16.class);
		shitomasi_F32 = FactoryIntensityPointAlg.shiTomasi(radius, weighted, GrayF32.class);

		harris_S16 = FactoryIntensityPointAlg.harris(radius, 0.04f, weighted, GrayS16.class);
		harris_F32 = FactoryIntensityPointAlg.harris(radius, 0.04f, weighted, GrayF32.class);
	}

	// @formatter:off
	@Benchmark public void ShiTomasi_S16() {shitomasi_S16.process(derivX_S16,derivY_S16,intensity);}
	@Benchmark public void ShiTomasi_F32() {shitomasi_F32.process(derivX_F32,derivY_F32,intensity);}
	@Benchmark public void Harris_S16() {harris_S16.process(derivX_S16,derivY_S16,intensity);}
	@Benchmark public void Harris_F32() {harris_F32.process(derivX_F32,derivY_F32,intensity);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkSsdCornerIntensity.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
