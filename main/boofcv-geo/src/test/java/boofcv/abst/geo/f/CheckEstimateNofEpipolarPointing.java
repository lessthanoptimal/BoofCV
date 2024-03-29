/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.f;

import boofcv.abst.geo.EstimateNofEpipolar;
import boofcv.abst.geo.EstimateNofEpipolarPointing;
import boofcv.alg.geo.f.EpipolarTestSimulation;
import boofcv.struct.geo.AssociatedPair3D;
import boofcv.struct.geo.GeoModelEstimatorN;
import boofcv.struct.geo.QueueMatrix;
import georegression.geometry.GeometryMath_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applies various compliance tests for implementations of {@link EstimateNofEpipolar}
 * amd {@link GeoModelEstimatorN}.
 *
 * @author Peter Abeles
 */
public abstract class CheckEstimateNofEpipolarPointing extends EpipolarTestSimulation {

	// the algorithm being tested
	EstimateNofEpipolarPointing alg;

	// true if pixels or false if normalized
	boolean isPixels;

	protected CheckEstimateNofEpipolarPointing( EstimateNofEpipolarPointing alg, boolean pixels) {
		this.alg = alg;
		isPixels = pixels;
	}

	/**
	 * Makes sure the minimum number of points has been set
	 */
	@Test void checkMinimumPoints() {
		assertTrue(alg.getMinimumPoints()>0);
	}

	/**
	 * Make sure the ordering of the epipolar constraint is computed correctly
	 */
	@Test void checkConstraint() {
		init(50,isPixels);

		boolean workedOnce = false;

		DogArray<DMatrixRMaj> solutions = new QueueMatrix(3, 3);

		for( int i = 0; i < 10; i++ ) {
			List<AssociatedPair3D> pairs = randomPairsPointing(alg.getMinimumPoints());

			if( !alg.process(pairs,solutions)) {
				continue;
			}

			if( solutions.size() <= 0 )
				continue;

			workedOnce = true;

			for( DMatrixRMaj F : solutions.toList() ) {
				// normalize to ensure proper scaling
				double n = CommonOps_DDRM.elementMaxAbs(F);
				CommonOps_DDRM.scale(1.0/n,F);

				for( AssociatedPair3D p : pairs ) {
					double correct = Math.abs(GeometryMath_F64.innerProd(p.p2, F, p.p1));
					double wrong = Math.abs(GeometryMath_F64.innerProd(p.p1, F, p.p2));

					assertTrue(correct < wrong*0.001);
				}

			}
		}
		assertTrue(workedOnce);
	}
}
