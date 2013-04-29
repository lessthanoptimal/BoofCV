/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.h;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PairLineNorm;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class CommonHomographyInducedPlane {

	public DenseMatrix64F K = PerspectiveOps.calibrationMatrix(500, 520, 0.1, 400, 450);

	public Se3_F64 rightToLeft = new Se3_F64();
	public Se3_F64 leftToRight;

	public Point3D_F64 X1 = new Point3D_F64(1,0,5);
	public Point3D_F64 X2 = new Point3D_F64(1,1,5);
	public Point3D_F64 X3 = new Point3D_F64(2,-2,5);
	public Point3D_F64 X4 = new Point3D_F64(-1,3,5);

	public AssociatedPair p1,p2,p3,p4;

	public DenseMatrix64F E,F;

	// epipole in second image
	public Point3D_F64 e2 = new Point3D_F64();

	public CommonHomographyInducedPlane() {

		rightToLeft.getT().set(10,0,0);
		Se3_F64 leftToRight = rightToLeft.invert(null);

		p1 = render(leftToRight,K,X1);
		p2 = render(leftToRight,K,X2);
		p3 = render(leftToRight,K,X3);
		p4 = render(leftToRight,K,X4);


		E = MultiViewOps.createEssential(leftToRight.getR(), leftToRight.getT());
		F = MultiViewOps.createFundamental(E,K);

		MultiViewOps.extractEpipoles(F,new Point3D_F64(),e2);
	}

	public void checkHomography( DenseMatrix64F H ) {
		Point2D_F64 found = new Point2D_F64();
		GeometryMath_F64.mult(H,p4.p1,found);

		assertTrue(found.isIdentical(p4.p2, 1e-8));
	}

	public static AssociatedPair render( Se3_F64 leftToRight , DenseMatrix64F K , Point3D_F64 X1 ) {
		AssociatedPair ret = new AssociatedPair();

		ret.p1 = PerspectiveOps.renderPixel(new Se3_F64(),K,X1);
		ret.p2 = PerspectiveOps.renderPixel(leftToRight,K,X1);

		return ret;
	}

	public static PairLineNorm convert( AssociatedPair x1 , AssociatedPair x2  ) {
		PairLineNorm ret = new PairLineNorm();

		GeometryMath_F64.cross(x1.p1, x2.p1, ret.l1);
		GeometryMath_F64.cross(x1.p2,x2.p2,ret.l2);

		return ret;
	}
}
