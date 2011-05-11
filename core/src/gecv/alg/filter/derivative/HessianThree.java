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
import gecv.alg.filter.derivative.impl.HessianThree_Standard;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * <p>
 * Computes the second derivative (Hessian) of an image using.  This hessian is derived by using the same gradient
 * function used in {@link GradientThree}, which uses a kernel of [-1 0 1].
 * </p>
 *
 * <p>
 * 
 * Kernel for &part; <sup>2</sup>f/&part; x<sup>2</sup> and &part;<sup>2</sup>f /&part; y<sup>2</sup> is
 * [1 0 -2 0 1] and &part;<sup>2</sup>f/&part; x&part;y is:<br>
 * <table border="1">
 * <tr> <td> 1 </td> <td> 0 </td> <td> -1 </td> </tr>
 * <tr> <td> 0 </td> <td> 0 </td> <td> 0 </td> </tr>
 * <tr> <td> -1 </td> <td> 0 </td> <td> 1 </td> </tr>
 * </table}
 * </p>
 *
 * @author Peter Abeles
 */
public class HessianThree {

	/**
	 * <p>
	 * Computes the second derivative of an {@link gecv.struct.image.ImageUInt8} along the x and y axes.
	 * </p>
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivXX Second derivative along the x-axis. Modified.
	 * @param derivYY Second derivative along the y-axis. Modified.
	 */
	public static void process( ImageUInt8 orig, ImageSInt16 derivXX, ImageSInt16 derivYY, ImageSInt16 derivXY) {
		InputSanityCheck.checkSameShape(orig, derivXX, derivYY, derivXY);
		HessianThree_Standard.deriv_I8(orig, derivXX, derivYY,derivXY);
	}

	/**
	 * Computes the second derivative of an {@link gecv.struct.image.ImageUInt8} along the x and y axes.
	 *
	 * @param orig   Which which is to be differentiated. Not Modified.
	 * @param derivXX Second derivative along the x-axis. Modified.
	 * @param derivYY Second derivative along the y-axis. Modified.
	 */
	public static void process( ImageFloat32 orig, ImageFloat32 derivXX, ImageFloat32 derivYY, ImageFloat32 derivXY) {
		InputSanityCheck.checkSameShape(orig, derivXX, derivYY, derivXY);
		HessianThree_Standard.deriv_F32(orig, derivXX, derivYY, derivXY);
	}
}
