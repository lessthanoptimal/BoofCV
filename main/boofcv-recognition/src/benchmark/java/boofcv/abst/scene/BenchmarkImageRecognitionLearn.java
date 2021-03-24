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

package boofcv.abst.scene;

import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.io.UtilIO;
import boofcv.io.image.ImageFileListIterator;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkImageRecognitionLearn {
	@Param({"true","false"})
	public boolean concurrent;

	String imagePath = UtilIO.pathExample("recognition/vacation");
	List<String> images;

	SceneRecognition<GrayU8> recognizer;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		images = UtilIO.listByPrefix(imagePath, null, ".jpg");
		Collections.sort(images);

		var config = new ConfigFeatureToSceneRecognition();
		config.recognizeNister2006.tree.branchFactor = 4;
		config.recognizeNister2006.tree.maximumLevel = 4;
		recognizer = FactorySceneRecognition.createFeatureToScene(config, ImageType.SB_U8);
	}

	@Benchmark public void Nister2006() {
		recognizer.learnModel(new ImageFileListIterator<>(images, recognizer.getImageType()));
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImageRecognitionLearn.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
