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

package boofcv.alg.filter.binary;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

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
@Fork(value=1)
public class BenchmarkBinaryBlobLabeling {

	@Param({"true","false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	private final GrayU8 original = new GrayU8(size, size);
	private final GrayU8 input = new GrayU8(size, size);
	private final GrayS32 output = new GrayS32(size, size);

	LinearContourLabelChang2004 chang4 = new LinearContourLabelChang2004(ConnectRule.FOUR);
	LinearContourLabelChang2004 chang8 = new LinearContourLabelChang2004(ConnectRule.EIGHT);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		original.reshape(size, size);
		input.reshape(size, size);
		output.reshape(size, size);

		ImageMiscOps.fillUniform(original, rand, 0, 2);

		for( int y = 0; y < original.height; y++ ) {
			for( int x = 0; x < original.width; x++ ) {
				if( x == 0 || y == 0 || x == original.width-1 || y == original.height-1 )
					original.unsafe_set(x,y,0);
			}
		}
	}

	@Benchmark public void Chang2004_4() { input.setTo(original); chang4.process(input, output); }
	@Benchmark public void Chang2004_8() { input.setTo(original); chang8.process(input, output); }

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkBinaryBlobLabeling.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
