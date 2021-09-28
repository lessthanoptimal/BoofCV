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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for interpolating on a per-pixel basis
 *
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkInterpolatePixel {
	@Param({"true", "false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	GrayF32 inputF32 = new GrayF32(size, size);
	GrayF32 outputF32 = new GrayF32(size, size);

	// defines the region its interpolation
	static float start = 10.1f;
	static float end = 310.1f;
	static float step = 1f;

	InterpolatePixelS<GrayF32> bilinear_sb;
	InterpolatePixelS<GrayF32> nearest_sb;

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		inputF32.reshape(size, size);
		outputF32.reshape(size, size);

		GImageMiscOps.fillUniform(inputF32, rand, 0, 200);

		bilinear_sb = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
		nearest_sb = FactoryInterpolation.nearestNeighborPixelS(GrayF32.class);
	}

	@Benchmark
	public void bilinear_F32() {
		bilinear_sb.setImage(inputF32);
		int idx = 0;
		for (float x = start; x <= end; x += step)
			for (float y = start; y <= end; y += step)
				outputF32.data[idx++] = bilinear_sb.get(x, y);
	}

	@Benchmark
	public void nn_F32() {
		nearest_sb.setImage(inputF32);
		int idx = 0;
		for (float x = start; x <= end; x += step)
			for (float y = start; y <= end; y += step)
				outputF32.data[idx++] = nearest_sb.get(x, y);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkInterpolatePixel.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
