/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.struct.image.InterleavedF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHornSchunck extends BoofStandardJUnit {

	@Test void innerAverageFlow_borderAverageFlow() {
		InterleavedF32 flow = new InterleavedF32(30,35,2);
		InterleavedF32 found = new InterleavedF32(30,35,2);

		for( int y = 0; y < flow.height; y++ ) {
			for( int x = 0; x < flow.width; x++ ) {
				flow.setBand(x,y,0, rand.nextFloat()*2);
				flow.setBand(x,y,1, rand.nextFloat()*2);
			}
		}

		HornSchunck.borderAverageFlow(flow, found);
		HornSchunck.innerAverageFlow(flow, found);

		float[] expected = new float[2];

		for( int y = 0; y < flow.height; y++ ) {
			for( int x = 0; x < flow.width; x++ ) {
				computeAverage(flow,x,y,expected);

				assertEquals(expected[0],found.getBand(x,y,0),1e-4);
				assertEquals(expected[1],found.getBand(x,y,1),1e-4);
			}
		}
	}

	private void computeAverage( InterleavedF32 flow , int x , int y , float[] expected )  {
		expected[0] = expected[1] = 0;

		addValue(flow,x+1,y  ,0.1666667f,expected);
		addValue(flow,x-1,y  ,0.1666667f,expected);
		addValue(flow,x  ,y+1,0.1666667f,expected);
		addValue(flow,x  ,y-1,0.1666667f,expected);

		addValue(flow,x+1,y+1,0.08333333f,expected);
		addValue(flow,x-1,y+1,0.08333333f,expected);
		addValue(flow,x+1,y-1,0.08333333f,expected);
		addValue(flow,x-1,y-1,0.08333333f,expected);
	}

	private void addValue( InterleavedF32 flow , int x , int y , float coef , float[] expected ) {
		if( x < 0 ) x = 0;
		else if( x >= flow.width ) x = flow.width - 1;
		if( y < 0 ) y = 0;
		else if( y >= flow.height ) y = flow.height - 1;

		expected[0] += flow.getBand(x,y,0)*coef;
		expected[1] += flow.getBand(x,y,1)*coef;

	}

}
