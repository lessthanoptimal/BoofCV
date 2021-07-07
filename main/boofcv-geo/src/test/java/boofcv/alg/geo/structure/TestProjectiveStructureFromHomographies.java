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
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestProjectiveStructureFromHomographies extends CommonStructure {

	/**
	 * Given perfect input see if it can create camera matrices which will return the original observations
	 */
	@Test void perfect_input() {
		int numViews = 8;
		int numFeatures = 20;
		simulate(numViews,numFeatures,true);

		ProjectiveStructureFromHomographies alg = new ProjectiveStructureFromHomographies();

		assertTrue(alg.proccess(computeHomographies(),convertToPointIndex(),numFeatures));

		DMatrixRMaj P = new DMatrixRMaj(3,4);
		Point3D_F64 X = new Point3D_F64();
		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			alg.getCameraMatrix(viewIdx,P);

			for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
				alg.getFeature3D(featureIdx,X);

				Point2D_F64 expected = observations.get(viewIdx).get(featureIdx);
				Point2D_F64 x = PerspectiveOps.renderPixel(P,X,(Point2D_F64)null);

				assertTrue( expected.distance(x) < UtilEjml.TEST_F64_SQ );
			}
		}
	}

	/**
	 * See if it correctly detects points on plane at infinity.
	 */
	@Test void filterPointsOnPlaneAtInfinity() {
		// hand construct a 3x3 matrix to make the z for a specific point zero

		DMatrixRMaj H = new DMatrixRMaj(new double[][]{{1,2,3},{4,5,6},{2,-1,-1}});

		List<DMatrixRMaj> homographies = new ArrayList<>();
		homographies.add(H);
		homographies.add(H);
		homographies.add(H);

		List<List<PointIndex2D_F64>> points = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			points.add(new ArrayList<>());
		}

		points.get(0).add( new PointIndex2D_F64(1,1.5,0));
		points.get(0).add( new PointIndex2D_F64(2,1.5,0));
		points.get(0).add( new PointIndex2D_F64(8,1.5,0));
		points.get(1).add( new PointIndex2D_F64(1,1,0));
		points.get(2).add( new PointIndex2D_F64(4,1.5,0));

		ProjectiveStructureFromHomographies alg = new ProjectiveStructureFromHomographies();

		alg.filterPointsOnPlaneAtInfinity(homographies,points,3);

		assertEquals(3,alg.filtered.get(0).size());
		assertEquals(0,alg.filtered.get(1).size());
		assertEquals(1,alg.filtered.get(2).size());
	}

	/**
	 * Tests linear system by seeing if zero is returned when truth is passed in. Input camera matrix is
	 * K*[R,T] to make the match much easier.
	 */
	@Test void constructLinearSystem() {
		int numViews = 4;
		int numFeatures = 7;
		simulate(numViews,numFeatures,true);

		ProjectiveStructureFromHomographies alg = new ProjectiveStructureFromHomographies();

		List<DMatrixRMaj> homographies = new ArrayList<>();
		for (int i = 0; i < projections.size(); i++) {
			DMatrixRMaj R = new DMatrixRMaj(3,3);
			CommonOps_DDRM.extract(projections.get(i),0,0,R);
			homographies.add( R );
		}
		List<List<PointIndex2D_F64>> observations = convertToPointIndex();

		alg.computeConstants(homographies,observations,numFeatures);

		alg.constructLinearSystem(homographies,observations);

		DMatrixRMaj X = new DMatrixRMaj(alg.numUnknown,1);

		for (int i = 0; i < features3D.size(); i++) {
			int row = i*3;
			X.data[row  ] = features3D.get(i).x;
			X.data[row+1] = features3D.get(i).y;
			X.data[row+2] = features3D.get(i).z;
		}

		for (int i = 0; i < homographies.size(); i++) {
			// homography is from view 0 to view i
			DMatrixRMaj P = projections.get(i);

			int row = features3D.size()*3 + i*3;
			X.data[row  ] = P.get(0,3);
			X.data[row+1] = P.get(1,3);
			X.data[row+2] = P.get(2,3);
		}

		DMatrixRMaj B = new DMatrixRMaj(alg.numEquations,1);

		CommonOps_DDRM.mult(alg.A,X,B);

		assertEquals(0,NormOps_DDRM.normF(B), UtilEjml.TEST_F64);
	}
}
