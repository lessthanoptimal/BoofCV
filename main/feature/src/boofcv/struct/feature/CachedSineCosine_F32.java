/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.feature;


/**
 * Precomputes the output of sine/cosine operations.  Given an angle it will
 * return an approximation much faster than computing it from scratch.
 *
 * @author Peter Abeles
 */
// todo place in ConnectLinesGrid
// todo          HoughTransformLinePolar
public class CachedSineCosine_F32 {

	// minimum angle in the table
	float minAngle;
	// Maximum angle in the table
	float maxAngle;
	// radians between each step
	float delta;

	// cosine table
	public float c[];
	// sine table
	public float s[];

	public CachedSineCosine_F32( float minAngle, float maxAngle, int size )  {
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
		this.delta = (maxAngle - minAngle)/size;

		c = new float[size];
		s = new float[size];

		for( int i = 0; i < size; i++ ) {
			float angle = (maxAngle - minAngle)*i/size + minAngle;
			c[i] = (float)Math.cos(angle);
			s[i] = (float)Math.sin(angle);
		}
	}

	public int computeIndex( float angle ) {
		return (int)((angle- minAngle)/delta);
	}

}
