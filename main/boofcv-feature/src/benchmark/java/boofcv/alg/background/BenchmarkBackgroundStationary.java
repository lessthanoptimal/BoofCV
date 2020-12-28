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

package boofcv.alg.background;

import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.ConfigBackgroundGmm;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Profiles the time to run each algorithm but ignores the time to load an image frame from the video
 *
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=1)
@SuppressWarnings({"rawtypes", "unchecked"})
public class BenchmarkBackgroundStationary {

	File file = new File(UtilIO.pathExample("background/street_intersection.mp4"));

	@Param({"SB_U8","PL_U8","SB_F32"})
	String imageTypeName;

	ImageType imageType;

	@Setup public void setup() {
		imageType = ImageType.stringToType(imageTypeName,3);
	}

	@Benchmark public void greedy() {
		var config = new ConfigBackgroundBasic(12);
		process(FactoryBackgroundModel.stationaryBasic(config,imageType));
	}

	@Benchmark public void gaussian() {
		var config =new ConfigBackgroundGaussian(12);
		process(FactoryBackgroundModel.stationaryGaussian(config,imageType));
	}

	@Benchmark public void gmm() {
		var config = new ConfigBackgroundGmm();
		process(FactoryBackgroundModel.stationaryGmm(config,imageType));
	}

	private void process(BackgroundModelStationary alg) {
		SimpleImageSequence sequence = DefaultMediaManager.INSTANCE.openVideo(file.getAbsolutePath(),imageType);
		GrayU8 background = new GrayU8(sequence.getWidth(),sequence.getHeight());

		int total = 0;
		while( sequence.hasNext() && total++ < 60 ) {
			ImageBase image = sequence.next();
			alg.updateBackground(image,background);
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkBackgroundStationary.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
