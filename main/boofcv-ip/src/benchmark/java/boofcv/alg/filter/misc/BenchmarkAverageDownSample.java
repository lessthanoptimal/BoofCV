/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=1)
public class BenchmarkAverageDownSample {
	@Param({"true","false"})
	public boolean concurrent;

	@Param({"1000"})
	public int size;

	@Param({"U8", "U16", "F32", "F64"})
	public String imageType;

	ImageGray input;
	ImageGray out2;
	ImageGray out3;
	ImageGray out8;
	ImageGray middle;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		ImageDataType dataType = ImageDataType.valueOf(imageType);

		input = GeneralizedImageOps.createSingleBand(dataType, size, size);
		GImageMiscOps.fillUniform(input, new Random(234), 0, 200);

		out2 = GeneralizedImageOps.createSingleBand(dataType, size/2, size/2);
		int size3 = AverageDownSampleOps.downSampleSize(size, 3);
		out3 = GeneralizedImageOps.createSingleBand(dataType, size3, size3);
		int size8 = AverageDownSampleOps.downSampleSize(size, 8);
		out8 = GeneralizedImageOps.createSingleBand(dataType, size8, size8);
		middle = dataType == ImageDataType.F64 ? new GrayF64(size/2, size) : new GrayF32(size/2, size);
	}

	@Benchmark public void special2x2() {
		AverageDownSampleOps.down(input, 2, out2);
	}

	@Benchmark public void general_3() {
		AverageDownSampleOps.down(input, 3, out3);
	}

	@Benchmark public void general_8() {
		AverageDownSampleOps.down(input, 8, out8);
	}

	@Benchmark public void general_HV() {
		AverageDownSampleOps.down(input, false, out2, middle);
	}
}
