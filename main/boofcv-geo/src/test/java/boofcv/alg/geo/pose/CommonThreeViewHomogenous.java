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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.h.CommonHomographyChecks;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public abstract class CommonThreeViewHomogenous extends BoofStandardJUnit {
	protected DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(
			new CameraPinhole(500,500,0,250,250,1000,1000),(DMatrixRMaj)null);

	// Projection matrix 3x4
	protected List<DMatrixRMaj> cameras = new ArrayList<>();
	// fundamental matrices from view i to view 0
	protected List<DMatrixRMaj> fundamentals = new ArrayList<>();
	// list of points in world reference frame
	protected List<Point4D_F64> worldPts;
	// pixel observations
	protected List<Point2D_F64>[] pixelsInView = new ArrayList[3];
	protected List<AssociatedTriple> triples = new ArrayList<>();

	public void createScene( int numFeatures , boolean planar ) {
		// randomly generate points in space
		if( planar ) {
			worldPts = CommonHomographyChecks.createRandomPlaneH(rand, 3, numFeatures);
		} else {
			worldPts = GeoTestingOps.randomPointsH_F64(-1, 1,-1,1,2,4, numFeatures, rand);
		}

		Se3_F64 motion_0_to_0 = new Se3_F64();
		Se3_F64 motion_0_to_1 = SpecialEuclideanOps_F64.eulerXyz(0.01,0.5,0.2,0.1,-0.02,0.015,null);
		Se3_F64 motion_0_to_2 = SpecialEuclideanOps_F64.eulerXyz(0.05,0.03,0.53,0.02,0.1,-0.01,null);

		DMatrixRMaj P0 = PerspectiveOps.createCameraMatrix(motion_0_to_0.R,motion_0_to_0.T,K,null);
		DMatrixRMaj P1 = PerspectiveOps.createCameraMatrix(motion_0_to_1.R,motion_0_to_1.T,K,null);
		DMatrixRMaj P2 = PerspectiveOps.createCameraMatrix(motion_0_to_2.R,motion_0_to_2.T,K,null);

		cameras.clear();
		cameras.add(P0);
		cameras.add(P1);
		cameras.add(P2);
		fundamentals.clear();
		fundamentals.add(MultiViewOps.createFundamental(motion_0_to_1.R,motion_0_to_1.T,K,K,null));
		fundamentals.add(MultiViewOps.createFundamental(motion_0_to_2.R,motion_0_to_2.T,K,K,null));

		triples.clear();
		pixelsInView[0] = new ArrayList<>();
		pixelsInView[1] = new ArrayList<>();
		pixelsInView[2] = new ArrayList<>();

		Point2D_F64 x0 = new Point2D_F64();
		Point2D_F64 x1 = new Point2D_F64();
		Point2D_F64 x2 = new Point2D_F64();
		for (int i = 0; i < numFeatures; i++) {
			Point4D_F64 X = worldPts.get(i);
			PerspectiveOps.renderPixel(P0,X,x0);
			PerspectiveOps.renderPixel(P1,X,x1);
			PerspectiveOps.renderPixel(P2,X,x2);
			pixelsInView[0].add(x0.copy());
			pixelsInView[1].add(x1.copy());
			pixelsInView[2].add(x2.copy());

			AssociatedTriple t = new AssociatedTriple();
			t.setTo(x0,x1,x2);
			triples.add(t);
		}
	}
}
