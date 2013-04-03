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

package boofcv.alg.feature.describe.brief;

import georegression.struct.point.Point2D_I32;

/**
 * <p>
 * Describes the layout of a BRIEF descriptor.  This descriptor is composed of a set of locations
 * where image intensity is sampled and a list of which locations are compared against each other.
 * </p>
 *
 * <p>
 * NOTE: The data structure here is different than the one implied in the paper.  A single list of sample points
 * is provided instead of two lists.  This way a single set of points can sample within the same set, reducing
 * the number of samples taken.
 * </p>
 *
 * @author Peter Abeles
 */
public class BinaryCompareDefinition_I32 {
	// radius of the region
	public int radius;
	// points whose intensity values are sampled
	public Point2D_I32 samplePoints[];
	// indexes of points which are compared
	public Point2D_I32 compare[];

	public BinaryCompareDefinition_I32(int radius, int numSamples, int numPairs) {
		this.radius = radius;
		samplePoints = new Point2D_I32[ numSamples ];
		compare = new Point2D_I32[ numPairs ];

		for( int i = 0; i < samplePoints.length; i++ ) {
			samplePoints[i] = new Point2D_I32();
		}
		for( int i = 0; i < compare.length; i++ ) {
			compare[i] = new Point2D_I32();
		}
	}

	/**
	 * Length of the descriptor (or number of bits required to encode it)
	 *
	 * @return Descriptor length.
	 */
	public int getLength() {
		return samplePoints.length;
	}
}
