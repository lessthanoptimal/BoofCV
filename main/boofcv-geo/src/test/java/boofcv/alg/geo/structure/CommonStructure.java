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

package boofcv.alg.geo.structure;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class CommonStructure extends BoofStandardJUnit {
	protected CameraPinhole pinhole = new CameraPinhole(400, 420, 0.1, 500, 490, -1, -1);

	protected List<Point3D_F64> features3D;
	protected List<Se3_F64> worldToViews;
	protected List<DMatrixRMaj> projections;
	protected List<List<Point2D_F64>> observations;

	public void simulate( int numViews, int numFeatures, boolean planar ) {
		worldToViews = new ArrayList<>();
		projections = new ArrayList<>();
		observations = new ArrayList<>();

		// Randomly generate structure in front of the cameras
		if (planar) {
			PlaneNormal3D_F64 plane = new PlaneNormal3D_F64(0, 0, 2, 0.1, -0.05, 1);
			plane.n.normalize();
			features3D = UtilPoint3D_F64.random(plane, 0.5, numFeatures, rand);
		} else {
			features3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -0.5, 0.5, numFeatures, rand);
		}

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(pinhole, (DMatrixRMaj)null);

		// Generate views the adjust all 6-DOF but and distinctive while pointing at the points
		for (int i = 0; i < numViews; i++) {
			Se3_F64 worldToView = new Se3_F64();
			worldToView.T.x = -1 + 0.1*i;
			worldToView.T.y = rand.nextGaussian()*0.05;
			worldToView.T.z = -0.5 + 0.05*i + rand.nextGaussian()*0.01;

			double rotX = rand.nextGaussian()*0.05;
			double rotY = rand.nextGaussian()*0.05;
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, rotX, rotY, 0, worldToView.R);

			DMatrixRMaj P = new DMatrixRMaj(3, 4);
			PerspectiveOps.createCameraMatrix(worldToView.R, worldToView.T, K, P);

			worldToViews.add(worldToView);
			projections.add(P);
		}

		// generate observations
		WorldToCameraToPixel w2p = new WorldToCameraToPixel();
		for (int i = 0; i < numViews; i++) {
			List<Point2D_F64> viewObs = new ArrayList<>();

			w2p.configure(pinhole, worldToViews.get(i));
			for (int j = 0; j < numFeatures; j++) {
				viewObs.add(w2p.transform(features3D.get(j)));
			}
			observations.add(viewObs);
		}
	}

	public List<List<PointIndex2D_F64>> convertToPointIndex() {
		List<List<PointIndex2D_F64>> ret = new ArrayList<>();

		for (int i = 0; i < observations.size(); i++) {
			List<Point2D_F64> view = observations.get(i);

			List<PointIndex2D_F64> indexes = new ArrayList<>();

			for (int j = 0; j < view.size(); j++) {
				Point2D_F64 p = view.get(j);
				indexes.add(new PointIndex2D_F64(p.x, p.y, j));
			}

			// order shouldn't matter
			Collections.shuffle(indexes, rand);

			ret.add(indexes);
		}

		return ret;
	}

	public List<DMatrixRMaj> computeHomographies() {
		List<DMatrixRMaj> ret = new ArrayList<>();

		Estimate1ofEpipolar estimateH = FactoryMultiView.homographyDLT(true);

		Point2D_F64 sanity = new Point2D_F64();

		for (int i = 0; i < observations.size(); i++) {
			List<Point2D_F64> view0 = observations.get(0);
			List<Point2D_F64> viewI = observations.get(i);

			List<AssociatedPair> matches = new ArrayList<>();

			for (int j = 0; j < view0.size(); j++) {
				matches.add(new AssociatedPair(view0.get(j), viewI.get(j)));
			}

			DMatrixRMaj H = new DMatrixRMaj(3, 3);
			if (!estimateH.process(matches, H))
				throw new RuntimeException("EGads");
			ret.add(H);

			// make sure the homography is from view 0 to view i
			GeometryMath_F64.mult(H, view0.get(0), sanity);
			double error = viewI.get(0).distance(sanity);
			assertEquals(0, error, UtilEjml.TEST_F64);
		}

		return ret;
	}
}
