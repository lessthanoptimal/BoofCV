/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
 * Describes the layout of a BRIEF descriptor.  This descriptor is composed of a set of locations
 * where image intensity is sampled and compared against each other.
 *
 * @author Peter Abeles
 */
public class BriefDefinition {
	// radius of the region
	public int radius;
	// first set of locations
	public Point2D_I32 setA[];
	// second set of locations
	public Point2D_I32 setB[];

	public BriefDefinition( int radius , int numPairs ) {
		this.radius = radius;
		setA = new Point2D_I32[ numPairs ];
		setB = new Point2D_I32[ numPairs ];

		for( int i = 0; i < setA.length; i++ ) {
			setA[i] = new Point2D_I32();
			setB[i] = new Point2D_I32();
		}
	}

	/**
	 * Length of the descriptor (or number of bits required to encode it)
	 *
	 * @return Descriptor length.
	 */
	public int getLength() {
		return setA.length;
	}
}
