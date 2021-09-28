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

package boofcv.core.encoding;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.*;
import org.openjdk.jmh.annotations.*;

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
@Fork(value=1)
public class BenchmarkConvertYV12 {

	@Param({"true","false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	byte[] yv12;

	GrayU8 grayU8 = new GrayU8(1, 1);
	GrayF32 grayF32 = new GrayF32(1, 1);
	Planar<GrayU8> planarU8 = new Planar<GrayU8>(GrayU8.class, 1, 1, 3);
	Planar<GrayF32> planarF32 = new Planar<GrayF32>(GrayF32.class, 1, 1, 3);
	InterleavedU8 interU8 = new InterleavedU8(1, 1, 3);
	InterleavedF32 interF32 = new InterleavedF32(1, 1, 3);

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		yv12 = new byte[size*size * 2];
		rand.nextBytes(yv12);

		grayU8.reshape(size,size);
		grayF32.reshape(size,size);
		interU8.reshape(size,size);
		interF32.reshape(size,size);
		planarU8.reshape(size,size);
		planarF32.reshape(size,size);

		// convert is faster than more random numbers
		GImageMiscOps.fillUniform(grayU8,rand,0,200);
		ConvertImage.convert(grayU8,grayF32);
		GImageMiscOps.fillUniform(interU8,rand,0,200);
		ConvertImage.convert(interU8,interF32);
		GImageMiscOps.fillUniform(planarU8,rand,0,200);
		GConvertImage.convert(planarU8,planarF32);
	}

	@Benchmark
	public void nv21ToGray_U8() {
		ConvertYV12.yu12ToBoof(yv12, size, size, grayU8);
	}

	@Benchmark
	public void nv21ToGray_F32() {
		ConvertYV12.yu12ToBoof(yv12, size, size, grayF32);
	}

	@Benchmark
	public void nv21TPlanarRgb_U8() {
		ConvertYV12.yu12ToBoof(yv12, size, size, planarU8);
	}

	@Benchmark
	public void nv21ToPlanarRgb_F32() {
		ConvertYV12.yu12ToBoof(yv12, size, size, planarF32);
	}

	@Benchmark
	public void nv21ToInterleaved_U8() {
		ConvertYV12.yu12ToBoof(yv12, size, size, interU8);
	}

	@Benchmark
	public void nv21ToInterleaved_F32() {
		ConvertYV12.yu12ToBoof(yv12, size, size, interF32);
	}
}
