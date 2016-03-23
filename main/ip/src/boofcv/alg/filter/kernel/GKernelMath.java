/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.kernel;

import boofcv.struct.convolve.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;


/**
 * Contains generalized function with weak typing from {@link KernelMath}.
 *
 * @author Peter Abeles
 */
public class GKernelMath {
	
	public static Kernel2D transpose( Kernel2D a ) {
		if( a instanceof Kernel2D_F32)
			return KernelMath.transpose((Kernel2D_F32)a);
		else
			return KernelMath.transpose((Kernel2D_I32)a);
	}

	public static Kernel1D convolve1D( Kernel1D a , Kernel1D b ) {
		if( a.isInteger() != b.isInteger() )
			throw new IllegalArgumentException("But input kernels must be of the same type.");

		if( a.isInteger() ) {
			throw new IllegalArgumentException("Add support");
		} else {
			return KernelMath.convolve1D_F32((Kernel1D_F32)a,(Kernel1D_F32)b);
		}
	}

	public static Kernel2D convolve( Kernel1D a , Kernel1D b ) {
		if( a.isInteger() != b.isInteger() )
			throw new IllegalArgumentException("But input kernels must be of the same type.");

		if( a.isInteger() ) {
			return KernelMath.convolve2D((Kernel1D_I32) a, (Kernel1D_I32) b);
		} else {
			return KernelMath.convolve2D((Kernel1D_F32) a, (Kernel1D_F32) b);
		}
	}

	public static <T extends ImageGray> T convertToImage(Kernel2D kernel ) {
		if( kernel.isInteger() ) {
			return (T)KernelMath.convertToImage((Kernel2D_I32)kernel);
		} else {
			return (T)KernelMath.convertToImage((Kernel2D_F32)kernel);
		}
	}

	public static Kernel2D convertToKernel( ImageGray image ) {
		if( image.getDataType().isInteger() ) {
			return KernelMath.convertToKernel((GrayI)image);
		} else {
			return KernelMath.convertToKernel((GrayF32)image);
		}
	}
}
