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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
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
public class BenchmarkGradientToEdge {

	@Param({"true", "false"})
	boolean concurrent;

	final int imageSize = 800;

	GrayF32 derivX_F32 = new GrayF32(imageSize, imageSize);
	GrayF32 derivY_F32 = new GrayF32(imageSize, imageSize);

	GrayF32 intensity_F32 = new GrayF32(imageSize, imageSize);
	GrayF32 orientation_F32 = new GrayF32(imageSize, imageSize);

	GrayS8 direction = new GrayS8(imageSize, imageSize);

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		var rand = new Random(234234);

		ImageMiscOps.fillUniform(derivX_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(derivY_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(orientation_F32, rand, (float)(-Math.PI/2.0), (float)(Math.PI/2.0));
	}

	// @formatter:off
	@Benchmark public void Euclidian_F32() {GradientToEdgeFeatures.intensityE(derivX_F32,derivY_F32,intensity_F32);}
	@Benchmark public void Abs_F32() {GradientToEdgeFeatures.intensityAbs(derivX_F32,derivY_F32,intensity_F32);}
	@Benchmark public void Orientation_F32() {GradientToEdgeFeatures.direction(derivX_F32,derivY_F32,intensity_F32);}
	@Benchmark public void Orientation2_F32() {GradientToEdgeFeatures.direction2(derivX_F32,derivY_F32,intensity_F32);}
	@Benchmark public void Discretize4() {GradientToEdgeFeatures.discretizeDirection4(intensity_F32,direction);}
	@Benchmark public void Discretize8() {GradientToEdgeFeatures.discretizeDirection8(intensity_F32,direction);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkGradientToEdge.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
