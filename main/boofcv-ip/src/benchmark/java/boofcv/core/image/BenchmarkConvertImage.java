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

package boofcv.core.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.impl.ImplConvertImage;
import boofcv.core.image.impl.ImplConvertImage_MT;
import boofcv.struct.image.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks related to functions inside of ConvertImage
 * 
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=2)
public class BenchmarkConvertImage {
	@Param({"true","false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	private final int numBands = 3;

	GrayU8 grayU8 = new GrayU8(size, size);
	GrayF32 grayF32 = new GrayF32(size, size);
	InterleavedU8 interU8 = new InterleavedU8(size, size,numBands);
	InterleavedF32 interF32 = new InterleavedF32(size, size,numBands);
	Planar<GrayU8> planarU8 = new Planar<>(GrayU8.class,size,size,numBands);
	Planar<GrayF32> planarF32 = new Planar<>(GrayF32.class,size,size,numBands);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		grayU8.reshape(size,size);
		grayF32.reshape(size,size);
		interU8.reshape(size,size);
		interF32.reshape(size,size);
		planarU8.reshape(size,size);
		planarF32.reshape(size,size);

//		grayU8 = BoofTesting.createSubImageOf(grayU8);

		// convert is faster than more random numbers
		GImageMiscOps.fillUniform(grayU8,rand,0,200);
		ConvertImage.convert(grayU8,grayF32);
		GImageMiscOps.fillUniform(interU8,rand,0,200);
		ConvertImage.convert(interU8,interF32);
		GImageMiscOps.fillUniform(planarU8,rand,0,200);
		GConvertImage.convert(planarU8,planarF32);
	}

	@Benchmark
	public void GRU8_to_GRF32() {
		// manually turn on and off threading to show it doesn't do much
		if( concurrent ) {
			ImplConvertImage_MT.convert(grayU8, grayF32);
		} else {
			ImplConvertImage.convert(grayU8, grayF32);
		}
	}

	@Benchmark
	public void GRF32_to_GRU8() {
		if( concurrent ) {
			ImplConvertImage_MT.convert(grayF32, grayU8);
		} else {
			ImplConvertImage.convert(grayF32, grayU8);
		}
	}

	@Benchmark
	public void ITU8_to_ITF32() {
		if( concurrent ) {
			ImplConvertImage_MT.convert(interU8, interF32);
		} else {
			ImplConvertImage.convert(interU8, interF32);
		}
	}

	@Benchmark
	public void ITU8_to_PL32() {
		ConvertImage.convert(interU8,planarU8);
	}

	@Benchmark
	public void PL32_to_ITU8() {
		ConvertImage.convert(interU8,planarU8);
	}

	@Benchmark
	public void ITU8_to_GRU8() {
		ConvertImage.average(interU8,grayU8);
	}

	@Benchmark
	public void PLU8_to_GRU8() {
		ConvertImage.average(planarU8,grayU8);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvertImage.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
