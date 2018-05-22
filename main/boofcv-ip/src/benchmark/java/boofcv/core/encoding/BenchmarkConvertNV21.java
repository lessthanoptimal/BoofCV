/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
@Fork(value=2)
public class BenchmarkConvertNV21 {

	static final int width = 640, height = 480;

	byte nv21[] = new byte[width * height * 2];

	GrayU8 grayU8 = new GrayU8(width, height);
	GrayF32 grayF32 = new GrayF32(width, height);
	Planar<GrayU8> planarU8 = new Planar<GrayU8>(GrayU8.class, width, height, 3);
	Planar<GrayF32> planarF32 = new Planar<GrayF32>(GrayF32.class, width, height, 3);
	InterleavedU8 interleavedU8 = new InterleavedU8(width, height, 3);
	InterleavedF32 interleavedF32 = new InterleavedF32(width, height, 3);

	public BenchmarkConvertNV21() {
		Random rand = new Random(234);
		for (int i = 0; i < nv21.length; i++) {
			nv21[i] = (byte) rand.nextInt(256);
		}
	}

	@Benchmark
	public void nv21ToGray_U8() {
		ConvertNV21.nv21ToGray(nv21, width, height, grayU8);
	}

	@Benchmark
	public void nv21ToGray_F32() {
		ConvertNV21.nv21ToGray(nv21, width, height, grayF32);
	}

	@Benchmark
	public void nv21TPlanarRgb_U8() {
		ConvertNV21.nv21TPlanarRgb_U8(nv21, width, height, planarU8);
	}

	@Benchmark
	public void nv21ToPlanarRgb_F32() {
		ConvertNV21.nv21ToPlanarRgb_F32(nv21, width, height, planarF32);
	}

	@Benchmark
	public void nv21ToInterleaved_U8() {
		ConvertNV21.nv21ToInterleaved(nv21, width, height, interleavedU8);
	}

	@Benchmark
	public void nv21ToInterleaved_F32() {
		ConvertNV21.nv21ToInterleaved(nv21, width, height, interleavedF32);
	}
}

