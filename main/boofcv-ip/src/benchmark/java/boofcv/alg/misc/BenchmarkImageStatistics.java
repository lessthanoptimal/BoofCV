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
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=1)
public class BenchmarkImageStatistics {

	@Param({"true","false"})
	public boolean concurrent;

//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	GrayU8 imgA_U8 = new GrayU8(size, size);
	GrayU8 imgB_U8 = new GrayU8(size, size);

	GrayF32 imgA_F32 = new GrayF32(size, size);
	GrayF32 imgB_F32 = new GrayF32(size, size);

	int[] histogram = new int[256];

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		imgA_U8.reshape(size, size);
		imgB_U8.reshape(size, size);
		imgA_F32.reshape(size, size);
		imgB_F32.reshape(size, size);

		GImageMiscOps.fillUniform(imgA_U8,rand,0,200);
		GImageMiscOps.fillUniform(imgB_U8,rand,0,200);
		GImageMiscOps.fillUniform(imgA_F32,rand,-100,100);
		GImageMiscOps.fillUniform(imgB_F32,rand,-100,100);
	}

	@Benchmark
	public void maxAbs() {
		GImageStatistics.maxAbs(imgA_U8);
	}

	@Benchmark
	public void histogram() {
		GImageStatistics.histogram(imgA_U8,0,histogram);
	}

	@Benchmark
	public void max() {
		GImageStatistics.max(imgA_U8);
	}

	@Benchmark
	public void min() {
		GImageStatistics.min(imgA_U8);
	}

	@Benchmark
	public void mean() {
		GImageStatistics.mean(imgA_U8);
	}

	@Benchmark
	public void meanDiffAbs() {
		GImageStatistics.meanDiffAbs(imgA_F32,imgB_F32);
	}

	@Benchmark
	public void meanDiffSq() {
		GImageStatistics.meanDiffSq(imgA_U8,imgB_U8);
	}

	@Benchmark
	public void sum() {
		GImageStatistics.sum(imgA_U8);
	}

	@Benchmark
	public void variance() {
		GImageStatistics.variance(imgA_U8,120);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImageStatistics.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
