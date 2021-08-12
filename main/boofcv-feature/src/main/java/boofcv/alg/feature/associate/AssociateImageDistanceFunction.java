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

package boofcv.alg.feature.associate;

import georegression.struct.point.Point2D_F64;

/**
 * Computes the distance between two points. For use in {@link AssociateGreedyBase2D}
 */
public interface AssociateImageDistanceFunction {
	/**
	 * The source point which the distance is computed relative to
	 *
	 * @param index Index in the source list
	 * @param pixel pixel value
	 */
	void setSource( int index, Point2D_F64 pixel );

	/**
	 * Distance from the source
	 *
	 * @param index Index in dst list
	 * @param pixel pixel value
	 */
	double distance( int index, Point2D_F64 pixel );

	/** Copy for concurrency. All functions must be thread safe */
	AssociateImageDistanceFunction copyConcurrent();

	/**
	 * Converts a distance specified in pixels into the output distanec measure
	 */
	double convertPixelsToDistance( double pixels );
}
