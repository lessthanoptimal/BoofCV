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

package boofcv.alg.feature.describe;

import boofcv.struct.feature.TupleDesc_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribeSiftCommon {

	@Test
	public void normalizeDescriptor() {
		TupleDesc_F64 descriptor = new TupleDesc_F64(128);
		descriptor.value[5] = 100;
		descriptor.value[20] = 120;
		descriptor.value[60] = 20;

		DescribeSiftCommon alg = new DescribeSiftCommon(4,4,8,0.5,0.2);
		alg.normalizeDescriptor(descriptor,alg.maxDescriptorElementValue);

		assertEquals(1,normL2(descriptor),1e-8);

		// cropping should make 5 and 20 the same
		assertEquals(descriptor.value[5],descriptor.value[20],1e-8);
	}

	private double normL2( TupleDesc_F64 desc ) {
		double total = 0;
		for( double d : desc.value) {
			total += d*d;
		}
		return Math.sqrt(total);
	}

	/**
	 * Tests trilinear interpolation by checking out some of its properties instead of its value
	 * exactly
	 */
	@Test
	public void trilinearInterpolation() {
		DescribeSiftCommon alg = new DescribeSiftCommon(4,4,8,0.5,0.2);

		TupleDesc_F64 descriptor = new TupleDesc_F64(128);

		// in the middle of the feature, the total amount added to the descriptor should equal the input weight
		// upper edges will have a value less than the input weight
		alg.trilinearInterpolation(2.0f,1.25f,2.0f,0.5,descriptor);

		double sum = 0;
		int count = 0;
		for (int i = 0; i < descriptor.size(); i++) {
			sum += descriptor.value[i];
			if( descriptor.value[i] != 0 )
				count++;
		}
		assertEquals(2.0,sum,1e-6);
		assertTrue(count>1);

		// try an edge case
		sum = 0;
		descriptor.fill(0);
		alg.trilinearInterpolation(2.0f,3.25f,3.25f,0.5,descriptor);
		for (int i = 0; i < descriptor.size(); i++) {
			sum += descriptor.value[i];
		}
		assertEquals( 2.0*0.75*0.75*1.0, sum, 1e-8 );

		// now have something exactly at the start of a bin.  all the weight should be in one location
		descriptor.fill(0);
		alg.trilinearInterpolation(2.0f,3f,3f,2*Math.PI/8,descriptor);
		count = 0;
		for (int i = 0; i < descriptor.size(); i++) {
			double weight = descriptor.value[i];
			if( weight > 0 ) {
				assertEquals(2.0,weight,1e-8);
				count++;
			}
		}
		assertEquals(1,count);
	}
}