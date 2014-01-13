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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.edge.impl.ImplEdgeNonMaxSuppression;
import boofcv.alg.feature.detect.edge.impl.ImplEdgeNonMaxSuppressionCrude;
import boofcv.alg.feature.detect.edge.impl.ImplGradientToEdgeFeatures;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSInt8;

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
	 * Computes the edge orientation using the {@link Math#atan} function.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians (-pi/2 to pi/2).
	 */
	static public void direction( ImageFloat32 derivX , ImageFloat32 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction(derivX,derivY,angle);
	}

	/**
	 * Computes the edge orientation using the {@link Math#atan2} function.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians (-pi to pi).
	 */
	static public void direction2( ImageFloat32 derivX , ImageFloat32 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction2(derivX,derivY,angle);
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
	 * Computes the edge orientation using the {@link Math#atan} function.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians (-pi/2 to pi/2).
	 */
	static public void direction( ImageSInt16 derivX , ImageSInt16 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction(derivX,derivY,angle);
	}

	/**
	 * Computes the edge orientation using the {@link Math#atan2} function.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians (-pi to pi).
	 */
	static public void direction2( ImageSInt16 derivX , ImageSInt16 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction2(derivX,derivY,angle);
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
	 * Computes the edge orientation using the {@link Math#atan} function.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians (-pi/2 to pi/2).
	 */
	static public void direction( ImageSInt32 derivX , ImageSInt32 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction(derivX,derivY,angle);
	}

	/**
	 * Computes the edge orientation using the {@link Math#atan2} function.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians (-pi to pi).
	 */
	static public void direction2( ImageSInt32 derivX , ImageSInt32 derivY , ImageFloat32 angle )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,angle);

		ImplGradientToEdgeFeatures.direction2(derivX,derivY,angle);
	}

	/**
	 * <p>
	 * Converts an image containing edge angles (-pi/2 to pi/2) into a discrete set of angles.
	 * The conversion is done by rounding the angle to the nearest orientation in the set.
	 * </p>
	 * <p>
	 * Discrete value to angle (degrees): 0=0,1=45,2=90,-1=-45
	 * </p>
	 *
	 * @param angle Input image containing edge orientations.  Orientations are assumed to be
	 * from -pi/2 to pi/2. Not modified.
	 * @param discrete  Output set of discretized angles.  Values will be from -1 to 2, inclusive. If null a new
	 * image will be declared and returned. Modified.
	 * @return Discretized direction.
	 */
	static public ImageSInt8 discretizeDirection4( ImageFloat32 angle , ImageSInt8 discrete )
	{
		discrete = InputSanityCheck.checkDeclare(angle,discrete,ImageSInt8.class);

		final float A = (float)(Math.PI/8.0);
		final float B = (float)(Math.PI/4.0);
		final int w = angle.width;
		final int h = angle.height;

		for( int y = 0; y < h; y++ ) {
			int indexSrc = angle.startIndex + y*angle.stride;
			int indexDst = discrete.startIndex + y*discrete.stride;

			int end = indexSrc + w;
			for( ; indexSrc < end; indexSrc++ , indexDst++ ) {
				float a = angle.data[indexSrc];
				int val;
				if( a >= 0 ) {
					val = (int)((a+A)/B);
				} else {
					val = (int)((a-A)/B);
				}
				discrete.data[indexDst] = (byte)(val == -2 ? 2 : val);
			}
		}

		return discrete;
	}

	/**
	 * <p>
	 * Converts an image containing edge angles (-pi to pi) into a discrete set of 8 angles.
	 * The conversion is done by rounding the angle to the nearest orientation in the set.
	 * </p>
	 * <p>
	 * Discrete value to angle (degrees): 0=0,1=45,2=90,3=135,4=180,-1=-45,-2=--90,-3=-135
	 * </p>
	 *
	 * @param angle Input image containing edge orientations.  Orientations are assumed to be
	 * from -pi to pi. Not modified.
	 * @param discrete  Output set of discretized angles.  Values will be from -3 to 4, inclusive. If null a new
	 * image will be declared and returned. Modified.
	 * @return Discretized direction.
	 */
	static public ImageSInt8 discretizeDirection8( ImageFloat32 angle , ImageSInt8 discrete )
	{
		discrete = InputSanityCheck.checkDeclare(angle,discrete,ImageSInt8.class);

		final float A = (float)(Math.PI/8.0);
		final float B = (float)(Math.PI/4.0);
		final int w = angle.width;
		final int h = angle.height;

		for( int y = 0; y < h; y++ ) {
			int indexSrc = angle.startIndex + y*angle.stride;
			int indexDst = discrete.startIndex + y*discrete.stride;

			int end = indexSrc + w;
			for( ; indexSrc < end; indexSrc++ , indexDst++ ) {
				float a = angle.data[indexSrc];
				int val;
				if( a >= 0 ) {
					val = (int)((a+A)/B);
				} else {
					val = (int)((a-A)/B);
				}
				discrete.data[indexDst] = (byte)(val == -4 ? 4 : val);
			}
		}

		return discrete;
	}

	/**
	 * <p>
	 * Sets edge intensities to zero if the pixel has an intensity which is less than either of
	 * the two adjacent pixels.  Pixel adjacency is determined by the gradients discretized direction.
	 * </p>
	 *
	 * @param intensity Edge intensities. Not modified.
	 * @param direction 4-Discretized direction.  See {@link #discretizeDirection4(boofcv.struct.image.ImageFloat32, boofcv.struct.image.ImageSInt8)}. Not modified.
	 * @param output Filtered intensity. If null a new image will be declared and returned. Modified.
	 * @return Filtered edge intensity.
	 */
	static public ImageFloat32 nonMaxSuppression4( ImageFloat32 intensity , ImageSInt8 direction , ImageFloat32 output )
	{
		InputSanityCheck.checkSameShape(intensity,direction);
		output = InputSanityCheck.checkDeclare(intensity,output);

		ImplEdgeNonMaxSuppression.inner4(intensity,direction,output);
		ImplEdgeNonMaxSuppression.border4(intensity,direction,output);

		return output;
	}

	/**
	 * <p>
	 * Sets edge intensities to zero if the pixel has an intensity which is less than either of
	 * the two adjacent pixels.  Pixel adjacency is determined by the gradients discretized direction.
	 * </p>
	 *
	 * @param intensity Edge intensities. Not modified.
	 * @param direction 8-Discretized direction.  See {@link #discretizeDirection8(boofcv.struct.image.ImageFloat32, boofcv.struct.image.ImageSInt8)}. Not modified.
	 * @param output Filtered intensity. If null a new image will be declared and returned. Modified.
	 * @return Filtered edge intensity.
	 */
	static public ImageFloat32 nonMaxSuppression8( ImageFloat32 intensity , ImageSInt8 direction , ImageFloat32 output )
	{
		InputSanityCheck.checkSameShape(intensity,direction);
		output = InputSanityCheck.checkDeclare(intensity,output);

		ImplEdgeNonMaxSuppression.inner8(intensity,direction,output);
		ImplEdgeNonMaxSuppression.border8(intensity,direction,output);

		return output;
	}

	/**
	 * <p>
	 * Sets edge intensities to zero if the pixel has an intensity which is less than any of
	 * the two adjacent pixels.  Pixel adjacency is determined based upon the sign of the image gradient.  Less precise
	 * than other methods, but faster.
	 * </p>
	 *
	 * @param intensity Edge intensities. Not modified.
	 * @param derivX Image derivative along x-axis.
	 * @param derivY Image derivative along y-axis.
	 * @param output Filtered intensity. If null a new image will be declared and returned. Modified.
	 * @return Filtered edge intensity.
	 */
	static public ImageFloat32 nonMaxSuppressionCrude4( ImageFloat32 intensity , ImageFloat32 derivX , ImageFloat32 derivY, ImageFloat32 output )
	{
		InputSanityCheck.checkSameShape(intensity,derivX,derivY);
		output = InputSanityCheck.checkDeclare(intensity,output);

		ImplEdgeNonMaxSuppressionCrude.inner4(intensity, derivX,derivY, output);
		ImplEdgeNonMaxSuppressionCrude.border4(intensity,derivX,derivY, output);

		return output;
	}

	/**
	 * <p>
	 * Sets edge intensities to zero if the pixel has an intensity which is less than any of
	 * the two adjacent pixels.  Pixel adjacency is determined based upon the sign of the image gradient.  Less precise
	 * than other methods, but faster.
	 * </p>
	 *
	 * @param intensity Edge intensities. Not modified.
	 * @param derivX Image derivative along x-axis.
	 * @param derivY Image derivative along y-axis.
	 * @param output Filtered intensity. If null a new image will be declared and returned. Modified.
	 * @return Filtered edge intensity.
	 */
	static public ImageFloat32 nonMaxSuppressionCrude4( ImageFloat32 intensity , ImageSInt16 derivX , ImageSInt16 derivY, ImageFloat32 output )
	{
		InputSanityCheck.checkSameShape(intensity,derivX,derivY);
		output = InputSanityCheck.checkDeclare(intensity,output);

		ImplEdgeNonMaxSuppressionCrude.inner4(intensity, derivX,derivY, output);
		ImplEdgeNonMaxSuppressionCrude.border4(intensity,derivX,derivY, output);

		return output;
	}

	/**
	 * <p>
	 * Sets edge intensities to zero if the pixel has an intensity which is less than any of
	 * the two adjacent pixels.  Pixel adjacency is determined based upon the sign of the image gradient.  Less precise
	 * than other methods, but faster.
	 * </p>
	 *
	 * @param intensity Edge intensities. Not modified.
	 * @param derivX Image derivative along x-axis.
	 * @param derivY Image derivative along y-axis.
	 * @param output Filtered intensity. If null a new image will be declared and returned. Modified.
	 * @return Filtered edge intensity.
	 */
	static public ImageFloat32 nonMaxSuppressionCrude4( ImageFloat32 intensity , ImageSInt32 derivX , ImageSInt32 derivY, ImageFloat32 output )
	{
		InputSanityCheck.checkSameShape(intensity,derivX,derivY);
		output = InputSanityCheck.checkDeclare(intensity,output);

		ImplEdgeNonMaxSuppressionCrude.inner4(intensity, derivX,derivY, output);
		ImplEdgeNonMaxSuppressionCrude.border4(intensity,derivX,derivY, output);

		return output;
	}
}
