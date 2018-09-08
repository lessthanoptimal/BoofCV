/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.calib.CameraPinhole;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestProjectiveReconstructionByFactorization {

	Random rand = new Random(234);

	CameraPinhole pinhole = new CameraPinhole(400,420,0.1,500,490,-1,-1);

	List<Point3D_F64> features3D;
	List<Se3_F64> worldToViews;
	List<DMatrixRMaj> projections;
	List<List<Point2D_F64>> observations;

	/**
	 * Perfect observations and perfect input, Output should be just perfect
	 */
	@Test
	public void perfect_input() {
		int numViews = 8;
		int numFeatures = 10;
		simulate(numViews,numFeatures);

		ProjectiveReconstructionByFactorization alg = new ProjectiveReconstructionByFactorization();
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
				Point3D_F64 xh = PerspectiveOps.renderPixel(P,X,null);

				Point2D_F64 found = new Point2D_F64(xh.x/xh.z, xh.y/xh.z);

				assertTrue( expected.distance(found) < UtilEjml.TEST_F64 );
			}
		}
	}

	/**
	 * All data is perfect, but 1 is used for the depth estimate
	 */
	@Test
	public void perfect_input_badDepths() {
		int numViews = 8;
		int numFeatures = 10;
		simulate(numViews,numFeatures);

		ProjectiveReconstructionByFactorization alg = new ProjectiveReconstructionByFactorization();
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
				Point3D_F64 xh = PerspectiveOps.renderPixel(P,X,null);

				Point2D_F64 found = new Point2D_F64(xh.x/xh.z, xh.y/xh.z);

//				System.out.println(expected+" "+found);
				if( expected.distance(found) <= 2 )
					total++;
			}
		}
		// see if a large number of solutions are within 2 pixels
		assertTrue( total >= 0.95*numViews*numFeatures );
	}

	private void setPerfectDepths(int numFeatures, ProjectiveReconstructionByFactorization alg) {
		double depths[] = new double[numFeatures];
		Point3D_F64 X = new Point3D_F64();
		for (int viewIdx = 0; viewIdx < projections.size(); viewIdx++) {
			for (int featIdx = 0; featIdx < features3D.size(); featIdx++) {
				worldToViews.get(viewIdx).transform(features3D.get(featIdx),X);
				depths[featIdx] = X.z;
			}
			alg.setDepths(viewIdx,depths);
		}
	}

	public void simulate( int numViews , int numFeatures ) {
		worldToViews = new ArrayList<>();
		projections = new ArrayList<>();
		observations = new ArrayList<>();

		// Randomly generate structure in front of the cameras
		features3D = UtilPoint3D_F64.random(new Point3D_F64(0,0,2),-0.5,0.5,numFeatures,rand);

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(pinhole,(DMatrixRMaj)null);

		// Generate views the adjust all 6-DOF but and distinctive while pointing at the points
		for (int i = 0; i < numViews; i++) {
			Se3_F64 worldToView = new Se3_F64();
			worldToView.T.x = -1 + 0.1*i;
			worldToView.T.y = rand.nextGaussian()*0.05;
			worldToView.T.z = -0.5 + 0.05*i + rand.nextGaussian()*0.01;

			double rotX = rand.nextGaussian()*0.1;
			double rotY = rand.nextGaussian()*0.1;
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX,rotY,0,worldToView.R);

			DMatrixRMaj P = new DMatrixRMaj(3,4);
			PerspectiveOps.createCameraMatrix(worldToView.R,worldToView.T,K,P);

			worldToViews.add(worldToView);
			projections.add(P);
		}

		// generate observations
		WorldToCameraToPixel w2p = new WorldToCameraToPixel();
		for (int i = 0; i < numViews; i++) {
			List<Point2D_F64> viewObs = new ArrayList<>();

			w2p.configure(pinhole,worldToViews.get(i));
			for (int j = 0; j < numFeatures; j++) {
				viewObs.add(w2p.transform(features3D.get(j)));
			}
			observations.add(viewObs);
		}
	}

}