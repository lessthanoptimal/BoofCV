/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.filter.kernel;

import boofcv.alg.filter.kernel.SteerableCoefficients;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestFactorySteerCoefficients extends BoofStandardJUnit {

	/**
	 * For certain angles all but one coefficient should be zero
	 */
	@Test void polynomialZeros() {
		for( int order = 1; order <= 4; order++ ) {
			SteerableCoefficients coefs = FactorySteerCoefficients.polynomial(order);

			for( int i = 0; i <= order; i++ ) {
				double angle = i*Math.PI/(order+1);

				for( int j = 0; j <= order; j++ ) {
					if( i == j )
						assertEquals(1,coefs.compute(angle,j),1e-4);
					else
						assertEquals(0,coefs.compute(angle,j),1e-4);
				}
			}
		}
	}

	/**
	 * See if it is zero
	 */
	@Test void separableZeros() {
		for( int order = 1; order <= 4; order++ ) {
			SteerableCoefficients coefs = FactorySteerCoefficients.separable(order);

			for( int i = 0; i <= order; i++ ) {
				if( i == 0 )
					assertEquals(1,coefs.compute(0,i),1e-4);
				else
					assertEquals(0,coefs.compute(0,i),1e-4);
			}
		}
	}
}
