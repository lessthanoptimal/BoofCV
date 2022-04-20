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

package boofcv.alg.geo.triangulate;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CommonTriangulationChecks extends BoofStandardJUnit {

	int N = 30;

	protected CameraPinhole intrinsic = new CameraPinhole(400, 410, 0, 500, 505, 1000, 1100);
	protected DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic, (DMatrixRMaj)null);

	protected Point3D_F64 worldPoint = new Point3D_F64();
	protected Point4D_F64 worldPointH;
	protected List<Point2D_F64> obsPixels;
	protected List<Point2D_F64> obsNorm;
	protected List<Point3D_F64> obsPointing;
	protected List<Se3_F64> motionWorldToCamera;
	protected List<DMatrixRMaj> essential;
	protected List<DMatrixRMaj> fundamental;
	protected List<DMatrixRMaj> cameraMatrices;

	public static Point3D_F64 normTo3D( Point2D_F64 a ) {
		return new Point3D_F64(a.x, a.y, 1);
	}

	public void createScene() {
		createScene(new Point3D_F64(0.1, -0.2, 4));
	}

	public void createScene( Point3D_F64 worldPoint ) {
		Point4D_F64 X = new Point4D_F64(worldPoint.x, worldPoint.y, worldPoint.z, 1.0);
		createScene(X);
	}

	public void createScene( Point4D_F64 point ) {
		worldPointH = point.copy();
		PerspectiveOps.homogenousTo3dPositiveZ(point, Double.MAX_VALUE, 1e-16, worldPoint);
		motionWorldToCamera = new ArrayList<>();
		obsNorm = new ArrayList<>();
		obsPointing = new ArrayList<>();
		obsPixels = new ArrayList<>();
		essential = new ArrayList<>();
		fundamental = new ArrayList<>();
		cameraMatrices = new ArrayList<>();

		CameraPinhole dummyIntrinsic = new CameraPinhole(1.0, 1.0, 0.0, 0.0, 0.0, 0, 0);

		for (int i = 0; i < N; i++) {
			// random motion from world to frame 'i'
			Se3_F64 world_to_view = new Se3_F64();
			if (i > 0) {
				world_to_view.getR().setTo(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
						rand.nextGaussian()*0.01, rand.nextGaussian()*0.05, rand.nextGaussian()*0.1, null));
				world_to_view.getT().setTo(0.2 + rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, rand.nextGaussian()*0.01);
			}

			DMatrixRMaj E = MultiViewOps.createEssential(world_to_view.getR(), world_to_view.getT(), null);
			DMatrixRMaj F = MultiViewOps.createFundamental(E, intrinsic);

			Point2D_F64 norm = PerspectiveOps.renderPixel(world_to_view, dummyIntrinsic, worldPointH, null);
			Point2D_F64 pixel = PerspectiveOps.renderPixel(world_to_view, intrinsic, worldPointH, null);

			obsNorm.add(norm);
			obsPointing.add(normTo3D(norm));
			obsPixels.add(pixel);
			motionWorldToCamera.add(world_to_view);
			essential.add(E);
			fundamental.add(F);
			cameraMatrices.add(PerspectiveOps.createCameraMatrix(world_to_view.R, world_to_view.T, K, null));
		}
	}

	protected Point3D_F64 convertH( Point4D_F64 X ) {
		return new Point3D_F64(X.x/X.w, X.y/X.w, X.z/X.w);
	}

	protected Point4D_F64 convertH( Point3D_F64 X ) {
		double scale = rand.nextGaussian();
		if (Math.abs(scale) < 1e-5)
			scale = 0.001;
		Point4D_F64 P = new Point4D_F64();
		P.x = X.x*scale;
		P.y = X.y*scale;
		P.z = X.z*scale;
		P.w = scale;
		return P;
	}
}
