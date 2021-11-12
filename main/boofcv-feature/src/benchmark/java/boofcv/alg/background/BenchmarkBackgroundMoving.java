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

package boofcv.alg.background;

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.ConfigBackgroundGmm;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
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
@Fork(value = 1)
@SuppressWarnings({"rawtypes", "unchecked"})
public class BenchmarkBackgroundMoving {
	@Param({"true", "false"})
	boolean concurrent;

	@Param({"SB_U8", "PL_U8", "IL_U8", "SB_F32"})
	String imageTypeName;

	ImageType imageType;

	PointTransformHomography_F32 transform = new PointTransformHomography_F32();

	@Setup public void setup() {
		imageType = ImageType.stringToType(imageTypeName, 3);
		BoofConcurrency.USE_CONCURRENT = concurrent;
	}

	@Benchmark public void basic() {
		var config = new ConfigBackgroundBasic(12);
		process(FactoryBackgroundModel.movingBasic(config, transform, imageType));
	}

	@Benchmark public void gaussian() {
		var config = new ConfigBackgroundGaussian(12);
		process(FactoryBackgroundModel.movingGaussian(config, transform, imageType));
	}

	@Benchmark public void gmm() {
		var config = new ConfigBackgroundGmm();
		process(FactoryBackgroundModel.movingGmm(config, transform, imageType));
	}

	private void process( BackgroundModelMoving alg ) {
		var rand = new Random(234);

		// Create a very basic synthetic scene. Originally this used a real video sequence but reading the image
		// was heavily skewing profiling results.
		ImageBase image = imageType.createImage(400, 300);
		var background = new GrayU8(image.getWidth(), image.getHeight());

		// Make sure the input is non-uniform to avoid the optimizer from being too smart
		var homeToWorld = new Homography2D_F32(1,0,0,0,1,0,0,0,1);
		GImageMiscOps.fillUniform(image, rand, 0, 10);

		alg.initialize(image.width, image.height, homeToWorld);

		for (int frame = 0; frame < 60; frame++) {
			var motion = new Homography2D_F32(1,0,0.01f,0,1,-0.04f,0,0,1);

			// Change the image so it's not constant
			GPixelMath.plus(image, rand.nextDouble()*10, image);
			alg.updateBackground(motion, image);
			alg.segment(motion, image, background);
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkBackgroundMoving.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
