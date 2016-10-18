/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.NormOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Common data structures used when checking trifocal tensor algorithms
 *
 * @author Peter Abeles
 */
public abstract class CommonTrifocalChecks {
	Random rand = new Random(234);

	// camera calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,200,0,80,150,0,0,1);

	Se3_F64 se2,se3;
	DenseMatrix64F P2,P3;
	TrifocalTensor tensor;
	// storage for the found solution
	TrifocalTensor found = new TrifocalTensor();

	DenseMatrix64F F2,F3;

	List<Point3D_F64> worldPts = new ArrayList<>();
	// observation in pixels for all views
	List<AssociatedTriple> observations = new ArrayList<>();
	// observations where the first view is in normalized image coordinates
	List<AssociatedTriple> observationsSpecial = new ArrayList<>();
	// All observations are in normalized image coordinates
	List<AssociatedTriple> observationsNorm = new ArrayList<>();

	public CommonTrifocalChecks() {
		se2 = new Se3_F64();
		se3 = new Se3_F64();

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.05, 0.05, -0.02, se2.R);
		se2.getT().set(0.3,0,0.05);

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1,-0.2,0.05,se3.R);
		se3.getT().set(0.6, 0.2, -0.02);

		computeStuffFromPose();

	}

	public void checkTrifocalWithConstraint( TrifocalTensor tensor , double tol ) {
		for( int i = 0; i < observations.size(); i++ ) {
			AssociatedTriple o = observations.get(i);

			DenseMatrix64F c = MultiViewOps.constraint(tensor,o.p1,o.p2,o.p3,null);

			double v = NormOps.normF(c)/(c.numCols*c.numRows);

			assertEquals(0,v,tol);
		}
	}

	public void createRandomScenario() {
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
				rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, -rand.nextGaussian()*0.1, se2.R);
		se2.getT().set(0.3,0,0.05);

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
				rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, -rand.nextGaussian()*0.1, se3.R);
		se3.getT().set(0.6, 0.2, -0.02);

		computeStuffFromPose();
	}

	private void computeStuffFromPose() {
		P2 = PerspectiveOps.createCameraMatrix(se2.R, se2.T, K, null);
		P3 = PerspectiveOps.createCameraMatrix(se3.R, se3.T, K, null);
		tensor = MultiViewOps.createTrifocal(P2, P3, null);

		F2 = MultiViewOps.createEssential(se2.getR(), se2.getT());
		F2 = MultiViewOps.createFundamental(F2, K);
		F3 = MultiViewOps.createEssential(se3.getR(), se3.getT());
		F3 = MultiViewOps.createFundamental(F3, K);

		for( int i = 0; i < 20; i++ ) {
			Point3D_F64 p = new Point3D_F64();
			p.x = rand.nextGaussian()*0.5;
			p.y = rand.nextGaussian()*0.5;
			p.z = rand.nextGaussian()*0.5 + 2;

			worldPts.add(p);

			AssociatedTriple o = new AssociatedTriple();
			o.p1 = PerspectiveOps.renderPixel(new Se3_F64(), K, p);
			o.p2 = PerspectiveOps.renderPixel(se2,K,p);
			o.p3 = PerspectiveOps.renderPixel(se3,K,p);

			AssociatedTriple oS = o.copy();
			oS.p1 = PerspectiveOps.renderPixel(new Se3_F64(), null, p);

			AssociatedTriple oN = new AssociatedTriple();
			oN.p1 = PerspectiveOps.renderPixel(new Se3_F64(), null, p);
			oN.p2 = PerspectiveOps.renderPixel(se2,null,p);
			oN.p3 = PerspectiveOps.renderPixel(se3,null,p);

			observations.add(o);
			observationsSpecial.add(oS);
			observationsNorm.add(oN);
		}
	}
}
