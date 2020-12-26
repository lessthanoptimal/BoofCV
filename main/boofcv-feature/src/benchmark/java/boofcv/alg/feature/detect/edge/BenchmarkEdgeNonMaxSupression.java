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

import boofcv.alg.feature.detect.edge.impl.ImplEdgeNonMaxSuppression;
import boofcv.alg.misc.GImageMiscOps;
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
public class BenchmarkEdgeNonMaxSupression {

	@Param({"true", "false"})
	boolean concurrent;

	int imageSize = 800;

	GrayF32 intensity, output;
	GrayS8 direction4 = new GrayS8(imageSize, imageSize);
	GrayS8 direction8 = new GrayS8(imageSize, imageSize);

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		var rand = new Random(234234);

		intensity = new GrayF32(imageSize, imageSize);
		output = intensity.createSameShape();

		GImageMiscOps.fillUniform(intensity, rand, 0, 100);
		ImageMiscOps.fillUniform(direction4, rand, -1, 3);
		ImageMiscOps.fillUniform(direction8, rand, -3, 5);
	}

	// @formatter:off
	@Benchmark public void Naive4() {ImplEdgeNonMaxSuppression.naive4(intensity, direction4, output);}
	@Benchmark public void Main4() {GradientToEdgeFeatures.nonMaxSuppression4(intensity, direction4, output);}
	@Benchmark public void Naive8() {ImplEdgeNonMaxSuppression.naive8(intensity, direction4, output);}
	@Benchmark public void Main8() {GradientToEdgeFeatures.nonMaxSuppression8(intensity, direction4, output);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkEdgeNonMaxSupression.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
