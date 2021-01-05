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

package boofcv.alg.distort;

import boofcv.struct.distort.PixelTransform;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.point.Point2D_F32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkPixelTransform {

	private static final int size = 800;

	Point2D_F32 distorted = new Point2D_F32();

	Affine2D_F32 affine = new Affine2D_F32();
	Homography2D_F32 homography = new Homography2D_F32();

	@Setup public void configure() {
		affine.setTo(1.1f, 0.1f, -0.1f, 0.9f, 0.5f, -0.2f);
		homography.setTo(1.1f, 0.1f, 0.5f, -0.1f, 0.9f, -0.2f, 0, 0, 1);
	}

	// @formatter:off
	@Benchmark public void affine() {process(new PixelTransformAffine_F32(affine));}
	@Benchmark public void homography() {process(new PixelTransformHomography_F32(homography));}
	// @formatter:on

	void process( PixelTransform<Point2D_F32> alg ) {
		for (int y = 0; y < size; y++)
			for (int x = 0; x < size; x++)
				alg.compute(x, y, distorted);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkPixelTransform.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
