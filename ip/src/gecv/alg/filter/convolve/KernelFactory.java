/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.convolve;

import gecv.core.image.GeneralizedImageOps;
import gecv.struct.convolve.*;
import pja.stats.UtilGaussian;

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
public class KernelFactory {

	/**
	 * <p>
	 * Create an integer table convolution kernel.  All the elements are equal to one.
	 * </p>
	 *
	 * <p>
	 * See {@link ConvolveImageBox} for faster ways to convolve these kernels.
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
	 * See {@link ConvolveImageBox} or {@link ConvolveImageMean} for faster ways to convolve these kernels.
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
			return (T)KernelFactory.random1D_F32(radius, min, max, rand);
		} else if (Kernel1D_I32.class == type) {
			return (T)KernelFactory.random1D_I32(radius, min, max, rand);
		} else if (Kernel2D_I32.class == type) {
			return (T)KernelFactory.random2D_I32(radius, min, max, rand);
		} else if (Kernel2D_F32.class == type) {
			return (T)KernelFactory.random2D_F32(radius, min, max, rand);
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
	 * Creates a Gaussian kernel of the specified type.
	 *
	 * @param type Class of the kernel which is to be created.
	 * @param radius The kernel's radius.
	 * @param normalize If applicable, should the kernel be normalized.
	 * @return The generated kernel.
	 */
	public static <T extends KernelBase> T gaussian( Class<?> type , int radius , boolean normalize)
	{
		if (Kernel1D_F32.class == type) {
			return (T)KernelFactory.gaussian1D_F32(radius,radius, normalize);
		} else if (Kernel1D_I32.class == type) {
			return (T)KernelFactory.gaussian1D_I32(radius, radius );
		} else if (Kernel2D_I32.class == type) {
			return (T)KernelFactory.gaussian2D_I32(radius, radius );
		} else if (Kernel2D_F32.class == type) {
			return (T)KernelFactory.gaussian2D_F32(radius, radius, normalize);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}
	}

	/**
	 * <p>
	 * Creates an integer Gaussian kernel with the specified width.  A default sigma of width/ 5.0 is used.
	 * </p>
	 *
	 * @param radius The kernel's radius.
	 */
	public static Kernel1D gaussian1D( Class<?> imageType , int radius) {
		if( GeneralizedImageOps.isFloatingPoint(imageType) )
			return gaussian1D_F32(radius,true);
		else
			return gaussian1D_I32(radius);
	}

	public static Kernel1D gaussian1D( Class<?> imageType , double sigma , int radius) {
		if( GeneralizedImageOps.isFloatingPoint(imageType) )
			return gaussian1D_F32(sigma,radius,true);
		else
			return gaussian1D_I32(sigma,radius);
	}

	public static Kernel1D gaussianDerivative1D( Class<?> imageType , double sigma , int radius) {
		if( GeneralizedImageOps.isFloatingPoint(imageType) )
			return gaussianDerivative1D_F32(sigma,radius,true);
		else
			return gaussianDerivative1D_I32(sigma,radius);
	}

	/**
	 * <p>
	 * Creates an integer Gaussian kernel with the specified width.  A default sigma of width/ 5.0 is used.
	 * </p>
	 *
	 * @param radius The kernel's radius.
	 */
	public static Kernel1D_I32 gaussian1D_I32(int radius) {
		return gaussian1D_I32(sigmaForRadius(radius), radius);
	}

	/**
	 * Creates an integer Gaussian kernel with the specified sigma and radius.
	 *
	 * @param sigma  The distributions standard deviation.
	 * @param radius The kernel's radius.
	 */
	public static Kernel1D_I32 gaussian1D_I32(double sigma, int radius) {
		double mult = 1.0 / UtilGaussian.computePDF(0, sigma, radius);

		Kernel1D_I32 ret = new Kernel1D_I32(radius * 2 + 1);
		int[] gaussian = ret.data;
		int index = 0;
		for (int i = -radius; i <= radius; i++) {
			gaussian[index++] = (int) (UtilGaussian.computePDF(0, sigma, i) * mult);
		}

		return ret;
	}

