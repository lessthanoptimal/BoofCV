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

package boofcv.alg.misc;

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
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
public class BenchmarkImageBandMath {

	public static final int radius = 5;

	@Param({"true","false"})
	public boolean concurrent;

//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	@Param({"10","11"})
	int numBands = 10;

	Planar<GrayU8> input = new Planar<>(GrayU8.class,size, size, numBands);
	GrayU8 output = new GrayU8(size, size);
	GrayU8 tmp = new GrayU8(size, size);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		input.reshape(size, size);
		output.reshape(size, size);
		tmp.reshape(size, size);

		GImageMiscOps.fillUniform(input,rand,0,200);
	}

	@Benchmark
	public void minimum() {
		GImageBandMath.minimum(input,output);
	}

	@Benchmark
	public void maximum() {
		GImageBandMath.maximum(input,output);
	}

	@Benchmark
	public void median() {
		GImageBandMath.median(input,output);
	}

	@Benchmark
	public void average() {
		GImageBandMath.average(input,output);
	}

	@Benchmark
	public void stdDev() {
		GImageBandMath.stdDev(input,output,tmp);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImageBandMath.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
