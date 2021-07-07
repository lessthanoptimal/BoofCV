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

package boofcv.alg.geo.calibration;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestZhang99CalibrationMatrixFromHomography extends BoofStandardJUnit {

	List<DMatrixRMaj> homographies;

	@Test void withSkew() {

		DMatrixRMaj K = GenericCalibrationGrid.createStandardCalibration();

		// try different numbers of observations
		for( int N = 3; N <= 6; N++ ) {
			homographies = GenericCalibrationGrid.createHomographies(K, N, rand);

			Zhang99CalibrationMatrixFromHomographies alg =
					new Zhang99CalibrationMatrixFromHomographies(false);

			alg.process(homographies);

			DMatrixRMaj K_found = alg.getCalibrationMatrix();

			checkK(K,K_found);
		}
	}

	@Test void withNoSkew() {

		// try different sizes
		for( int N = 2; N <= 5; N++ ) {
			DMatrixRMaj K = GenericCalibrationGrid.createStandardCalibration();
			// force skew to zero
			K.set(0,1,0);

			homographies = GenericCalibrationGrid.createHomographies(K, N, rand);

			Zhang99CalibrationMatrixFromHomographies alg =
					new Zhang99CalibrationMatrixFromHomographies(true);

			alg.process(homographies);

			DMatrixRMaj K_found = alg.getCalibrationMatrix();

			checkK(K, K_found);
		}
	}

	/**
	 * compare two calibration matrices against each other taking in account the differences in tolerance
	 * for different elements
	 */
	private void checkK( DMatrixRMaj a , DMatrixRMaj b ) {
		assertEquals(a.get(0,0),b.get(0,0),0.05);
		assertEquals(a.get(1,1),b.get(1,1),0.05);
		assertEquals(a.get(0,1),b.get(0,1),0.01);
		assertEquals(a.get(0,2),b.get(0,2),2);
		assertEquals(a.get(1,2),b.get(1,2),2);
		assertEquals(a.get(2,2),b.get(2,2),1e-8);
	}


}
