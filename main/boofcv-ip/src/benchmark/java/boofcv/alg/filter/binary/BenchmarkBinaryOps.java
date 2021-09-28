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

package boofcv.alg.filter.binary;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkBinaryOps {
	@Param({"true", "false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	private final GrayU8 inputA = new GrayU8(size, size);
	private final GrayU8 inputB = new GrayU8(size, size);
	private final GrayU8 output = new GrayU8(size, size);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		inputA.reshape(size, size);
		inputB.reshape(size, size);
		output.reshape(size, size);

		ImageMiscOps.fillUniform(inputA, rand, 0, 1);
		ImageMiscOps.fillUniform(inputB, rand, 0, 1);
	}

	// @formatter:off
	@Benchmark public void erode4() { BinaryImageOps.erode4(inputA, 1, output); }
	@Benchmark public void erode8() { BinaryImageOps.erode8(inputA, 1, output); }
	@Benchmark public void dilate4() { BinaryImageOps.dilate4(inputA, 1, output); }
	@Benchmark public void dilate8() { BinaryImageOps.dilate8(inputA, 1, output); }
	@Benchmark public void removePointNoise() { BinaryImageOps.removePointNoise(inputA, output); }
	@Benchmark public void edge4() { BinaryImageOps.edge4(inputA, output, true); }
	@Benchmark public void edge8() { BinaryImageOps.edge8(inputA, output, true); }
	@Benchmark public void logicAnd() { BinaryImageOps.logicAnd(inputA, inputB, output); }
	@Benchmark public void logicOr() { BinaryImageOps.logicOr(inputA, inputB, output); }
	@Benchmark public void logicXor() { BinaryImageOps.logicXor(inputA, inputB, output); }
	@Benchmark public void invert() { BinaryImageOps.invert(inputA, output); }
	@Benchmark public void thin() { BinaryImageOps.thin(inputA, 5, output); }
	// @formatter:on
}
