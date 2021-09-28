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

package boofcv.alg.filter.misc;

import boofcv.alg.filter.misc.impl.*;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;
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
public class BenchmarkAverageDownSample {
	@Param({"true","false"})
	public boolean concurrent;

	@Param({"1000"})
	public int size;

	GrayU8 inputU8 = new GrayU8(1,1);
	GrayS8 inputS8 = new GrayS8(1,1);
	GrayU8 out8 = new GrayU8(1,1);
	GrayF32 outF32 = new GrayF32(1,1);

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		inputU8.reshape(size, size);
		inputS8.reshape(size, size);
		out8.reshape(size, size);
		outF32.reshape(size/2, size/2);

		ImageMiscOps.fillUniform(inputU8,rand,0,200);
		ImageMiscOps.fillUniform(inputS8,rand,0,200);
	}

	@Benchmark public void general_8_U8() {
		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplAverageDownSampleN_MT.down(inputU8, 8, out8);
		} else {
			ImplAverageDownSampleN.down(inputU8, 8, out8);
		}
	}

	@Benchmark public void general_2_U8() {
		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplAverageDownSampleN_MT.down(inputU8, 2, out8);
		} else {
			ImplAverageDownSampleN.down(inputU8, 2, out8);
		}
	}

	@Benchmark public void general_2_S8() {
		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplAverageDownSampleN_MT.down(inputS8, 2, out8);
		} else {
			ImplAverageDownSampleN.down(inputS8, 2, out8);
		}
	}

	@Benchmark public void special2x2_U8() {
		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplAverageDownSample2_MT.down(inputU8, out8);
		} else {
			ImplAverageDownSample2.down(inputU8, out8);
		}
	}

	@Benchmark public void general_HV_U8() {
		outF32.reshape(size/2, size);
		out8.reshape(size/2, size/2);
		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplAverageDownSample_MT.horizontal(inputU8, outF32);
			ImplAverageDownSample_MT.vertical(outF32, out8);
		} else {
			ImplAverageDownSample.horizontal(inputU8, outF32);
			ImplAverageDownSample.vertical(outF32, out8);
		}
	}
}
