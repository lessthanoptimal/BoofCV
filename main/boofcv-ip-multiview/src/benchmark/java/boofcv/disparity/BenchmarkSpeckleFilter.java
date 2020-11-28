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

package boofcv.disparity;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.segmentation.cc.ConnectedTwoRowSpeckleFiller;
import boofcv.struct.image.GrayF32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkSpeckleFilter {

	@Param({"800"})
	public int size;

	public float tolerance = 1.0f;
	public int maximumArea = 1000;
	public float fillColor = 40.0f;

	GrayF32 inputF32 = new GrayF32(1, 1);
	GrayF32 outputF32 = new GrayF32(1, 1);

	ConnectedTwoRowSpeckleFiller dualRow = new ConnectedTwoRowSpeckleFiller();

	@Setup
	public void setup() {
		Random rand = new Random(234);

		inputF32.reshape(size, size);

		ImageMiscOps.fillUniform(inputF32, rand, 0, fillColor);
		// give it a large flat region similar to what real data would have
		ImageMiscOps.fillRectangle(inputF32, 20.1f, size/10, size/12, size/2, size/3);
		for (int i = 0; i < 2000; i++) {
			inputF32.data[rand.nextInt(inputF32.totalPixels())] = fillColor;
		}
	}

	@Benchmark
	public void dualRow_F32() {
		// copy the image since it's modified
		outputF32.setTo(inputF32);
		dualRow.process(outputF32, maximumArea, tolerance, fillColor);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkSpeckleFilter.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}