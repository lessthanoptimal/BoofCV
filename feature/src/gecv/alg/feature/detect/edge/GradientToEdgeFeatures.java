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

package gecv.alg.feature.detect.edge;

import gecv.alg.InputSanityCheck;
import gecv.alg.feature.detect.edge.impl.ImplEdgeNonMaxSuppression;
import gecv.alg.feature.detect.edge.impl.ImplGradientToEdgeFeatures;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;

/**
 * <p>
 * Give the image's gradient in the x and y direction compute the edge's intensity and orientation.
 * Two ways are provided for computing the edge's intensity: euclidean norm and sum of absolute values (induced 1-norm).
 * The former is the most accurate, while the later is much faster.
 * </p>
 *
 * <p>
 * norm: sqrt( g<sub>x</sub><sup>2</sup> + g<sub>y</sub><sup>2</sup>)<br>
 * abs: |g<sub>x</sub>| + |g<sub>y</sub>|
 * angle: atan( g<sub>y</sub> / g<sub>x</sub> )
 * </p>
 *
 * <p>
 * When computing the angle care is taken to avoid divided by zero errors.
 * </p>
 *
 * @author Peter Abeles
 */
public class GradientToEdgeFeatures {

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param intensity Edge intensity.
	 */
	static public void intensityE( ImageFloat32 derivX , ImageFloat32 derivY , ImageFloat32 intensity )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

		ImplGradientToEdgeFeatures.intensityE(derivX,derivY,intensity);
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param intensity Edge intensity.
	 */
	static public void intensityAbs( ImageFloat32 derivX , ImageFloat32 derivY , ImageFloat32 intensity )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

		ImplGradientToEdgeFeatures.intensityAbs(derivX,derivY,intensity);
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians.
	 */
	static public void direction( ImageFloat32 derivX , ImageFloat32 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction(derivX,derivY,angle);
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param intensity Edge intensity.
	 */
	static public void intensityE( ImageSInt16 derivX , ImageSInt16 derivY , ImageFloat32 intensity )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

		ImplGradientToEdgeFeatures.intensityE(derivX,derivY,intensity);
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param intensity Edge intensity.
	 */
	static public void intensityAbs( ImageSInt16 derivX , ImageSInt16 derivY , ImageFloat32 intensity )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

		ImplGradientToEdgeFeatures.intensityAbs(derivX,derivY,intensity);
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians.
	 */
	static public void direction( ImageSInt16 derivX , ImageSInt16 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction(derivX,derivY,angle);
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param intensity Edge intensity.
	 */
	static public void intensityE( ImageSInt32 derivX , ImageSInt32 derivY , ImageFloat32 intensity )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

		ImplGradientToEdgeFeatures.intensityE(derivX,derivY,intensity);
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param intensity Edge intensity.
	 */
	static public void intensityAbs( ImageSInt32 derivX , ImageSInt32 derivY , ImageFloat32 intensity )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,intensity);

		ImplGradientToEdgeFeatures.intensityAbs(derivX,derivY,intensity);
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians.
	 */
	static public void direction( ImageSInt32 derivX , ImageSInt32 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction(derivX,derivY,angle);
	}

	/**
	 * <p>
	 * Converts an image containing edge angles into a discrete set of line angles which
	 * represent orientations of 0, 45, 90, and 135 degrees.  The conversion is done by rounding
	 * the angle to the nearest orientation in the set.
	 * </p>
	 * <p>
	 * Discrete value to angle (degrees): 0=90,1=45,2=0,3=-45
	 * </p>
	 *
	 * @param angle Input image containing edge orientations.  Orientations are assumed to be
	 * from -pi/2 to pi/2. Not modified.
	 * @param discrete  Output set of discretized angles.  Values will be from 0 to 3, inclusive. If null a new
	 * image will be declared and returned. Modified.
	 * @return Discretized direction.
	 */
	static public ImageUInt8 discretizeDirection( ImageFloat32 angle , ImageUInt8 discrete )
	{
		discrete = InputSanityCheck.checkDeclare(angle,discrete,ImageUInt8.class);

		final float A = (float)(Math.PI/2.0);
		final float B = (float)(Math.PI/4.0);
		final int w = angle.width;
		final int h = angle.height;

		for( int y = 0; y < h; y++ ) {
			int indexSrc = angle.startIndex + y*angle.stride;
			int indexDst = discrete.startIndex + y*discrete.stride;

			int end = indexSrc + w;
			for( ; indexSrc < end; indexSrc++ , indexDst++ ) {
				discrete.data[indexDst] = (byte)((int)Math.round((angle.data[indexSrc]+A)/B) % 4);
			}
		}

		return discrete;
	}

	/**
	 * <p>
	 * Sets edge intensities to zero if the pixel has an intensity which is not greater than any of
	 * the two adjacent pixels.  Pixel adjacency is determined by the gradients discretized direction.
	 * </p>
	 *
	 * @param intensity Edge intensities. Not modified.
	 * @param direction Discretized direction.  See {@link #discretizeDirection(gecv.struct.image.ImageFloat32, gecv.struct.image.ImageUInt8)}. Not modified.
	 * @param output Filtered intensity. If null a new image will be declared and returned. Modified.
	 * @return Filtered edge intensity.
	 */
	static public ImageFloat32 nonMaxSuppression( ImageFloat32 intensity , ImageUInt8 direction , ImageFloat32 output )
	{
		InputSanityCheck.checkSameShape(intensity,direction);
		output = InputSanityCheck.checkDeclare(intensity,output);

		ImplEdgeNonMaxSuppression.inner(intensity,direction,output);
		ImplEdgeNonMaxSuppression.border(intensity,direction,output);

		return output;
	}
}
