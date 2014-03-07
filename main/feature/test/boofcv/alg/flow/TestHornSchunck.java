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

package boofcv.alg.flow;

import boofcv.struct.flow.ImageFlow;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHornSchunck {

	Random rand = new Random(123);

	@Test
	public void innerAverageFlow_borderAverageFlow() {
		ImageFlow flow = new ImageFlow(30,35);
		ImageFlow found = new ImageFlow(30,35);

		for( int y = 0; y < flow.height; y++ ) {
			for( int x = 0; x < flow.width; x++ ) {
				flow.get(x,y).x = rand.nextFloat()*2;
				flow.get(x,y).y = rand.nextFloat()*2;
			}
		}

		HornSchunck.borderAverageFlow(flow, found);
		HornSchunck.innerAverageFlow(flow, found);

		ImageFlow.D expected = new ImageFlow.D();

		for( int y = 0; y < flow.height; y++ ) {
			for( int x = 0; x < flow.width; x++ ) {
				computeAverage(flow,x,y,expected);

				assertEquals(expected.x,found.get(x,y).x,1e-4);
				assertEquals(expected.y,found.get(x,y).y,1e-4);
			}
		}
	}

	private void computeAverage( ImageFlow flow , int x , int y , ImageFlow.D expected )  {
		expected.x = expected.y = 0;

		addValue(flow,x+1,y  ,0.1666667f,expected);
		addValue(flow,x-1,y  ,0.1666667f,expected);
		addValue(flow,x  ,y+1,0.1666667f,expected);
		addValue(flow,x  ,y-1,0.1666667f,expected);

		addValue(flow,x+1,y+1,0.08333333f,expected);
		addValue(flow,x-1,y+1,0.08333333f,expected);
		addValue(flow,x+1,y-1,0.08333333f,expected);
		addValue(flow,x-1,y-1,0.08333333f,expected);
	}

	private void addValue( ImageFlow flow , int x , int y , float coef , ImageFlow.D expected ) {
		if( x < 0 ) x = 0;
		else if( x >= flow.width ) x = flow.width - 1;
		if( y < 0 ) y = 0;
		else if( y >= flow.height ) y = flow.height - 1;

		ImageFlow.D a = flow.get(x,y);
		expected.x += a.x*coef;
		expected.y += a.y*coef;

	}

}
