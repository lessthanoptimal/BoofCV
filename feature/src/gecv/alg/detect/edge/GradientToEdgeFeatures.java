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

package gecv.alg.detect.edge;

import gecv.alg.InputSanityCheck;
import gecv.alg.detect.edge.impl.ImplGradientToEdgeFeatures;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;

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
}
