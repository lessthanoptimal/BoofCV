/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.tld;

import georegression.struct.point.Point2D_F32;

import java.util.Random;

/**
 * Fern descriptor used in {@link TldTracker}.  The number of features can be at most 32, enough to fit inside
 * an integer. The location of each point is from -0.5 to 0.5 and randomly selected.  When computed it is scaled
 * independently along x and y axis to the region's width and height, respectively.
 *
 * @author Peter Abeles
 */
public class TldFernDescription {

	/**
	 * Pairs used to compute fern.  Must be &le; 32 to fit inside an integer
	 */
	SamplePair pairs[];

	/**
	 * Creates random fern.
	 *
	 * @param rand Random number generator used to select sample locations
	 * @param num Number of features/pairs
	 */
	public TldFernDescription(Random rand, int num) {
		if( num < 1 || num > 32 )
			throw new IllegalArgumentException("Number of pairs must be from 1 to 32, inclusive");

		pairs = new SamplePair[num];
		for( int i = 0; i < num; i++ ) {
			SamplePair p = new SamplePair();

			p.a.set( rand.nextFloat()-0.5f , rand.nextFloat()-0.5f );
			p.b.set( rand.nextFloat()-0.5f , rand.nextFloat()-0.5f );

			pairs[i] = p;
		}
	}

	public static class SamplePair
	{
		Point2D_F32 a = new Point2D_F32();
		Point2D_F32 b = new Point2D_F32();
	}
}
