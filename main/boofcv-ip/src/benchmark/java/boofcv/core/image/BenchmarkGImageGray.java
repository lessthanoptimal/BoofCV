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

package boofcv.core.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime,Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 2)
public class BenchmarkGImageGray {
	static int width = 640;
	static int height = 480;

	GrayF32 input = new GrayF32(width,height);
	GrayF32 output = new GrayF32(width,height);

	@Setup
	public void setup() {
		Random rand = new Random(234);

		GImageMiscOps.fillUniform(input, rand,-1, 1);
	}

	@Benchmark public void IndexDirect_U8() {
		int index = 0;
		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ , index++) {
				output.data[index] = (byte)input.data[index];
			}
		}
	}

	@Benchmark public void IndexDirect_F32() {
		int index = 0;
		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ , index++) {
				output.data[index] = input.data[index];
			}
		}
	}

	@Benchmark public void IndexAccess() {
		GImageGray output = FactoryGImageGray.wrap(this.output);
		int index = 0;
		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ , index++) {
				output.set(index,input.data[index]);
			}
		}
	}

	@Benchmark public void PixelAccess() {
		GImageGray output = FactoryGImageGray.wrap(this.output);

		int index = 0;
		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ , index++) {
				output.set(x,y,input.data[index]);
			}
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkGImageGray.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
