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
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkPixelMath {

	public static final int radius = 5;

	@Param({"true", "false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
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

		GImageMiscOps.fillUniform(input, rand, 0, 200);
		GImageMiscOps.fillUniform(input2, rand, 0, 200);
	}

	// @formatter:off
	@Benchmark public void abs() {GPixelMath.abs(input, output);}
	@Benchmark public void abs_operator() {PixelMath.operator1(input, a -> (byte)Math.abs(a), output);}
	@Benchmark public void add() {GPixelMath.add(input, input2, output);}
	@Benchmark public void add_operator() {GPixelMath.operator2(input, (PixelMathLambdas.Function2_F32)Float::sum, input2, output);}
	@Benchmark public void negative() {GPixelMath.negative(input, output);}
	@Benchmark public void subtract() {GPixelMath.divide(input, input2, output);}
	@Benchmark public void multiply() {GPixelMath.divide(input, input2, output);}
	@Benchmark public void divide() {GPixelMath.divide(input, input2, output);}
	@Benchmark public void sqrt() {GPixelMath.sqrt(input, output);}
	@Benchmark public void log() {GPixelMath.log(input, 1.0f, output);}
	@Benchmark public void logSign() {GPixelMath.logSign(input, 1.0f, output);}
	@Benchmark public void pow2() {GPixelMath.pow2(input, output);}
	@Benchmark public void stdev() {GPixelMath.stdev(input, input2, output);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkPixelMath.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
