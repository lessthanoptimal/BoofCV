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

package boofcv.android;

import android.media.Image;
import android.media.MockImage_420_888;
import boofcv.alg.color.ColorFormat;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_I8;
import org.openjdk.jmh.annotations.*;
import pabeles.concurrency.GrowArray;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=1)
public class BenchmarkYuv420_888 {

//	@Param({"true","false"})
//	public boolean concurrent;

	@Param({"600","5000"})
	public int size;


	Image image;

	final GrayU8 grayU8 = new GrayU8(1, 1);
	final GrayF32 grayF32 = new GrayF32(1, 1);
	final Planar<GrayU8> planarU8 = new Planar<>(GrayU8.class, 1, 1, 3);
	final Planar<GrayF32> planarF32 = new Planar<>(GrayF32.class, 1, 1, 3);
	final InterleavedU8 interleavedU8 = new InterleavedU8(1, 1, 3);
	final InterleavedF32 interleavedF32 = new InterleavedF32(1, 1, 3);

	GrowArray<DogArray_I8> work = new GrowArray<>(DogArray_I8::new);

	@Setup
	public void setup() {
//		BoofConcurrency.USE_CONCURRENT = concurrent;
		image = new MockImage_420_888(new Random(234),size,size,2,2,0);

		grayU8.reshape(size, size);
		grayF32.reshape(size, size);
		planarU8.reshape(size, size, 3);
		planarF32.reshape(size, size, 3);
		interleavedU8.reshape(size, size, 3);
		interleavedF32.reshape(size, size, 3);
	}

	@Benchmark
	public void yuvToGray_U8() {
		ConvertCameraImage.imageToBoof(image, ColorFormat.RGB, grayU8, work);
	}

	@Benchmark
	public void yuvToGray_F32() {
		ConvertCameraImage.imageToBoof(image, ColorFormat.RGB, grayF32, work);
	}

	@Benchmark
	public void yuvToInterleavedRgbU8() {
		ConvertCameraImage.imageToBoof(image, ColorFormat.RGB, interleavedU8, work);
	}

	@Benchmark
	public void yuvToInterleavedRgbF32() {
		ConvertCameraImage.imageToBoof(image, ColorFormat.RGB, interleavedF32, work);
	}

	@Benchmark
	public void yuvToPlanarRgbU8() {
		ConvertCameraImage.imageToBoof(image, ColorFormat.RGB, planarU8, work);
	}

	@Benchmark
	public void yuvToPlanarRgbF32() {
		ConvertCameraImage.imageToBoof(image, ColorFormat.RGB, planarF32, work);
	}
}
