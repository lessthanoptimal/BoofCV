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
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import pabeles.concurrency.GrowArray;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class CommonBenchmarkConvolve_IL {
	protected static int width = 800, height = 600, numBands= 2;

	protected static Kernel2D_F32 kernel2D_F32;
	protected static Kernel1D_F32 kernelF32;
	protected static Kernel1D_S32 kernelI32;
	protected static Kernel2D_S32 kernel2D_I32;
	protected static InterleavedF32 input_F32 = new InterleavedF32(width, height, numBands);
	protected static InterleavedF64 input_F64 = new InterleavedF64(width, height, numBands);
	protected static InterleavedF32 out_F32 = new InterleavedF32(width, height, numBands);
	protected static InterleavedF64 out_F64 = new InterleavedF64(width, height, numBands);
	protected static InterleavedU8 input_U8 = new InterleavedU8(width, height, numBands);
	protected static InterleavedS16 input_S16 = new InterleavedS16(width, height, numBands);
	protected static InterleavedS32 input_S32 = new InterleavedS32(width, height, numBands);
	protected static InterleavedU16 input_U16 = new InterleavedU16(width, height, numBands);
	protected static InterleavedU8 out_U8 = new InterleavedU8(width, height, numBands);
	protected static InterleavedS16 out_S16 = new InterleavedS16(width, height, numBands);
	protected static InterleavedS32 out_S32 = new InterleavedS32(width, height, numBands);

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

		kernelF32 = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, -1, radius);
		kernelI32 = FactoryKernelGaussian.gaussian(Kernel1D_S32.class, -1, radius);
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
