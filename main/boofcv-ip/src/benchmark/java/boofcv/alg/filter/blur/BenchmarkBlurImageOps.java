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

package boofcv.alg.filter.blur;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.concurrency.GrowArray;
import boofcv.concurrency.IWorkArrays;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.GrowQueue_I32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=2)
public class BenchmarkBlurImageOps {

	public static final int radius = 5;

	@Param({"true","false"})
	public boolean concurrent;

	@Param({"100", "500", "1000", "5000", "10000"})
	public int size;

	GrayU8 input = new GrayU8(size, size);
	GrayU8 output = new GrayU8(size, size);
	GrayU8 storage = new GrayU8(size, size);
	IWorkArrays work = new IWorkArrays();
	GrowArray<GrowQueue_I32> growArray = new GrowArray<>(GrowQueue_I32::new);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		input.reshape(size, size);
		output.reshape(size, size);
		storage.reshape(size, size);

		ImageMiscOps.fillUniform(input,rand,0,200);
		ImageMiscOps.fillUniform(output,rand,0,200);
		ImageMiscOps.fillUniform(storage,rand,0,200);
	}

	@Benchmark
	public void mean() {
		BlurImageOps.mean(input,output,radius,storage,work);
	}

	@Benchmark
	public void gaussian() {
		BlurImageOps.gaussian(input,output,-1,radius,storage);
	}

	@Benchmark
	public void median() {
		BlurImageOps.median(input,output,radius,radius,growArray);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkBlurImageOps.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
