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
public class TestFitQuadratic3by3 {
	
	Random rand = new Random(234);

	/**
	 * Impulse function with a peak at zero.  should be no change
	 */
	@Test
	public void impulseAtZero() {
		FitQuadratic3by3 alg = new FitQuadratic3by3();

		for( int i = -1; i <= 1; i++ ) {
			for( int j = -1; j <= 1; j++ ) {
				alg.setValue(j,i,2);
			}
		}
		alg.setValue(0,0,5);
		
		alg.process();
		
		assertEquals(0,alg.getDeltaX(),1e-8);
		assertEquals(0,alg.getDeltaY(),1e-8);
	}

	/**
	 * skewed in an x-direction
	 */
	@Test
	public void skewedX() {
		FitQuadratic3by3 alg = new FitQuadratic3by3();

		for( int i = -1; i <= 1; i++ ) {
			for( int j = -1; j <= 1; j++ ) {
				alg.setValue(j,i,2);
			}
		}
		alg.setValue(0,0,5);
		// give it larger values on the -1 side
		alg.setValue(-1, 0,4);

		alg.process();

		assertTrue(alg.getDeltaX() < 0);
		assertTrue(alg.getDeltaX() >= -1);
		assertEquals(0,alg.getDeltaY(),1e-8);
	}

	/**
	 * skewed in an x-direction
	 */
	@Test
	public void skewedY() {
		FitQuadratic3by3 alg = new FitQuadratic3by3();

		for( int i = -1; i <= 1; i++ ) {
			for( int j = -1; j <= 1; j++ ) {
				alg.setValue(j,i,2);
			}
		}
		alg.setValue(0,0,5);
		// give it larger values on the -1 side
		alg.setValue(0, -1,4);

		alg.process();

		assertTrue(alg.getDeltaY() < 0);
		assertTrue(alg.getDeltaY() >= -1);
		assertEquals(0,alg.getDeltaX(),1e-8);
	}

}
