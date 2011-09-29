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

import java.util.Random;

/**
 * Creates different brief descriptors.  The original paper proposed several different configurations.  The
 * best one were randomly generated with a gaussian distribution.
 *
 * @author Peter Abeles
 */
public class FactoryBriefDefinition {

	/**
	 * Creates a descriptor by randomly selecting points inside a square region using a Gaussian distribution
	 * with a sigma of (5/2)*radius.
	 *
	 * @param rand Random number generator.
	 * @param radius Radius of the square region.  width = 2*radius+1.
	 * @param numPairs Number of sample point pairs.
	 * @return Definition of a BRIEF feature.
	 */
	public static BriefDefinition gaussian( Random rand, int radius , int numPairs ) {
		BriefDefinition ret = new BriefDefinition(radius,numPairs);

		double sigma = (2.0*radius+1.0)/5.0;
		for( int i = 0; i < numPairs; i++ ) {
			randomGaussian(rand,sigma,radius,ret.setA[i]);
			randomGaussian(rand,sigma,radius,ret.setB[i]);
		}

		return ret;
	}


	/**
	 * Randomly selects a point which is inside a square region using a Gaussian distribution.
	 */
	private static void randomGaussian( Random rand , double sigma , int radius , Point2D_I32 pt ) {

		int x,y;

		while( true ) {
			x = (int)(rand.nextGaussian()*sigma);
			if( x >= -radius && x <= radius )
				break;
		}

		while( true ) {
			y = (int)(rand.nextGaussian()*sigma);
			if( y >= -radius && y <= radius )
				break;
		}

		pt.set(x,y);
	}
}
