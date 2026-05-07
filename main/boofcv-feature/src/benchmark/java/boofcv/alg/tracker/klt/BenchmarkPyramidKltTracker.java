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
import boofcv.alg.transform.pyramid.PyramidDiscreteAverage;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.ImagePyramid;
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
public class BenchmarkPyramidKltTracker {

	int width = 300, height = 200;
	int numLevels = 3;

	@Param({"GrayF32", "GrayU8"})
	String imageType;

	@Param({"1", "30"})
	int radius;

	Random rand = new Random(234234);

	ImagePyramid image;
	ImageGray[] derivX, derivY;
	Class type;
	PyramidKltTracker klt;

	PyramidKltFeature feature;

	@Setup public void setup() {
		if (imageType.equals("GrayF32")) {
			type = GrayF32.class;
		} else {
			type = GrayU8.class;
		}

		var config = new ConfigKlt();
		config.maxIterations = 30;
		config.minPositionDelta = 1e-4f;
		klt = FactoryTrackerAlg.kltPyramid(config, type, null);

		var configLevels = new ConfigDiscreteLevels();
		configLevels.numLevelsRequested = numLevels;

		image = new PyramidDiscreteAverage(ImageType.single(type), false, configLevels);
		image.initialize(width, height);

		ImageType derivType = klt.tracker.interpDeriv.getImageType();

		derivX = new ImageGray[numLevels];
		derivY = new ImageGray[numLevels];
		for (int i = 0; i < image.getNumLayers(); i++) {
			ImageGray level = (ImageGray)image.getLayer(i);
			GImageMiscOps.addUniform(level, rand, 0, 200);
			derivX[i] = (ImageGray)derivType.createImage(level.width, level.height);
			derivY[i] = (ImageGray)derivType.createImage(level.width, level.height);
			GImageMiscOps.addUniform(derivX[i], rand, 0, 200);
			GImageMiscOps.addUniform(derivY[i], rand, 0, 200);
		}

		klt.setImage(image, derivX, derivY, 1);
		feature = new PyramidKltFeature(numLevels, radius);
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
				.include(BenchmarkPyramidKltTracker.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
