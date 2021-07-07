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

package boofcv.alg.feature.describe;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * Standard very basic tests for descriptions using TupleDesc_F64
 *
 * @author Peter Abeles
 */
public abstract class StandardTupleDescribeTests extends BoofStandardJUnit {

	int width = 50;
	int height = 60;

	int c_x = width/2;
	int c_y = height/2;

	int radius = 10;

	GrayF32 image = new GrayF32(width,height);

	protected StandardTupleDescribeTests() {
		GImageMiscOps.fillUniform(image, rand, 0, 100);
	}

	public abstract TupleDesc_F64 describe( int x , int y , double theta , GrayF32 image );

	/**
	 * Does it produce a the same features when given a subimage?
	 */
	@Test void checkSubImage()
	{
		TupleDesc_F64 expected = describe(c_x,c_y,0,image);
		GrayF32 sub = BoofTesting.createSubImageOf(image);
		TupleDesc_F64 found = describe(c_x,c_y,0,sub);

		BoofTesting.assertEquals(expected.data,found.data,1e-8);
	}

	/**
	 * Does it produce a different feature when rotated?
	 */
	@Test void changeRotation() {
		TupleDesc_F64 a = describe(c_x,c_y,0,image);
		TupleDesc_F64 b = describe(c_x,c_y,1,image);

		boolean equals = true;
		for(int i = 0; i < a.data.length; i++ ) {
			double diff = Math.abs(a.data[i]-b.data[i]);
			if( diff > 1e-8 ) {
				equals = false;
				break;
			}
		}
		assertFalse(equals);
	}
}
