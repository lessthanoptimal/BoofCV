/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.concurrency.IWorkArrays;
import boofcv.struct.image.GrayU8;
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
	public int width;

	GrayU8 input = new GrayU8(width,width);
	GrayU8 output = new GrayU8(width,width);
	GrayU8 storage = new GrayU8(width,width);
	IWorkArrays work = new IWorkArrays();

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		input.reshape(width,width);
		output.reshape(width,width);
		storage.reshape(width,width);

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


	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkBlurImageOps.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
