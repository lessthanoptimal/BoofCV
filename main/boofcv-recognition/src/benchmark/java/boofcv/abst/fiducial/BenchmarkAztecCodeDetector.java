/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial;

import boofcv.BoofTesting;
import boofcv.abst.distort.FDistort;
import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.alg.fiducial.aztec.AztecEncoder;
import boofcv.alg.fiducial.aztec.AztecGenerator;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkAztecCodeDetector {

	List<GrayU8> images = new ArrayList<>();

	AztecCodeDetector<GrayU8> detector = FactoryFiducial.aztec(null, GrayU8.class);

	/**
	 * Generate a set of synthetic images with two markers in it to test against
	 */
	@Setup public void setup() {
		var rand = new Random(BoofTesting.BASE_SEED);
		AztecCode marker1 = new AztecEncoder().addAutomatic("small").fixate();
		AztecCode marker2 = new AztecEncoder().addAutomatic("Much Larger Than the Oth9er!!#").fixate();

		GrayU8 image1 = AztecGenerator.renderImage(5, 0, marker1);
		GrayU8 image2 = AztecGenerator.renderImage(5, 0, marker2);

		var fullImage = new GrayU8(image1.width + image2.width + 50, image1.width + image2.width + 100);
		GImageMiscOps.fillUniform(fullImage, rand, 50, 150);

		GImageMiscOps.copy(0, 0, 10, 15, image1.width, image1.height, image1, fullImage);
		GImageMiscOps.copy(0, 0, 20 + image1.width, 50, image2.width, image2.height, image2, fullImage);

		images.add(fullImage);

		// manually checked that all the distorted images have markers inside the image
		for (int i = 0; i < 4; i++) {
			GrayU8 distorted = fullImage.createSameShape();
			new FDistort(fullImage, distorted).affine(
					1.0 + rand.nextGaussian()*0.1, rand.nextGaussian()*0.01,
					rand.nextGaussian()*0.01, 1.0 + rand.nextGaussian()*0.1,
					rand.nextGaussian()*0.5, rand.nextGaussian()*0.5).apply();
			images.add(distorted);
		}
	}

	@Benchmark public void aztec() {
		for (int imageIdx = 0; imageIdx < images.size(); imageIdx++) {
			detector.process(images.get(imageIdx));
			// sanity check to make sure everything is running as expected
			BoofMiscOps.checkEq(2, detector.getDetections().size());
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkAztecCodeDetector.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
