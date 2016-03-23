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

package boofcv.alg.feature.detect.edge;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;


/**
 * Image type agnostic version of {@link GradientToEdgeFeatures}.
 *
 * @author Peter Abeles
 */
public class GGradientToEdgeFeatures {

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param intensity Edge intensity.
	 */
	static public <D extends ImageGray>
	void intensityE( D derivX , D derivY , GrayF32 intensity )
	{
		if( derivX instanceof GrayF32) {
			GradientToEdgeFeatures.intensityE((GrayF32)derivX,(GrayF32)derivY,intensity);
		} else if( derivX instanceof GrayS16) {
			GradientToEdgeFeatures.intensityE((GrayS16)derivX,(GrayS16)derivY,intensity);
		} else if( derivX instanceof GrayS32) {
			GradientToEdgeFeatures.intensityE((GrayS32)derivX,(GrayS32)derivY,intensity);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	/**
	 * Computes the edge intensity using a Euclidean norm.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param intensity Edge intensity.
	 */
	static public <D extends ImageGray>
	void intensityAbs( D derivX , D derivY , GrayF32 intensity )
	{
		if( derivX instanceof GrayF32) {
			GradientToEdgeFeatures.intensityAbs((GrayF32)derivX,(GrayF32)derivY,intensity);
		} else if( derivX instanceof GrayS16) {
			GradientToEdgeFeatures.intensityAbs((GrayS16)derivX,(GrayS16)derivY,intensity);
		} else if( derivX instanceof GrayS32) {
			GradientToEdgeFeatures.intensityAbs((GrayS32)derivX,(GrayS32)derivY,intensity);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	/**
	 * Computes the edge orientation using the {@link Math#atan} function.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians (-pi/2 to pi/2).
	 */
	static public <D extends ImageGray>
	void direction( D derivX , D derivY , GrayF32 angle )
	{
		if( derivX instanceof GrayF32) {
			GradientToEdgeFeatures.direction((GrayF32)derivX,(GrayF32)derivY,angle);
		} else if( derivX instanceof GrayS16) {
			GradientToEdgeFeatures.direction((GrayS16)derivX,(GrayS16)derivY,angle);
		} else if( derivX instanceof GrayS32) {
			GradientToEdgeFeatures.direction((GrayS32)derivX,(GrayS32)derivY,angle);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	/**
	 * Computes the edge orientation using the {@link Math#atan2} function.
	 *
	 * @param derivX Derivative along x-axis. Not modified.
	 * @param derivY Derivative along y-axis. Not modified.
	 * @param angle Edge orientation in radians (-pi to pi).
	 */
	static public <D extends ImageGray>
	void direction2( D derivX , D derivY , GrayF32 angle )
	{
		if( derivX instanceof GrayF32) {
			GradientToEdgeFeatures.direction2((GrayF32)derivX,(GrayF32)derivY,angle);
		} else if( derivX instanceof GrayS16) {
			GradientToEdgeFeatures.direction2((GrayS16)derivX,(GrayS16)derivY,angle);
		} else if( derivX instanceof GrayS32) {
			GradientToEdgeFeatures.direction2((GrayS32)derivX,(GrayS32)derivY,angle);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
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
	 * @param output Filtered intensity. Modified.
	 */
	static public <D extends ImageGray>
	void nonMaxSuppressionCrude4(GrayF32 intensity , D derivX , D derivY , GrayF32 output )
	{
		if( derivX instanceof GrayF32) {
			GradientToEdgeFeatures.nonMaxSuppressionCrude4(intensity, (GrayF32) derivX, (GrayF32) derivY,output);
		} else if( derivX instanceof GrayS16) {
			GradientToEdgeFeatures.nonMaxSuppressionCrude4(intensity, (GrayS16) derivX, (GrayS16) derivY,output);
		} else if( derivX instanceof GrayS32) {
			GradientToEdgeFeatures.nonMaxSuppressionCrude4(intensity, (GrayS32) derivX, (GrayS32) derivY,output);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}
}
