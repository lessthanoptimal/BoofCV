/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.describe.impl;

import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestImplSurfDescribeOps {

	Random rand = new Random(234);
	int width = 30;
	int height = 40;

	@Test
	public void gradientInner_F32() {
		ImageFloat32 ii = new ImageFloat32(width,height);
		GeneralizedImageOps.randomize(ii,rand,0,100);

		int r = 2;
		int w = r*2+1;

		double expectedX[] = new double[w*w];
		double expectedY[] = new double[w*w];
		double foundX[] = new double[w*w];
		double foundY[] = new double[w*w];

		NaiveSurfDescribeOps.gradient(ii,10,10,r,1,false,expectedX,expectedY);
		ImplSurfDescribeOps.gradientInner(ii,2,1,10-r,10-r,10+r+1,10+r+1,0,0,foundX,foundY);

		for( int i = 0; i < foundX.length; i++ ) {
			assertEquals("at "+i,expectedX[i],foundX[i],1e-4);
			assertEquals("at "+i,expectedY[i],foundY[i],1e-4);
		}

		NaiveSurfDescribeOps.gradient(ii,10,10,r,1.5,false,expectedX,expectedY);
		ImplSurfDescribeOps.gradientInner(ii,3,1,10-r,10-r,10+r+1,10+r+1,0,0,foundX,foundY);

		for( int i = 0; i < foundX.length; i++ ) {
			assertEquals("at "+i,expectedX[i],foundX[i],1e-4);
			assertEquals("at "+i,expectedY[i],foundY[i],1e-4);
		}
	}
}
