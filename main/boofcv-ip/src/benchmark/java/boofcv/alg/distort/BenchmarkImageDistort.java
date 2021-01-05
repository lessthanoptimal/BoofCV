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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.distort.FactoryDistort;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.point.Point2D_F32;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkImageDistort {
	@Param({"true", "false"})
	public boolean concurrent;

	//	@Param({"100", "500", "1000", "5000", "10000"})
	@Param({"5000"})
	public int size;

	GrayF32 inputF32 = new GrayF32(size, size);
	GrayF32 outputF32 = new GrayF32(size, size);

	ImageDistort<GrayF32, GrayF32> nearest_sb;
	ImageDistort<GrayF32, GrayF32> bilinear_sb;
	ImageDistort<GrayF32, GrayF32> bilinear_cache_sb;

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;
		Random rand = new Random(234);

		inputF32.reshape(size, size);
		outputF32.reshape(size, size);

		GImageMiscOps.fillUniform(inputF32, rand, 0, 200);

		Affine2D_F32 affine = new Affine2D_F32(
				0.9f, 0.1f, 0.0f,
				0.05f, 1.1f, 02f);
		PixelTransform<Point2D_F32> tran = new PixelTransformAffine_F32(affine);


		nearest_sb = FactoryDistort.distort(false, InterpolationType.NEAREST_NEIGHBOR, BorderType.EXTENDED,
				ImageType.single(GrayF32.class), ImageType.single(GrayF32.class));
		bilinear_sb = FactoryDistort.distort(false, InterpolationType.BILINEAR, BorderType.EXTENDED,
				ImageType.single(GrayF32.class), ImageType.single(GrayF32.class));
		bilinear_cache_sb = FactoryDistort.distort(true, InterpolationType.BILINEAR, BorderType.EXTENDED,
				ImageType.single(GrayF32.class), ImageType.single(GrayF32.class));

		nearest_sb.setModel(tran);
		bilinear_sb.setModel(tran);
		bilinear_cache_sb.setModel(tran);
	}

	@Benchmark
	public void nn_F32() {
		nearest_sb.apply(inputF32, outputF32, 0, 0, size, size);
	}

	@Benchmark
	public void bilinear_F32() {
		bilinear_sb.apply(inputF32, outputF32, 0, 0, size, size);
	}

	@Benchmark
	public void bilinear_cache_F32() {
		bilinear_cache_sb.apply(inputF32, outputF32, 0, 0, size, size);
	}
}
