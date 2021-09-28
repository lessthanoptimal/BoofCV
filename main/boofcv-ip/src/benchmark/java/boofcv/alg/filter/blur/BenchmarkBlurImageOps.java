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

package boofcv.alg.filter.blur;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import pabeles.concurrency.GrowArray;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkBlurImageOps {
	public static final int radius = 5;

	@Param({"true", "false"})
	public boolean concurrent;

	@Param({"1000"})
	public int size;

	private final GrayU8 inputU8 = new GrayU8(size, size);
	private final GrayU8 outputU8 = new GrayU8(size, size);
	private final GrayU8 storageU8 = new GrayU8(size, size);
	private final GrowArray<DogArray_I32> workI32 = new GrowArray<>(DogArray_I32::new);
	private final GrowArray<DogArray_I32> growArrayI32 = new GrowArray<>(DogArray_I32::new);

	private final GrayF32 inputF32 = new GrayF32(size, size);
	private final GrayF32 outputF32 = new GrayF32(size, size);
	private final GrayF32 storageF32 = new GrayF32(size, size);
	private final GrowArray<DogArray_F32> workF32 = new GrowArray<>(DogArray_F32::new);
	private final GrowArray<DogArray_F32> growArrayF32 = new GrowArray<>(DogArray_F32::new);

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		inputU8.reshape(size, size);
		outputU8.reshape(size, size);
		storageU8.reshape(size, size);

		ImageMiscOps.fillUniform(inputU8, rand, 0, 200);
		ImageMiscOps.fillUniform(outputU8, rand, 0, 200);
		ImageMiscOps.fillUniform(storageU8, rand, 0, 200);

		inputF32.reshape(size, size);
		outputF32.reshape(size, size);
		storageF32.reshape(size, size);

		ImageMiscOps.fillUniform(inputF32, rand, 0, 200);
		ImageMiscOps.fillUniform(outputF32, rand, 0, 200);
		ImageMiscOps.fillUniform(storageF32, rand, 0, 200);
	}

	// @formatter:off
	@Benchmark public void mean_U8() { BlurImageOps.mean(inputU8, outputU8, radius, storageU8, workI32); }
	@Benchmark public void gaussian_U8() { BlurImageOps.gaussian(inputU8, outputU8, -1, radius, storageU8); }
	@Benchmark public void median_U8() { BlurImageOps.median(inputU8, outputU8, radius, radius, growArrayI32); }
	@Benchmark public void mean_F32() { BlurImageOps.mean(inputF32, outputF32, radius, storageF32, workF32); }
	@Benchmark public void gaussian_F32() { BlurImageOps.gaussian(inputF32, outputF32, -1, radius, storageF32); }
	@Benchmark public void median_F32() { BlurImageOps.median(inputF32, outputF32, radius, radius, growArrayF32); }
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkBlurImageOps.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
