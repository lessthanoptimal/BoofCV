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
import boofcv.struct.image.InterleavedF32;
import boofcv.struct.image.InterleavedU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkImageMiscOps {

	@Param({"true", "false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"1000"})
	public int size;

	GrayU8 imgA_U8 = new GrayU8(size, size);
	GrayU8 imgB_U8 = new GrayU8(size, size);

	GrayF32 imgA_F32 = new GrayF32(size, size);
	GrayF32 imgB_F32 = new GrayF32(size, size);
	InterleavedU8 imgA_IU8 = new InterleavedU8(size, size, 3);
	InterleavedU8 imgB_IU8 = new InterleavedU8(size, size, 3);
	InterleavedF32 imgA_IF32 = new InterleavedF32(size, size, 3);
	InterleavedF32 imgB_IF32 = new InterleavedF32(size, size, 3);
	Random rand;

	@Setup public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		rand = new Random(234);

		imgA_U8.reshape(size, size);
		imgB_U8.reshape(size, size);
		imgA_F32.reshape(size, size);
		imgB_F32.reshape(size, size);
		imgA_IU8.reshape(size, size);
		imgB_IU8.reshape(size, size);
		imgA_IF32.reshape(size, size);
		imgB_IF32.reshape(size, size);

		GImageMiscOps.fillUniform(imgA_U8, rand, 0, 200);
		GImageMiscOps.fillUniform(imgB_U8, rand, 0, 200);
		GImageMiscOps.fillUniform(imgA_F32, rand, -100, 100);
		GImageMiscOps.fillUniform(imgB_F32, rand, -100, 100);
		GImageMiscOps.fillUniform(imgA_IU8, rand, -100, 100);
		GImageMiscOps.fillUniform(imgB_IU8, rand, -100, 100);
		GImageMiscOps.fillUniform(imgA_IF32, rand, -100, 100);
		GImageMiscOps.fillUniform(imgB_IF32, rand, -100, 100);
	}

	// @formatter:off
	@Benchmark public void copy_U8() {ImageMiscOps.copy(10,10,0,0,size-10,size-10,imgA_U8,imgB_U8);}
	@Benchmark public void copy_F32() {ImageMiscOps.copy(10,10,0,0,size-10,size-10,imgA_F32,imgB_F32);}
	@Benchmark public void fill_U8() {ImageMiscOps.fill(imgA_U8, 2);}
	@Benchmark public void fill_F32() {ImageMiscOps.fill(imgA_F32, 2.0f);}
	@Benchmark public void fill_IU8() {ImageMiscOps.fill(imgA_IU8, 2);}
	@Benchmark public void fill_IF32() {ImageMiscOps.fill(imgB_IF32, 2.0f);}
	@Benchmark public void fill_mb_IU8() {ImageMiscOps.fill(imgA_IU8, new int[]{5,8,2});}
	@Benchmark public void fill_mb_IF32() {ImageMiscOps.fill(imgB_IF32, new float[]{5,8,2});}
	@Benchmark public void fillBand_IU8() {ImageMiscOps.fillBand(imgA_IU8,1, 2);}
	@Benchmark public void fillBand_IF32() {ImageMiscOps.fillBand(imgB_IF32, 1, 2.0f);}
	@Benchmark public void fillRectangle_U8() {ImageMiscOps.fillRectangle(imgA_U8,2,10,12,size-10,size-12);}
	@Benchmark public void fillRectangle_IU8() {ImageMiscOps.fillRectangle(imgA_IU8,2,10,12,size-10,size-12);}
	@Benchmark public void fillUniform_U8() {ImageMiscOps.fillUniform(imgA_U8, rand, 0, 200);}
	@Benchmark public void fillUniform_F32() {ImageMiscOps.fillUniform(imgA_F32, rand, 0, 200);}
	@Benchmark public void fillUniform_IU8() {ImageMiscOps.fillUniform(imgA_IU8, rand, 0, 200);}
	@Benchmark public void fillUniform_IF32() {ImageMiscOps.fillUniform(imgA_IF32, rand, 0, 200);}
	@Benchmark public void fillGaussian_U8() {ImageMiscOps.fillGaussian(imgA_U8, rand, 0, 5, 0, 200);}
	@Benchmark public void fillGaussian_F32() {ImageMiscOps.fillGaussian(imgA_F32, rand, 0, 5, 0, 200);}
	@Benchmark public void fillGaussian_IU8() {ImageMiscOps.fillGaussian(imgA_IU8, rand, 0, 5, 0, 200);}
	@Benchmark public void fillGaussian_IF32() {ImageMiscOps.fillGaussian(imgA_IF32, rand, 0, 5, 0, 200);}
	@Benchmark public void fillBorder_U8() {ImageMiscOps.fillBorder(imgA_U8,2,10);}
	@Benchmark public void fillBorder_F32() {ImageMiscOps.fillBorder(imgA_F32, 2.0f,10);}
	@Benchmark public void extractBand_IU8() {ImageMiscOps.extractBand(imgA_IU8,2,imgA_U8);}
	@Benchmark public void extractBand_F32() {ImageMiscOps.extractBand(imgA_IF32,2,imgA_F32);}
	@Benchmark public void insertBand_IU8() {ImageMiscOps.insertBand(imgA_U8,2,imgA_IU8);}
	@Benchmark public void insertBand_IF32() {ImageMiscOps.insertBand(imgA_F32,2,imgA_IF32);}
	@Benchmark public void flipHorizontal_U8() {ImageMiscOps.flipHorizontal(imgA_U8);}
	@Benchmark public void flipHorizontal_F32() {ImageMiscOps.flipHorizontal(imgA_F32);}
	@Benchmark public void flipVertical_U8() {ImageMiscOps.flipVertical(imgA_U8);}
	@Benchmark public void flipVertical_F32() {ImageMiscOps.flipVertical(imgA_F32);}
	@Benchmark public void transpose_2_U8() {ImageMiscOps.transpose(imgA_U8,imgB_U8);}
	@Benchmark public void transpose_2_F32() {ImageMiscOps.transpose(imgA_F32,imgB_F32);}
	@Benchmark public void transpose_2_IU8() {ImageMiscOps.transpose(imgA_IU8,imgB_IU8);}
	@Benchmark public void rotateCW_U8() {ImageMiscOps.rotateCW(imgA_U8);}
	@Benchmark public void rotateCW_F32() {ImageMiscOps.rotateCW(imgA_F32);}
	@Benchmark public void rotateCW_2_U8() {ImageMiscOps.rotateCW(imgA_U8,imgB_U8);}
	@Benchmark public void rotateCW_2_F32() {ImageMiscOps.rotateCW(imgA_F32,imgB_F32);}
	@Benchmark public void rotateCCW_U8() {ImageMiscOps.rotateCCW(imgA_U8);}
	@Benchmark public void rotateCCW_F32() {ImageMiscOps.rotateCCW(imgA_F32);}
	@Benchmark public void rotateCCW_2_U8() {ImageMiscOps.rotateCCW(imgA_U8,imgB_U8);}
	@Benchmark public void rotateCCW_2_F32() {ImageMiscOps.rotateCCW(imgA_F32,imgB_F32);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImageMiscOps.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
