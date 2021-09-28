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

package boofcv.alg.transform.census;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU16;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
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
public class BenchmarkCensus {
	@Param({"true","false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	private final GrayU8 input = new GrayU8(size, size);
	private final GrayU8 output8 = new GrayU8(size, size);
	private final GrayS32 output32 = new GrayS32(size, size);
	private final GrayS64 output64 = new GrayS64(size, size);
	private final InterleavedU16 outputI16 = new InterleavedU16(size, size,1);

	private final ImageBorder_S32<GrayU8> border = (ImageBorder_S32)FactoryImageBorder.wrap(BorderType.ZERO,input);

	private final DogArray<Point2D_I32> points5x5 = CensusTransform.createBlockSamples(2);
	private final DogArray<Point2D_I32> points7x7 = CensusTransform.createBlockSamples(3);
	private final DogArray<Point2D_I32> points9x9 = CensusTransform.createBlockSamples(4);
	private final DogArray_I32 workSpace = new DogArray_I32();

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		input.reshape(size, size);
		output8.reshape(size, size);
		output32.reshape(size, size);

		ImageMiscOps.fillUniform(input, rand, 0, 1);
	}

	@Benchmark
	public void region3x3() {
		CensusTransform.dense3x3(input,output8, border);
	}

	@Benchmark
	public void region5x5() {
		CensusTransform.dense5x5(input,output32, border);
	}

	@Benchmark
	public void samples5x5_S64() {
		CensusTransform.sample_S64(input,points5x5,output64, border,workSpace);
	}

	@Benchmark
	public void samples7x7_S64() {
		CensusTransform.sample_S64(input,points7x7,output64, border,workSpace);
	}

	@Benchmark
	public void samples5x5_IU16() {
		CensusTransform.sample_IU16(input,points5x5,outputI16, border,workSpace);
	}

	@Benchmark
	public void samples9x9_IU16() {
		CensusTransform.sample_IU16(input,points9x9,outputI16, border,workSpace);
	}
}
