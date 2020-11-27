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

package boofcv.alg.filter.convolve;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.filter.convolve.noborder.ImplConvolveMean;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import pabeles.concurrency.GrowArray;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"UnusedDeclaration"})
public class BenchmarkConvolveMean {
	static private final int width = 640;
	static private final int height = 480;
	static private final Random rand = new Random(234);

	static private final GrayF32 input_F32 = new GrayF32(width,height);
	static private final GrayF32 out_F32 = new GrayF32(width,height);
	static private final GrayF32 storageF32 = new GrayF32(width,height);
	static private final GrowArray<DogArray_F32> workF32 = new GrowArray<>(DogArray_F32::new);
	static private Kernel1D_S32 kernelI32;
	static private final GrayU8 input_I8 = new GrayU8(width,height);
	static private final GrayS16 input_I16 = new GrayS16(width,height);
	static private final GrayU8 out_I8 = new GrayU8(width,height);
	static private final GrowArray<DogArray_I32> workI32 = new GrowArray<>(DogArray_I32::new);

	static private BlurFilter<GrayF32> filter;

	// iterate through different sized kernel radius
//	@Param({"1", "2", "3", "5","10"})
	private int radius;

	public BenchmarkConvolveMean() {
		ImageMiscOps.fillUniform(input_I8,rand,0,20);
		ImageMiscOps.fillUniform(input_I16,rand,0,20);
		ImageMiscOps.fillUniform(input_F32,rand,0,20);
	}

	protected void setUp() throws Exception {
		filter = FactoryBlurFilter.mean(ImageType.single(GrayF32.class),radius);
		Kernel1D_F32 kernelF32 = FactoryKernel.table1D_F32(radius, true);
		kernelI32 = FactoryKernel.table1D_S32(radius);
	}

	public int timeConvolve_Vertical_U8_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.vertical(kernelI32, input_I8,out_I8,radius*2+1);
		return 0;
	}

	public int timeConvolve_Horizontal_U8_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.horizontal(kernelI32, input_I8, out_I8, radius * 2 + 1);
		return 0;
	}

	public int timeMean_U8_I8_Vertical(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveMean.vertical(input_I8, out_I8, radius, radius*2+1, workI32);
		return 0;
	}

	public int timeMean_F32_F32_Vertical(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveMean.vertical(input_F32,out_F32,radius, radius*2+1, workF32);
		return 0;
	}

	public int timeMean_F32_F32_Horizontal(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveMean.horizontal(input_F32, out_F32, radius, radius*2+1);
		return 0;
	}

	public int timeMean_F32_F32_Blur(int reps) {
		for( int i = 0; i < reps; i++ )
			BlurImageOps.mean(input_F32, out_F32, radius, storageF32, workF32);
		return 0;
	}

	public int timeMean_F32_F32_BlurAbst(int reps) {
		for( int i = 0; i < reps; i++ )
			filter.process(input_F32, out_F32);
		return 0;
	}

	public static void main( String[] args ) {
		System.out.println("=========  Profile Image Size "+ width +" x "+ height +" ==========");

//		Runner.main(BenchmarkConvolveMean.class, args);
	}
}
