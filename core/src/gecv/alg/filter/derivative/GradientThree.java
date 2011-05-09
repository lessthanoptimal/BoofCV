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

package gecv.alg.filter.derivative;

import gecv.alg.InputSanityCheck;
import gecv.alg.filter.derivative.three.GradientThree_Standard;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;


/**
 * <p>
 * Computes an edge by convolving a 1D kernel, shown below, across the image.  Note this is NOT
 * the same as a prewitt edge detector.   kernel = [-1 0 1]
 * </p>
 * <p/>
 * <p>
 * For example in an integer image:<br>
 * derivX(x,y) = img(x+1,y) - img(x-1,y)<br>
 * derivY(x,y) = img(x,y+1) - img(x,y-1)<br>
 * </p>
 *
 * @author Peter Abeles
 */
public class GradientThree {


	/**
	 * Computes the derivative of an {@link ImageInt8} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 */
	public static void deriv_I8(ImageInt8 orig,
								ImageInt16 derivX,
								ImageInt16 derivY) {
		InputSanityCheck.checkSameShape(orig, false, derivX, true, derivY, true);
		GradientThree_Standard.deriv_I8(orig, derivX, derivY);
	}

	/**
	 * Computes the derivative of an {@link ImageInt8} along the x axis.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 */
	public static void derivX_I8(ImageInt8 orig,
								 ImageInt16 derivX) {
		InputSanityCheck.checkSameShape(orig, false, derivX, true);
		GradientThree_Standard.derivX_I8(orig, derivX);
	}

	/**
	 * Computes the derivative of an {@link ImageInt8} along the y axis.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 */
	public static void derivY_I8(ImageInt8 orig,
								 ImageInt16 derivY) {
		InputSanityCheck.checkSameShape(orig, false, derivY, true);
		GradientThree_Standard.derivY_I8(orig, derivY);
	}

	/**
	 * Computes the derivative of an {@link ImageFloat32} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 */
	public static void deriv_F32(ImageFloat32 orig,
								 ImageFloat32 derivX,
								 ImageFloat32 derivY) {
		InputSanityCheck.checkSameShape(orig, derivX, derivY);
		GradientThree_Standard.deriv_F32(orig, derivX, derivY);
	}

	/**
	 * Computes the derivative of an {@link ImageFloat32} along the x axis.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivX Derivative along the x-axis. Modified.
	 */
	public static void derivX_F32(ImageFloat32 orig,
								  ImageFloat32 derivX) {
		InputSanityCheck.checkSameShape(orig, derivX);
		GradientThree_Standard.derivX_F32(orig, derivX);
	}

	/**
	 * Computes the derivative of an {@link ImageFloat32} along the y axis.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivY Derivative along the y-axis. Modified.
	 */
	public static void derivY_F32(ImageFloat32 orig,
								  ImageFloat32 derivY) {
		InputSanityCheck.checkSameShape(orig, derivY);
		GradientThree_Standard.derivY_F32(orig, derivY);
	}
}
