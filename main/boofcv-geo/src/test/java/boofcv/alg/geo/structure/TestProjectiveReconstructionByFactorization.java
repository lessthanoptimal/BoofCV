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

package boofcv.alg.geo.structure;

import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestProjectiveReconstructionByFactorization extends CommonStructure {

	/**
	 * Perfect observations and perfect input, Output should be just perfect
	 */
	@Test void perfect_input() {
		int numViews = 8;
		int numFeatures = 10;
		simulate(numViews,numFeatures,false);

		ProjectiveStructureByFactorization alg = new ProjectiveStructureByFactorization();
		alg.initialize(features3D.size(),projections.size());

		// Perfect depths. It should only need 1 iteration
		setPerfectDepths(numFeatures, alg);

		// noise free pixel observations too
		for (int viewIdx = 0; viewIdx < projections.size(); viewIdx++) {
			alg.setPixels(viewIdx,observations.get(viewIdx));
		}

		assertTrue(alg.process());

		DMatrixRMaj P = new DMatrixRMaj(3,4);
		Point4D_F64 X = new Point4D_F64();
		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			alg.getCameraMatrix(viewIdx,P);

			for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
				alg.getFeature3D(featureIdx,X);

				Point2D_F64 expected = observations.get(viewIdx).get(featureIdx);
				Point3D_F64 xh = PerspectiveOps.renderPixel(P,X,(Point3D_F64)null);

				Point2D_F64 found = new Point2D_F64(xh.x/xh.z, xh.y/xh.z);

				assertTrue( expected.distance(found) < UtilEjml.TEST_F64 );
			}
		}
	}

	/**
	 * All data is perfect, but 1 is used for the depth estimate
	 */
	@Test void perfect_input_badDepths() {
		int numViews = 8;
		int numFeatures = 10;
		simulate(numViews,numFeatures,false);

		ProjectiveStructureByFactorization alg = new ProjectiveStructureByFactorization();
		alg.initialize(features3D.size(),projections.size());

		// depth isn't known so just set it to 1. it could easily converge to a poor local optimal
		alg.setAllDepths(1);

		for (int viewIdx = 0; viewIdx < projections.size(); viewIdx++) {
			alg.setPixels(viewIdx,observations.get(viewIdx));
		}

		assertTrue(alg.process());

		DMatrixRMaj P = new DMatrixRMaj(3,4);
		Point4D_F64 X = new Point4D_F64();
		int total = 0;
		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			alg.getCameraMatrix(viewIdx,P);

			for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
				alg.getFeature3D(featureIdx,X);

				Point2D_F64 expected = observations.get(viewIdx).get(featureIdx);
				Point3D_F64 xh = PerspectiveOps.renderPixel(P,X,(Point3D_F64)null);

				Point2D_F64 found = new Point2D_F64(xh.x/xh.z, xh.y/xh.z);

//				System.out.println(expected+" "+found);
				if( expected.distance(found) <= 2 )
					total++;
			}
		}
		// see if a large number of solutions are within 2 pixels
		assertTrue( total >= 0.95*numViews*numFeatures );
	}

	private void setPerfectDepths(int numFeatures, ProjectiveStructureByFactorization alg) {
		double[] depths = new double[numFeatures];
		Point3D_F64 X = new Point3D_F64();
		for (int viewIdx = 0; viewIdx < projections.size(); viewIdx++) {
			for (int featIdx = 0; featIdx < features3D.size(); featIdx++) {
				worldToViews.get(viewIdx).transform(features3D.get(featIdx),X);
				depths[featIdx] = X.z;
			}
			alg.setDepths(viewIdx,depths);
		}
	}


}
