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

package boofcv.alg.feature.peak;

import boofcv.BoofTesting;
import boofcv.alg.feature.detect.peak.MeanShiftPeak;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.weights.WeightPixelGaussian_F32;
import boofcv.alg.weights.WeightPixelUniform_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F32;
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
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkPeakFinding<T extends ImageGray<T>> {

	@Param({"SB_U8", "SB_F32"})
	String type;

	int radius = 2;

	int width = 320;
	int height = 240;

	T image;
	Class<T> imageType;
	List<Point2D_F32> locations = new ArrayList<>();

	@Setup public void setup() {
		Random rand = new Random(BoofTesting.BASE_SEED);

		this.imageType = ImageType.stringToType(type, 0).getImageClass();
		image = GeneralizedImageOps.createSingleBand(imageType, width, height);
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		for (int i = 0; i < 10000; i++) {
			Point2D_F32 p = new Point2D_F32();
			p.x = rand.nextFloat()*width;
			p.y = rand.nextFloat()*height;
			locations.add(p);
		}
	}

	@Benchmark public void MeanShiftGaussian() {
		WeightPixel_F32 weight = new WeightPixelGaussian_F32();
		process(new MeanShiftPeak<>(30, 0.1f, weight, true, imageType, BorderType.EXTENDED));
	}

	@Benchmark public void MeanShiftUniform() {
		WeightPixel_F32 weight = new WeightPixelUniform_F32();
		process(new MeanShiftPeak<>(30, 0.1f, weight, true, imageType, BorderType.EXTENDED));
	}

	private void process( MeanShiftPeak<T> alg ) {
		alg.setRadius(radius);
		alg.setImage(image);
		for (int i = 0; i < locations.size(); i++) {
			Point2D_F32 p = locations.get(i);
			alg.search(p.x, p.y);
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkPeakFinding.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
