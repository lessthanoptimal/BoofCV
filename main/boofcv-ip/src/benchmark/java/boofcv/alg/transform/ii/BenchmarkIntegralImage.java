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

package boofcv.alg.transform.ii;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
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
public class BenchmarkIntegralImage {
	static int size = 800;

	static GrayF32 input = new GrayF32(size, size);
	static GrayF32 integral = new GrayF32(size, size);
	static GrayF32 output = new GrayF32(size, size);

	IntegralKernel kernelXX = DerivativeIntegralImage.kernelDerivXX(9, null);

	@Setup public void setup() {
		Random rand = new Random(234);
		ImageMiscOps.fillUniform(input, rand, 0, 100);
		IntegralImageOps.transform(input, integral);
	}

	@Benchmark public void ComputeIntegral() {IntegralImageOps.transform(input, integral);}

	@Benchmark public void DerivXX() {
		DerivativeIntegralImage.derivXX(integral, output, 9);
		IntegralImageOps.convolveBorder(integral, kernelXX, output, 4, 4);
	}

	@Benchmark public void GenericDerivXX() {IntegralImageOps.convolve(integral, kernelXX, output);}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkIntegralImage.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
