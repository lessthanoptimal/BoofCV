/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.selector;

import boofcv.struct.image.GrayF32;
import org.jetbrains.annotations.Nullable;

/**
 * Samples the intensity at the specified point.
 *
 * @author Peter Abeles
 */
public interface SampleIntensity<Point> {
	/**
	 * Returns the intensity. If the sampling method does not require the intensity image then it may be null.
	 *
	 * @param intensity Image with intensity information. May be null if sampling method does not use it.
	 * @param index Index of the point in the list
	 * @param p Coordinate being sampled. Must be inside the image
	 * @return The intensity at the sample point
	 */
	float sample( @Nullable GrayF32 intensity, int index, Point p );

	/**
	 * Pixel coordinate x-axis
	 */
	int getX( Point p );

	/**
	 * Pixel coordinate y-axis
	 */
	int getY( Point p );
}
