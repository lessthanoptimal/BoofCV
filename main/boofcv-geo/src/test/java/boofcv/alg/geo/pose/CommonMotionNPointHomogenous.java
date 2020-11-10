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
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.h.CommonHomographyChecks;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;

/**
 * Common testing code for algorithms which estimate motion from a set of associated observations
 * and known 3D coordinates.
 *
 * @author Peter Abeles
 */
public class CommonMotionNPointHomogenous extends BoofStandardJUnit {

	// Projection matrix 3x4
	protected DMatrixRMaj projection;
	// list of points in world reference frame
	protected List<Point4D_F64> worldPts;
	// list of observation pairs in both reference frames
	protected List<AssociatedPair> assocPairs;
	protected List<Point2D_F64> pixelsView2 = new ArrayList<>();

	protected void generateScene(int N, DMatrixRMaj P, boolean planar) {
		this.projection = P;

		// randomly generate points in space
		if( planar ) {
			worldPts = CommonHomographyChecks.createRandomPlaneH(rand, 3, N);
		} else {
			worldPts = GeoTestingOps.randomPointsH_F64(-1, 1, N, rand);
		}

		DMatrixRMaj P0 = new DMatrixRMaj(3,4);
		CommonOps_DDRM.setIdentity(P0);

		// transform points into second camera's reference frame
		assocPairs = new ArrayList<>();
		pixelsView2 = new ArrayList<>();
		for(Point4D_F64 X : worldPts ) {
			Point2D_F64 p1 = PerspectiveOps.renderPixel(P0, X, (Point2D_F64)null);
			Point2D_F64 p2 = PerspectiveOps.renderPixel(P, X, (Point2D_F64)null);

			AssociatedPair pair = new AssociatedPair();
			pair.p1.setTo(p1.x,p1.y);
			pair.p2.setTo(p2.x,p2.y);
			assocPairs.add(pair);
			pixelsView2.add(p2);
		}
	}
}
