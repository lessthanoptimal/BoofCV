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

package boofcv.alg.interpolate;

import boofcv.alg.interpolate.impl.BilinearRectangle_F32;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkInterpolateRegion {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int regionSize = 400;

	static GrayF32 imgFloat32 = new GrayF32(imgWidth, imgHeight);
	static GrayU8 imgInt8 = new GrayU8(imgWidth, imgHeight);
	static GrayF32 outputImage = new GrayF32(regionSize, regionSize);

	// defines the region its interpolation
	static float start = 10.1f;

	BilinearRectangle_F32 alg = new BilinearRectangle_F32(imgFloat32);

	@Setup public void setup() {
		Random rand = new Random(234);
		ImageMiscOps.fillUniform(imgInt8, rand, 0, 100);
		ImageMiscOps.fillUniform(imgFloat32, rand, 0, 200);
	}

	@Benchmark public void Bilinear_F32() {alg.region(start, start, outputImage);}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkInterpolateRegion.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
