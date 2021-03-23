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

package boofcv.alg.filter.convolve;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import pabeles.concurrency.GrowArray;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class CommonBenchmarkConvolve_SB {
	protected static int width = 800, height = 600;

	protected static Kernel2D_F32 kernel2D_F32;
	protected static Kernel2D_F64 kernel2D_F64;
	protected static Kernel1D_F32 kernelF32;
	protected static Kernel1D_F64 kernelF64;
	protected static Kernel1D_S32 kernelI32;
	protected static Kernel2D_S32 kernel2D_I32;
	protected static GrayF32 input_F32 = new GrayF32(width, height);
	protected static GrayF64 input_F64 = new GrayF64(width, height);
	protected static GrayF32 out_F32 = new GrayF32(width, height);
	protected static GrayF64 out_F64 = new GrayF64(width, height);
	protected static GrayU8 input_U8 = new GrayU8(width, height);
	protected static GrayS16 input_S16 = new GrayS16(width, height);
	protected static GrayS16 input_U16 = new GrayS16(width, height);
	protected static GrayS32 input_S32 = new GrayS32(width, height);
	protected static GrayU8 out_U8 = new GrayU8(width, height);
	protected static GrayS16 out_S16 = new GrayS16(width, height);
	protected static GrayS32 out_S32 = new GrayS32(width, height);

	protected GrowArray<DogArray_I32> work_I32 = new GrowArray<>(DogArray_I32::new);
	protected GrowArray<DogArray_F32> work_F32 = new GrowArray<>(DogArray_F32::new);
	protected GrowArray<DogArray_F64> work_F64 = new GrowArray<>(DogArray_F64::new);

	public void setup( int radius ) {
		Random rand = new Random(234);

		ImageMiscOps.fillUniform(input_U8, rand, 0, 20);
		ImageMiscOps.fillUniform(input_S16, rand, 0, 20);
		ImageMiscOps.fillUniform(input_S32, rand, 0, 20);
		ImageMiscOps.fillUniform(input_F32, rand, 0, 20);
		ImageMiscOps.fillUniform(input_F64, rand, 0, 20);
		System.arraycopy(input_S16.data,0,input_U16.data,0,input_S16.data.length);

		kernelF64 = FactoryKernelGaussian.gaussian(Kernel1D_F64.class, -1, radius);
		kernelF32 = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, -1, radius);
		kernelI32 = FactoryKernelGaussian.gaussian(Kernel1D_S32.class, -1, radius);
		kernel2D_F64 = FactoryKernelGaussian.gaussian(Kernel2D_F64.class, -1, radius);
		kernel2D_F32 = FactoryKernelGaussian.gaussian(Kernel2D_F32.class, -1, radius);
		kernel2D_I32 = FactoryKernelGaussian.gaussian(Kernel2D_S32.class, -1, radius);
	}

	public void setupSkip( int radius, int skip ) {
		setup(radius);
		out_F32.reshape(width/skip, height/skip);
		out_U8.reshape(width/skip, height/skip);
		out_S16.reshape(width/skip, height/skip);
		out_S32.reshape(width/skip, height/skip);
	}
}
