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

package boofcv.factory.filter.kernel;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.convolve.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;

import java.util.Random;

/**
 * <p>
 * Factory used to create standard convolution kernels for floating point and
 * integer images.  The size of a kernel is specified by its radius.  The number of elements in a kernel
 * (or its width) is equal to 2*radius+1.
 * </p>
 * <p/>
 * <p>
 * Types of kernels include; Gaussian.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach", "unchecked"})
public class FactoryKernel {

	public static <T extends KernelBase> T createKernelForImage( int width , int offset, int DOF , Class imageType ) {
		boolean isFloat = GeneralizedImageOps.isFloatingPoint(imageType);
		int numBits = Math.max(32, GeneralizedImageOps.getNumBits(imageType));

		return createKernel(width,offset,DOF,isFloat,numBits);
	}

	public static <T extends KernelBase> T createKernel(int width , int offset, int DOF, boolean isFloat, int numBits ) {
		if( DOF == 1 ) {
			if( isFloat ) {
				if( numBits == 32 )
					return (T)new Kernel1D_F32(width,offset);
				else if( numBits == 64 )
					return (T)new Kernel1D_F64(width,offset);
			} else {
				if( numBits == 32 )
					return (T)new Kernel1D_I32(width,offset);
			}
		} else if( DOF == 2 ) {
			if( isFloat ) {
				if( numBits == 32 )
					return (T)new Kernel2D_F32(width,offset);
				else if( numBits == 64 )
					return (T)new Kernel2D_F64(width,offset);
			} else {
				if( numBits == 32 )
					return (T)new Kernel2D_I32(width,offset);
			}
		}
		throw new IllegalArgumentException("Unsupported specifications. DOF = "+DOF+" float = "+isFloat+" bits = "+numBits);
	}

	public static Kernel1D createKernel1D( int offset , int data[] , Class kernelType ) {
		Kernel1D out;

		if( kernelType == Kernel1D_F32.class ) {
			out = new Kernel1D_F32(data.length,offset);
		} else if( kernelType == Kernel1D_F64.class ) {
			out = new Kernel1D_F64(data.length,offset);
		} else if( kernelType == Kernel1D_I32.class ) {
			out = new Kernel1D_I32(data.length,offset);
		} else {
			throw new RuntimeException("Unknown kernel type "+kernelType.getSimpleName());
		}

		for (int i = 0; i < data.length; i++) {
			out.setD(i,data[i]);
		}

		return out;
	}

	/**
	 * <p>
	 * Create an integer table convolution kernel.  All the elements are equal to one.
	 * </p>
	 *
	 * <p>
	 * See {@link boofcv.alg.filter.convolve.ConvolveImageBox} for faster ways to convolve these kernels.
	 * </p>
	 *
	 * @param radius kernel's radius.
	 * @return table kernel.
	 */
	public static Kernel1D_I32 table1D_I32(int radius) {
		Kernel1D_I32 ret = new Kernel1D_I32(radius * 2 + 1);

		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = 1;
		}

		return ret;
	}

	/**
	 * <p>
	 * Create an floating point table convolution kernel.  If un-normalized then all
	 * the elements are equal to one, otherwise they are equal to one over the width.
	 * </p>
	 *
	 * <p>
	 * See {@link boofcv.alg.filter.convolve.ConvolveImageBox} or {@link boofcv.alg.filter.convolve.ConvolveImageMean} for faster ways to convolve these kernels.
	 * </p>
	 *
	 * @param radius kernel's radius.
	 * @return table kernel.
	 */
	public static Kernel1D_F32 table1D_F32(int radius, boolean normalized) {
		Kernel1D_F32 ret = new Kernel1D_F32(radius * 2 + 1);

		float val = normalized ? 1.0f / ret.width : 1.0f;

		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = val;
		}

		return ret;
	}

	public static Kernel1D_F64 table1D_F64(int radius, boolean normalized) {
		Kernel1D_F64 ret = new Kernel1D_F64(radius * 2 + 1);

		double val = normalized ? 1.0 / ret.width : 1.0;

		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = val;
		}

		return ret;
	}

	/**
	 * Creates a random kernel of the specified type where each element is drawn from an uniform
	 * distribution.
	 *
	 * @param type Class of the kernel which is to be created.
	 * @param radius The kernel's radius.
	 * @param min Min value.
	 * @param max Max value.
	 * @param rand Random number generator.
	 * @return The generated kernel.
	 */
	public static <T extends KernelBase> T random( Class<?> type , int radius , int min , int max , Random rand )
	{
		int width = radius*2+1;

		return random(type,width,radius,min,max,rand);
	}

	public static <T extends KernelBase> T random( Class<?> type , int width , int offset , int min , int max , Random rand )
	{
		if (Kernel1D_F32.class == type) {
			return (T) FactoryKernel.random1D_F32(width, offset, min, max, rand);
		} else if (Kernel1D_F64.class == type) {
			return (T) FactoryKernel.random1D_F64(width, offset, min, max, rand);
		} else if (Kernel1D_I32.class == type) {
			return (T) FactoryKernel.random1D_I32(width, offset, min, max, rand);
		} else if (Kernel2D_I32.class == type) {
			return (T) FactoryKernel.random2D_I32(width, offset, min, max, rand);
		} else if (Kernel2D_F32.class == type) {
			return (T) FactoryKernel.random2D_F32(width, offset, min, max, rand);
		} else if (Kernel2D_F64.class == type) {
			return (T) FactoryKernel.random2D_F64(width, offset, min, max, rand);
		} else {
			throw new RuntimeException("Unknown kernel type. "+type.getSimpleName());
		}
	}

	/**
	 * Creates a random 1D kernel drawn from a uniform distribution.
	 *
	 * @param width Kernel's width.
	 * @param offset Offset for element zero in the kernel
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel1D_I32 random1D_I32(int width , int offset, int min, int max, Random rand) {
		Kernel1D_I32 ret = new Kernel1D_I32(width,offset);

		int range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextInt(range) + min;
		}

		return ret;
	}

	/**
	 * Creates a random 1D kernel drawn from a uniform distribution.
	 *
	 * @param width Kernel's width.
	 * @param offset Offset for element zero in the kernel
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel1D_F32 random1D_F32(int width , int offset, float min, float max, Random rand) {
		Kernel1D_F32 ret = new Kernel1D_F32(width,offset);

		float range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextFloat() * range + min;
		}

		return ret;
	}


	public static Kernel1D_F64 random1D_F64(int width , int offset, double min, double max, Random rand) {
		Kernel1D_F64 ret = new Kernel1D_F64(width,offset);

		double range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextDouble() * range + min;
		}

		return ret;
	}

	/**
	 * Creates a random 2D kernel drawn from a uniform distribution.
	 *
	 * @param width Kernel's width.
	 * @param offset Offset for element zero in the kernel
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel2D_I32 random2D_I32(int width , int offset, int min, int max, Random rand) {
		Kernel2D_I32 ret = new Kernel2D_I32(width,offset);

		int range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextInt(range) + min;
		}

		return ret;
	}

	/**
	 * Creates a random 2D kernel drawn from a uniform distribution.
	 *
	 * @param width Kernel's width.
	 * @param offset Offset for element zero in the kernel
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel2D_F32 random2D_F32(int width , int offset, float min, float max, Random rand) {
		Kernel2D_F32 ret = new Kernel2D_F32(width,offset);

		float range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextFloat() * range + min;
		}

		return ret;
	}

	/**
	 * Creates a random 2D kernel drawn from a uniform distribution.
	 *
	 * @param width Kernel's width.
	 * @param offset Offset for element zero in the kernel
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel2D_F64 random2D_F64(int width , int offset, double min, double max, Random rand) {
		Kernel2D_F64 ret = new Kernel2D_F64(width,offset);

		double range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextDouble() * range + min;
		}

		return ret;
	}

	public static <K1 extends Kernel1D , K2 extends Kernel2D>
	Class<K1> get1DType( Class<K2> kernelType ) {
		if( kernelType == Kernel2D_F32.class )
			return (Class<K1>)Kernel1D_F32.class;
		else
			return (Class<K1>)Kernel1D_I32.class;
	}

	public static <K extends KernelBase, T extends ImageGray>
	Class<K> getKernelType( Class<T> imageType , int DOF ) {
		if( imageType == GrayF32.class ) {
			if( DOF == 1 )
				return (Class)Kernel1D_F32.class;
			else
				return (Class)Kernel2D_F32.class;
		} else if( GrayI.class.isAssignableFrom(imageType) ) {
			if( DOF == 1 )
				return (Class)Kernel1D_I32.class;
			else
				return (Class)Kernel2D_I32.class;
		}
		throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
	}
}
