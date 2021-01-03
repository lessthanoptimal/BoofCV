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

package boofcv.alg.misc;

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
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
public class BenchmarkPixelMath {

	public static final int radius = 5;

	@Param({"true","false"})
	public boolean concurrent;

//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"5000"})
	public int size;

	GrayF32 input = new GrayF32(size, size);
	GrayF32 input2 = new GrayF32(size, size);
	GrayF32 output = new GrayF32(size, size);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		input.reshape(size, size);
		input2.reshape(size, size);
		output.reshape(size, size);

		GImageMiscOps.fillUniform(input,rand,0,200);
		GImageMiscOps.fillUniform(input2,rand,0,200);
	}

	@Benchmark
	public void abs() {
		GPixelMath.abs(input,output);
	}

	@Benchmark
	public void abs_operator() {
		PixelMath.operator1(input, a -> (byte)Math.abs(a),output);
	}

	@Benchmark
	public void add() {
		GPixelMath.add(input,input2,output);
	}

	@Benchmark
	public void add_operator() {
		GPixelMath.operator2(input, (PixelMathLambdas.Function2_F32)Float::sum,input2,output);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkPixelMath.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
