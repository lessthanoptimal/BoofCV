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

package boofcv.factory.weights;

import boofcv.alg.weights.*;

/**
 * Factory for creating sample weight functions of different types.
 *
 * @author Peter Abeles
 */
public class FactoryWeights {

	/**
	 * Creates a weight function for the provided distributions.
	 *
	 * @param type Which type of distribution should be used
	 * @param param Distribution parameters.  For uniform this is the maximum distance.
	 *              Guassian its the standard deviation.
	 * @param safe If true it will then check the input distance to see if it matches.
	 * @return WeightDistance_F32
	 */
	public static WeightDistance_F32 distance( WeightType type , float param , boolean safe ) {

		if( safe )
			throw new IllegalArgumentException("Safe distributons not implemented yet");

		switch( type ) {
			case GAUSSIAN_SQ:
				return new WeightDistanceSqGaussian_F32(param);

			case UNIFORM:
				return new WeightDistanceUniform_F32(param);
		}

		throw new IllegalArgumentException("Unknown type "+type);
	}

	/**
	 * Creates a weight function for the provided distributions.
	 *
	 * @param type Which type of distribution should be used
	 * @param safe If true it will then check the input distance to see if it matches.
	 * @return WeightDistance_F32
	 */
	public static WeightPixel_F32 pixel( WeightType type , boolean safe ) {

		if( safe )
			throw new IllegalArgumentException("Safe distributons not implemented yet");

		switch( type ) {
			case GAUSSIAN_SQ:
				return new WeightPixelGaussian_F32();

			case UNIFORM:
				return new WeightPixelUniform_F32();
		}

		throw new IllegalArgumentException("Unknown type "+type);
	}
}
