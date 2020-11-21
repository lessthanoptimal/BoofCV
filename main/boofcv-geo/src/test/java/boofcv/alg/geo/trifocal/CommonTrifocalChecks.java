/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.trifocal;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.NormOps_DDRM;

import java.util.ArrayList;
import java.util.List;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common data structures used when checking trifocal tensor algorithms
 *
 * @author Peter Abeles
 */
public abstract class CommonTrifocalChecks extends BoofStandardJUnit {

	// camera calibration matrix
	protected DMatrixRMaj K = new DMatrixRMaj(3,3,true,60,0.01,200,0,80,150,0,0,1);

	protected Se3_F64 worldToCam1,worldToCam2, worldToCam3;
	protected DMatrixRMaj P1_k;
	protected DMatrixRMaj P2,P3;
	protected TrifocalTensor tensor;
	protected TrifocalTensor tensorPixels;
	// storage for the found solution
	protected TrifocalTensor found = new TrifocalTensor();

	protected DMatrixRMaj F2,F3;

	protected List<Point3D_F64> worldPts = new ArrayList<>();
	// Observation 1 is normalized and 2 and 3 are pixels. This reflects p1=[I|0]
	protected List<AssociatedTriple> observations = new ArrayList<>();
	// All observations in pixels
	protected List<AssociatedTriple> observationsPixels = new ArrayList<>();
	// All observations are in normalized image coordinates
	protected List<AssociatedTriple> observationsNorm = new ArrayList<>();

	protected int numFeatures = 20;

	protected CommonTrifocalChecks() {
		worldToCam1 = new Se3_F64();
		worldToCam2 = eulerXyz(0.3,0,0.05,0.05, 0.05, -0.02,null);
		worldToCam3 = eulerXyz(0.6, 0.2, -0.02,0.1,-0.2,0.05,null);

		createSceneObservations(false);
	}

	public void checkTrifocalWithConstraint( TrifocalTensor tensor , double tol ) {
		for( int i = 0; i < observations.size(); i++ ) {
			AssociatedTriple o = observations.get(i);

			DMatrixRMaj c = MultiViewOps.constraint(tensor,o.p1,o.p2,o.p3,null);

			double v = NormOps_DDRM.normF(c)/(c.numCols*c.numRows);

			assertEquals(0,v,tol);
		}
	}

	public void createRandomScenario( boolean planar ) {
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
				rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, -rand.nextGaussian()*0.1, worldToCam2.R);
		worldToCam2.getT().setTo(0.3,0,0.05);

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
				rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, -rand.nextGaussian()*0.1, worldToCam3.R);
		worldToCam3.getT().setTo(0.6, 0.2, -0.02);

		createSceneObservations(planar);
	}

	void createSceneObservations(boolean planar ) {
		P1_k = PerspectiveOps.createCameraMatrix(worldToCam1.R, worldToCam1.T, K, null);
		// P1 = [I|0]
		P2 = PerspectiveOps.createCameraMatrix(worldToCam2.R, worldToCam2.T, K, null);
		P3 = PerspectiveOps.createCameraMatrix(worldToCam3.R, worldToCam3.T, K, null);
		tensor = MultiViewOps.createTrifocal(P2, P3, null);
		tensorPixels = MultiViewOps.createTrifocal(P1_k,P2,P3,null);

		F2 = MultiViewOps.createEssential(worldToCam2.getR(), worldToCam2.getT(), null);
		F2 = MultiViewOps.createFundamental(F2, K);
		F3 = MultiViewOps.createEssential(worldToCam3.getR(), worldToCam3.getT(), null);
		F3 = MultiViewOps.createFundamental(F3, K);

		observations = new ArrayList<>();
		observationsPixels = new ArrayList<>();
		observationsNorm = new ArrayList<>();


		for(int i = 0; i < numFeatures; i++ ) {
			Point3D_F64 p = new Point3D_F64();
			p.x = rand.nextGaussian()*0.5;
			p.y = rand.nextGaussian()*0.5;
			if( planar ) {
				p.z = 2;
			} else {
				p.z = rand.nextGaussian() * 0.5 + 2;
			}
			worldPts.add(p);

			AssociatedTriple o = new AssociatedTriple();
			o.p1 = PerspectiveOps.renderPixel(new Se3_F64(), p, null);
			o.p2 = PerspectiveOps.renderPixel(P2,p);
			o.p3 = PerspectiveOps.renderPixel(P3,p);

			AssociatedTriple oP = new AssociatedTriple();
			oP.p1 = PerspectiveOps.renderPixel(new Se3_F64(), K, p, null);
			oP.p2 = PerspectiveOps.renderPixel(P2,p);
			oP.p3 = PerspectiveOps.renderPixel(P3,p);

			AssociatedTriple oN = new AssociatedTriple();
			oN.p1 = PerspectiveOps.renderPixel(new Se3_F64(), p, null);
			oN.p2 = PerspectiveOps.renderPixel(worldToCam2,p, null);
			oN.p3 = PerspectiveOps.renderPixel(worldToCam3,p, null);

			observations.add(o);
			observationsPixels.add(oP);
			observationsNorm.add(oN);
		}
	}
}
