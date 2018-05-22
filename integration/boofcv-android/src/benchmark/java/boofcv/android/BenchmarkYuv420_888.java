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

package boofcv.android;

import android.media.Image;
import android.media.MockImage_420_888;
import boofcv.alg.color.ColorFormat;
import boofcv.struct.image.*;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=2)
public class BenchmarkYuv420_888 {
	static final int width = 640, height = 480;

	final Image image = new MockImage_420_888(new Random(234),width,height,2,2,0);

	final GrayU8 grayU8 = new GrayU8(width, height);
	final GrayF32 grayF32 = new GrayF32(width, height);
	final Planar<GrayU8> planarU8 = new Planar<>(GrayU8.class, width, height, 3);
	final Planar<GrayF32> planarF32 = new Planar<>(GrayF32.class, width, height, 3);
	final InterleavedU8 interleavedU8 = new InterleavedU8(width, height, 3);
	final InterleavedF32 interleavedF32 = new InterleavedF32(width, height, 3);

	byte work[] = ConvertCameraImage.declareWork(image,null);

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
