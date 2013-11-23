/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.convolve.*;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;

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
		if (Kernel1D_F32.class == type) {
			return (T) FactoryKernel.random1D_F32(radius, min, max, rand);
		} else if (Kernel1D_I32.class == type) {
			return (T) FactoryKernel.random1D_I32(radius, min, max, rand);
		} else if (Kernel2D_I32.class == type) {
			return (T) FactoryKernel.random2D_I32(radius, min, max, rand);
		} else if (Kernel2D_F32.class == type) {
			return (T) FactoryKernel.random2D_F32(radius, min, max, rand);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}
	}

	/**
	 * Creates a random 1D kernel drawn from a uniform distribution.
	 *
	 * @param radius Kernel's radius.
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel1D_I32 random1D_I32(int radius, int min, int max, Random rand) {
		Kernel1D_I32 ret = new Kernel1D_I32(radius * 2 + 1);

		int range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextInt(range) + min;
		}

		return ret;
	}

	/**
	 * Creates a random 1D kernel drawn from a uniform distribution.
	 *
	 * @param radius Kernel's radius.
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel1D_F32 random1D_F32(int radius, float min, float max, Random rand) {
		Kernel1D_F32 ret = new Kernel1D_F32(radius * 2 + 1);

		float range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextFloat() * range + min;
		}

		return ret;
	}

	public static Kernel1D_F64 random1D_F64(int radius, double min, double max, Random rand) {
		Kernel1D_F64 ret = new Kernel1D_F64(radius * 2 + 1);

		double range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextDouble() * range + min;
		}

		return ret;
	}

	/**
	 * Creates a random 2D kernel drawn from a uniform distribution.
	 *
	 * @param radius Kernel's radius.
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel2D_I32 random2D_I32(int radius, int min, int max, Random rand) {
		Kernel2D_I32 ret = new Kernel2D_I32(radius * 2 + 1);

		int range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextInt(range) + min;
		}

		return ret;
	}

	/**
	 * Creates a random 2D kernel drawn from a uniform distribution.
	 *
	 * @param radius Kernel's radius.
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel2D_F32 random2D_F32(int radius, float min, float max, Random rand) {
		Kernel2D_F32 ret = new Kernel2D_F32(radius * 2 + 1);

		float range = max - min;
		for (int i = 0; i < ret.data.length; i++) {
			ret.data[i] = rand.nextFloat() * range + min;
		}

		return ret;
	}

	/**
	 * Creates a random 2D kernel drawn from a uniform distribution.
	 *
	 * @param radius Kernel's radius.
	 * @param min	minimum value.
	 * @param max	maximum value.
	 * @param rand   Random number generator.
	 * @return Randomized kernel.
	 */
	public static Kernel2D_F64 random2D_F64(int radius, double min, double max, Random rand) {
		Kernel2D_F64 ret = new Kernel2D_F64(radius * 2 + 1);

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

	public static <K extends KernelBase, T extends ImageSingleBand>
	Class<K> getKernelType( Class<T> imageType , int DOF ) {
		if( imageType == ImageFloat32.class ) {
			if( DOF == 1 )
				return (Class)Kernel1D_F32.class;
			else
				return (Class)Kernel2D_F32.class;
		} else if( ImageInteger.class.isAssignableFrom(imageType) ) {
			if( DOF == 1 )
				return (Class)Kernel1D_I32.class;
			else
				return (Class)Kernel2D_I32.class;
		}
		throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
	}
}
