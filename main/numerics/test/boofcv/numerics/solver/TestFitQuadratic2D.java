/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.solver;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFitQuadratic2D {

	Random rand = new Random(234);
	
	/**
	 * Impulse function with a peak at zero.  should be no change
	 */
	@Test
	public void impulseAtZero() {
		FitQuadratic2D alg = new FitQuadratic2D();

		alg.reset();

		for( int i = -4; i <= 4; i++ ) {
			for( int j = -2; j <= 2; j++ ) {
				if( i == 0 && j == 0 )
					alg.add(i,j,5);
				else
					alg.add(j, i, 2);
			}
		}

		assertTrue(alg.process());

		assertEquals(0, alg.getFoundX(),1e-8);
		assertEquals(0, alg.getFoundY(), 1e-8);
	}

	/**
	 * Impulse function with a peak at zero.
	 */
	@Test
	public void impulseNotZero() {
		FitQuadratic2D alg = new FitQuadratic2D();

		alg.reset();

		for( int i = 0; i <= 4; i++ ) {
			for( int j = -4; j <= 0; j++ ) {
				if( i == 2 && j == -2 )
					alg.add(i,j,5);
				else
					alg.add(i,j,2);
			}
		}

		assertTrue(alg.process());

		assertEquals(2, alg.getFoundX(),1e-8);
		assertEquals(-2, alg.getFoundY(), 1e-8);
	}
	
	@Test
	public void knownCoefficients() {
		double expectedX = 2;
		double expectedY = -3.5;

		double a = -2,b=-4,c=-0.5;
		double d = -2*a*expectedX-b*expectedY;
		double e = -2*c*expectedY-b*expectedX;

		FitQuadratic2D alg = new FitQuadratic2D();

		alg.reset();
		
		for( int i = 0; i < 100; i++ ) {
			double x = rand.nextGaussian()*2;
			double y = rand.nextGaussian()*4;

			double v = a*x*x + b*x*y + c*y*y + d*x + e*y;
			
			alg.add(x,y,v);
		}
		
		alg.process();
		
		assertEquals(expectedX,alg.getFoundX(),1e-8);
		assertEquals(expectedY,alg.getFoundY(),1e-8);
	}
}
