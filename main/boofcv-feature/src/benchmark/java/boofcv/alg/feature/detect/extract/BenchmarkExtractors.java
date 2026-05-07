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

package boofcv.alg.feature.detect.extract;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
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
@Fork(value = 1)
public class BenchmarkExtractors {
	private static float threshold = 1.0f;

	@Param({"true", "false"})
	public boolean concurrent;

	//	@Param({"500","5000"})
	public int width = 1000;

	//	@Param({"2", "5", "20"})
	@Param({"2", "20"})
	public int radius;

	GrayF32 intensity = new GrayF32(1, 1);
	QueueCorner corners;

	NonMaxSuppression<QueueCorner> blockStrictMax;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;

		intensity.reshape(width, width);
		corners = new QueueCorner();

		var rand = new Random(234);
		ImageMiscOps.fillUniform(intensity, rand, 0, 200);

		var config = new ConfigExtract();
		config.radius = radius;
		config.detectMaximums = true;
		config.detectMinimums = false;
		config.threshold = threshold;
		config.useStrictRule = true;
		blockStrictMax = FactoryFeatureExtractor.nonmax(config, 1);
	}

	@Benchmark public void blockStrictMax() {
		blockStrictMax.process(intensity, null, null, corners, corners);
	}

	@Benchmark public void blockStrictMinMax() {
		var config = new ConfigExtract();
		config.radius = radius;
		config.detectMaximums = true;
		config.detectMinimums = true;
		config.threshold = threshold;
		config.useStrictRule = true;
		NonMaxSuppression<QueueCorner> alg = FactoryFeatureExtractor.nonmax(config, 1);
		alg.process(intensity, null, null, corners, corners);
	}

	@Benchmark public void blockRelaxedMax() {
		var config = new ConfigExtract();
		config.radius = radius;
		config.detectMaximums = true;
		config.detectMinimums = false;
		config.threshold = threshold;
		config.useStrictRule = false;
		NonMaxSuppression<QueueCorner> alg = FactoryFeatureExtractor.nonmax(config, 1);
		alg.process(intensity, null, null, corners, corners);
	}

	@Benchmark public void blockRelaxedMinMax() {
		var config = new ConfigExtract();
		config.radius = radius;
		config.detectMaximums = true;
		config.detectMinimums = true;
		config.threshold = threshold;
		config.useStrictRule = false;
		NonMaxSuppression<QueueCorner> alg = FactoryFeatureExtractor.nonmax(config, 1);
		alg.process(intensity, null, null, corners, corners);
	}

	@Benchmark public void naiveStrictMax() {
		var alg = new NonMaxExtractorNaive<QueueCorner>(true);
		alg.storageAccess(QueueCorner::append, QueueCorner::reset);
		alg.radius = radius;
		alg.thresh = threshold;
		alg.process(intensity, corners);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkExtractors.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