	/**
	 * <p>
	 * Creates an integer Gaussian kernel for a 2D convolution.
	 * </p>
	 * <p/>
	 * <p>
	 * Should only be used for validation purposes.  Convolving a 1D kernel along each axis is faster.
	 * </p>
	 *
	 * @param radius The kernel's radius.
	 */
	public static Kernel2D_I32 gaussian2D_I32(double sigma, int radius) {
		Kernel1D_I32 ker1D = gaussian1D_I32(sigma, radius);
		int[] a = ker1D.data;

		Kernel2D_I32 ret = new Kernel2D_I32(radius * 2 + 1);
		int[] data = ret.data;

		int index = 0;
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a.length; j++) {
				data[index++] = a[i] * a[j];
			}
		}

		return ret;
	}

	/**
	 * <p>
	 * Creates a floating point Gaussian kernel with the sigma and radius.
	 * If normalized is set to true then the elements in the kernel will sum up to one.
	 * </p>
	 *
	 * @param sigma	 Distributions standard deviation.
	 * @param radius	Kernel's radius.
	 * @param normalize If the kernel should be normalized to one or not.
	 */
	public static Kernel1D_F32 gaussian1D_F32(double sigma, int radius, boolean normalize) {
		Kernel1D_F32 ret = new Kernel1D_F32(radius * 2 + 1);
		float[] gaussian = ret.data;
		int index = 0;
		for (int i = -radius; i <= radius; i++) {
			gaussian[index++] = (float) UtilGaussian.computePDF(0, sigma, i);
		}

		if (normalize) {
			normalizeSumToOne(ret);
		}

		return ret;
	}

	/**
	 * <p>
	 * Creates a floating point Gaussian kernel with the specified radius.   The sigma is set to width/ 5.0.
	 * If normalized is set to true then the elements in the kernel will sum up to one.
	 * </p>
	 *
	 * @param radius	The kernel's radius.
	 * @param normalize If the sum of the kernel should be equal to one or not.
	 */
	public static Kernel1D_F32 gaussian1D_F32(int radius, boolean normalize) {
		return gaussian1D_F32(sigmaForRadius(radius), radius, normalize);
	}

	/**
	 * Creates a kernel for a 2D convolution.  This should only be used for validation purposes.
	 *
	 * @param sigma	 Distributions standard deviation.
	 * @param radius	Kernel's radius.
	 * @param normalize If the kernel should be normalized to one or not.
	 */
	public static Kernel2D_F32 gaussian2D_F32(double sigma, int radius, boolean normalize) {
		float[] a = gaussian1D_F32(sigma, radius, normalize).data;

		Kernel2D_F32 ret = new Kernel2D_F32(radius * 2 + 1);
		float[] data = ret.data;

		int index = 0;
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a.length; j++) {
				data[index++] = a[i] * a[j];
			}
		}

		return ret;
	}

	/**
	 * <p> Creates a new kernel and autonmatically selects the width of the kernel to have the specified
	 * accuracy. </p>
	 * <p>
	 * radius = sqrt( -2&sigma;<sup>2</sup> *Log( &sigma; * sqrt(2&pi;) *minVal )<br>
	 * <br>
	 * This was found by solving for minVal in the Gaussian equation.
	 * </p>
	 *
	 * @param sigma	 The sigma for the Gaussian
	 * @param maxRadius The largest radius the kernel can have
	 * @param minVal	The approximate minimum value that an element should have.
	 */
	public static Kernel1D_F32 gaussian1D_F32(double sigma, int maxRadius, double minVal, boolean normalize) {
		double tempRadius = Math.sqrt(-2.0 * sigma * sigma * Math.log(sigma * Math.sqrt(2.0 * Math.PI) * minVal));

		if (Double.isNaN(tempRadius) || Double.isInfinite(tempRadius))
			throw new IllegalArgumentException("Bad minVal and/or sigma");

		int radius = (int) Math.ceil(tempRadius);

		int width = radius * 2 + 1;

		if (width > maxRadius * 2 + 1) {
			// use the max width instead
			radius = maxRadius;
			width = maxRadius * 2 + 1;
		}

		Kernel1D_F32 ret = new Kernel1D_F32(width);
		float[] gaussian = ret.data;
		int index = 0;
		for (int i = -radius; i <= radius; i++) {
			gaussian[index++] = (float) UtilGaussian.computePDF(0, sigma, i);
		}

		if (normalize) {
			normalizeSumToOne(ret);
		}

		return ret;
	}

	/**
	 * Computes the derivative of a Gaussian kernel.
	 *
	 * @param sigma Distributions standard deviation.
	 * @param radius Kernel's radius.
	 * @param normalize Scale it by the same factor as the normalized gaussian
	 * @return The derivative of the gaussian
	 */
	public static Kernel1D_F32 gaussianDerivative1D_F32(double sigma, int radius, boolean normalize) {

		Kernel1D_F32 ret = new Kernel1D_F32(radius * 2 + 1);
		float[] gaussian = ret.data;
		int index = 0;
		for (int i = -radius; i <= radius; i++) {
			gaussian[index++] = (float) UtilGaussian.derivativePDF(0, sigma, i);
		}

		if (normalize) {
			// scale it by the same scale factor as its corresponding Gaussian
			float norm = gaussian1D_F32(sigma,radius,false).computeSum();

			for (int i = 0; i < ret.width; i++) {
				gaussian[i] /= norm;
			}
		}

		return ret;
	}

	/**
	 * Computes the derivative of a Gaussian kernel.
	 *
	 * @param sigma Distributions standard deviation.
	 * @param radius Kernel's radius.
	 * @return The derivative of the gaussian
	 */
	public static Kernel1D_I32 gaussianDerivative1D_I32(double sigma, int radius ) 
	{
		Kernel1D_I32 ret = new Kernel1D_I32(radius * 2 + 1);
		int[] gaussian = ret.data;

		double div = Math.abs(UtilGaussian.derivativePDF(0, sigma, -radius));

		int index = 0;
		for (int i = -radius; i <= radius; i++) {
			gaussian[index++] = (int) (UtilGaussian.derivativePDF(0, sigma, i)/div);
		}

		return ret;
	}

	/**
	 * Normalizes the array such that it sums up to one.
	 *
	 * @param kernel The kernel being normalized.
	 */
	public static void normalizeSumToOne(Kernel1D_F32 kernel) {

		float[] data = kernel.data;
		float total = 0;
		for (int i = 0; i < data.length; i++) total += data[i];
		for (int i = 0; i < data.length; i++) data[i] /= total;
	}

	public static double sigmaForRadius( int radius ) {
		return (radius * 2 + 1) / 5.0;
	}

	public static int radiusForSigma( double sigma ) {
		return (int)Math.ceil(((5*sigma)-1)/2);
	}
}
