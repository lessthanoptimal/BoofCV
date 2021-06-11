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

package boofcv.alg.geo.selfcalib;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GEquation;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CommonAutoCalibrationChecks extends BoofStandardJUnit {

	List<Point3D_F64> cloud = new ArrayList<>();

	List<Se3_F64> listCameraToWorld = new ArrayList<>();

	List<DMatrixRMaj> listP = new ArrayList<>();
	DMatrixRMaj planeAtInfinity = new DMatrixRMaj(3, 1); // plane at infinity [p';1]
	DMatrixRMaj Q;

	public void renderTranslationOnly( CameraPinhole camera ) {
		List<CameraPinhole> cameras = new ArrayList<>();
		for (int i = 0; i < 11; i++) {
			Se3_F64 R = new Se3_F64();
			Se3_F64 axis = new Se3_F64();
			axis.T.z = -2 + rand.nextGaussian()*0.01;
			axis.T.x = rand.nextGaussian()*0.4;
			axis.T.y = rand.nextGaussian()*0.4;

			Se3_F64 cameraToWorld = new Se3_F64();
			R.concat(axis, cameraToWorld);

			listCameraToWorld.add(cameraToWorld);
			cameras.add(camera);
		}

		render(cameras);
	}

	public void renderRotationOnly( CameraPinhole camera ) {
		List<CameraPinhole> cameras = new ArrayList<>();
		for (int i = 0; i < 11; i++) {
			double yaw = Math.PI*i/9.0;
			double pitch = Math.PI*i/20.0;
			double roll = rand.nextGaussian()*0.1;

			Se3_F64 cameraToWorld = new Se3_F64();
			ConvertRotation3D_F64.eulerToMatrix(EulerType.YZX, yaw, roll, pitch, cameraToWorld.R);

			listCameraToWorld.add(cameraToWorld);
			cameras.add(camera);
		}

		render(cameras);
	}

	public void renderRotateOneAxis( CameraPinhole camera ) {
		List<CameraPinhole> cameras = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			double pitch = Math.PI*i/20.0;

			Se3_F64 cameraToWorld = new Se3_F64();
			ConvertRotation3D_F64.eulerToMatrix(EulerType.YZX, 0, 0, pitch, cameraToWorld.R);

			listCameraToWorld.add(cameraToWorld);
			cameras.add(camera);
		}

		render(cameras);
	}

	public void renderRotateTwoAxis( CameraPinhole camera ) {
		List<CameraPinhole> cameras = new ArrayList<>();
		for (int i = 0; i < 11; i++) {
			double yaw = Math.PI*i/9.0;
			double pitch = Math.PI*i/20.0;

			Se3_F64 cameraToWorld = new Se3_F64();
			ConvertRotation3D_F64.eulerToMatrix(EulerType.YZX, yaw, 0, pitch, cameraToWorld.R);

			listCameraToWorld.add(cameraToWorld);
			cameras.add(camera);
		}

		render(cameras);
	}

	public void renderStationary( CameraPinhole camera ) {
		List<CameraPinhole> cameras = new ArrayList<>();
		for (int i = 0; i < 11; i++) {
			Se3_F64 cameraToWorld = new Se3_F64();
			listCameraToWorld.add(cameraToWorld);
			cameras.add(camera);
		}

		render(cameras);
	}

	public void renderGood( List<CameraPinhole> cameras ) {
		for (int i = 0; i < cameras.size(); i++) {
			double yaw = Math.PI*i/18.0;
			double pitch = Math.PI*i/30.0;
			double roll = rand.nextGaussian()*0.05;

			Se3_F64 R = new Se3_F64();
			ConvertRotation3D_F64.eulerToMatrix(EulerType.YZX, yaw, roll, pitch, R.R);
			Se3_F64 axis = new Se3_F64();
			axis.T.z = -2 + rand.nextGaussian()*0.01;
			axis.T.x = rand.nextGaussian()*0.1;
			axis.T.y = rand.nextGaussian()*0.1;

			Se3_F64 cameraToWorld = new Se3_F64();
			R.concat(axis, cameraToWorld);

			listCameraToWorld.add(cameraToWorld);
		}

		render(cameras);
	}

	private void render( List<CameraPinhole> cameras ) {
		cloud = UtilPoint3D_F64.random(-1, 1, 200, rand);

		// make camera[0] origin. probably not needed
		Se3_F64 a = listCameraToWorld.get(0).invert(null);
		for (int i = 0; i < cloud.size(); i++) {
			a.transform(cloud.get(i), cloud.get(i));
		}

		Se3_F64 tmp = new Se3_F64();
		for (int i = 0; i < listCameraToWorld.size(); i++) {
			a.concat(listCameraToWorld.get(i), tmp);
			listCameraToWorld.get(i).setTo(tmp);
		}

		// compute fundamental matrices
		GEquation eq = new GEquation();

		// Create a homography which will change the projections P from metric into a
		// more general projective transform
		{

			// random scale, positive or negative, that isn't zero
			double scale = (rand.nextDouble() + 0.1)*(rand.nextGaussian() < 0 ? -1 : 1);

			DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(cameras.get(0), (DMatrixRMaj)null);
			eq.alias(K, "K", planeAtInfinity, "p", scale, "scale");
			eq.process("p=[0.5,0.25,0.4]'");
			eq.process("H = scale*[K [0;0;0]; -p'*K 1]"); // projective to metric
			eq.process("Hinv = inv(H)"); // metric to projective
		}
//		eq.lookupDDRM("H").print();

		for (int i = 0; i < listCameraToWorld.size(); i++) {
			double scale = (rand.nextDouble() + 0.1)*(rand.nextGaussian() < 0 ? -1 : 1);
			DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(cameras.get(i), (DMatrixRMaj)null);

			Se3_F64 b_to_a = listCameraToWorld.get(i);

			eq.alias(K, "K", b_to_a.R, "R", b_to_a.T, "T", scale, "scale");
			DMatrixRMaj P = eq.process("P = scale*[K*R, K*T]*Hinv").lookupDDRM("P").copy();
//			DMatrixRMaj P = eq.process("P = [K*R, K*T]").lookupDDRM("P").copy();
			listP.add(P);
		}

		// Compute Q_inf from its definition. See 19.8 in the Multi View Geometry Book
		Q = eq.process("Q=H*diag([1 1 1 0])*H'").process("Q=Q/normF(Q)").lookupDDRM("Q");
//		System.out.println("---------Q");
//		Q.print();
	}

	public void addProjectives( SelfCalibrationBase alg ) {
		for (int i = 0; i < listP.size(); i++) {
			alg.addCameraMatrix(listP.get(i));
		}
	}
}
