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

package boofcv.alg.tracker.klt;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unchecked", "rawtypes"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkKltTracker {

	int width = 300, height = 200;

	@Param({"GrayF32", "GrayU8"})
	String imageType;

	@Param({"1", "30"})
	int radius;

	Random rand = new Random(234234);

	ImageGray image;
	ImageGray derivX, derivY;
	Class type;
	KltTracker klt;

	KltFeature feature;

	@Setup public void setup() {
		if (imageType.equals("GrayF32")) {
			type = GrayF32.class;
		} else {
			type = GrayU8.class;
		}

		var config = new ConfigKlt();
		config.maxIterations = 30;
		config.minPositionDelta = 1e-4f;
		klt = FactoryTrackerAlg.klt(config, (Class)type, null);

		image = (ImageGray)klt.interpInput.getImageType().createImage(width, height);
		derivX = (ImageGray)klt.interpDeriv.getImageType().createImage(width, height);
		derivY = (ImageGray)klt.interpDeriv.getImageType().createImage(width, height);

		GImageMiscOps.addUniform(image, rand, 0, 200);
		GImageMiscOps.addUniform(derivX, rand, 0, 200);
		GImageMiscOps.addUniform(derivY, rand, 0, 200);

		klt.setImage(image, derivX, derivY, 1);
		feature = new KltFeature(radius);
	}

	@Benchmark public void setDescription() {
		feature.x = width/2;
		feature.y = height/2;
		klt.setDescription(feature);
	}

	@Benchmark public void track() {
		feature.x = width/2;
		feature.y = height/2;
		klt.track(feature);
	}

	static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkKltTracker.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
