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

package boofcv.alg.geo;

import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPerspectiveOps {

	Random rand = new Random(234);

	@Test
	public void guessIntrinsic_two() {

		double hfov = 30;
		double vfov = 35;

		CameraPinhole found = PerspectiveOps.createIntrinsic(640, 480, hfov, vfov);

		assertEquals(UtilAngle.degreeToRadian(hfov),2.0*Math.atan(found.cx/found.fx),1e-6);
		assertEquals(UtilAngle.degreeToRadian(vfov),2.0*Math.atan(found.cy/found.fy),1e-6);
	}

	@Test
	public void guessIntrinsic_one() {

		double hfov = 30;

		CameraPinholeRadial found = PerspectiveOps.createIntrinsic(640, 480, hfov);

		assertEquals(UtilAngle.degreeToRadian(hfov),2.0*Math.atan(found.cx/found.fx),1e-6);
		assertEquals(found.fx,found.fy,1e-6);
	}

	@Test
	public void scaleIntrinsic() {
		Point3D_F64 X = new Point3D_F64(0.1,0.3,2);

		CameraPinholeRadial param = new CameraPinholeRadial(200,300,2,250,260,200,300);
		DMatrixRMaj K = PerspectiveOps.calibrationMatrix(param, (DMatrixRMaj)null);

		// find the pixel location in the unscaled image
		Point2D_F64 a = PerspectiveOps.renderPixel(new Se3_F64(),K,X);

		PerspectiveOps.scaleIntrinsic(param,0.5);
		K = PerspectiveOps.calibrationMatrix(param,(DMatrixRMaj)null);

		// find the pixel location in the scaled image
		Point2D_F64 b = PerspectiveOps.renderPixel(new Se3_F64(),K,X);

		assertEquals(a.x*0.5,b.x,1e-8);
		assertEquals(a.y*0.5,b.y,1e-8);
	}

	@Test
	public void adjustIntrinsic() {

		DMatrixRMaj B = new DMatrixRMaj(3,3,true,2,0,1,0,3,2,0,0,1);

		CameraPinholeRadial param = new CameraPinholeRadial(200,300,2,250,260,200,300).fsetRadial(0.1,0.3);
		CameraPinholeRadial found = PerspectiveOps.adjustIntrinsic(param, B, null);

		DMatrixRMaj A = PerspectiveOps.calibrationMatrix(param, (DMatrixRMaj)null);

		DMatrixRMaj expected = new DMatrixRMaj(3,3);
		CommonOps_DDRM.mult(B, A, expected);

		assertArrayEquals(param.radial, found.radial, 1e-8);
		DMatrixRMaj foundM = PerspectiveOps.calibrationMatrix(found,(DMatrixRMaj)null);

		assertTrue(MatrixFeatures_DDRM.isIdentical(expected,foundM,1e-8));
	}

	@Test
	public void calibrationMatrix() {
		DMatrixRMaj K = PerspectiveOps.calibrationMatrix(1.0, 2, 3, 4, 5);

		assertEquals(1,K.get(0,0),1e-3);
		assertEquals(2,K.get(1,1),1e-3);
		assertEquals(3,K.get(0,1),1e-3);
		assertEquals(4,K.get(0,2),1e-3);
		assertEquals(5,K.get(1,2),1e-3);
		assertEquals(1,K.get(2,2),1e-3);
	}

	@Test
	public void matrixToParam() {
		double fx = 1;
		double fy = 2;
		double skew = 3;
		double cx = 4;
		double cy = 5;

		DMatrixRMaj K = new DMatrixRMaj(3,3,true,fx,skew,cx,0,fy,cy,0,0,1);
		CameraPinhole ret = PerspectiveOps.matrixToParam(K, 100, 200, null);

		assertTrue(ret.fx == fx);
		assertTrue(ret.fy == fy);
		assertTrue(ret.skew == skew);
		assertTrue(ret.cx == cx);
		assertTrue(ret.cy == cy);
		assertTrue(ret.width == 100);
		assertTrue(ret.height == 200);
	}

	@Test
	public void convertNormToPixel_intrinsic_F64() {
		CameraPinholeRadial intrinsic = new CameraPinholeRadial(100,150,0.1,120,209,500,600);

		DMatrixRMaj K = PerspectiveOps.calibrationMatrix(intrinsic, (DMatrixRMaj)null);

		Point2D_F64 norm = new Point2D_F64(-0.1,0.25);
		Point2D_F64 expected = new Point2D_F64();

		GeometryMath_F64.mult(K, norm, expected);

		Point2D_F64 found = PerspectiveOps.convertNormToPixel(intrinsic,norm.x,norm.y,null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test
	public void convertNormToPixel_matrix() {
		DMatrixRMaj K = new DMatrixRMaj(3,3,true,100,0.1,120,0,150,209,0,0,1);

		Point2D_F64 norm = new Point2D_F64(-0.1,0.25);
		Point2D_F64 expected = new Point2D_F64();

		GeometryMath_F64.mult(K,norm,expected);

		Point2D_F64 found = PerspectiveOps.convertNormToPixel(K,norm,null);

		assertEquals(expected.x,found.x,1e-8);
		assertEquals(expected.y,found.y,1e-8);
	}

	@Test
	public void convertPixelToNorm_intrinsic_F64() {
		CameraPinholeRadial intrinsic = new CameraPinholeRadial(100,150,0.1,120,209,500,600);

		DMatrixRMaj K = PerspectiveOps.calibrationMatrix(intrinsic, (DMatrixRMaj)null);
		DMatrixRMaj K_inv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(K,K_inv);

		Point2D_F64 pixel = new Point2D_F64(100,120);
		Point2D_F64 expected = new Point2D_F64();

		GeometryMath_F64.mult(K_inv,pixel,expected);

		Point2D_F64 found = PerspectiveOps.convertPixelToNorm(intrinsic, pixel, null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test
	public void convertPixelToNorm_matrix() {
		DMatrixRMaj K = new DMatrixRMaj(3,3,true,100,0.1,120,0,150,209,0,0,1);
		DMatrixRMaj K_inv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(K,K_inv);

		Point2D_F64 pixel = new Point2D_F64(100,120);
		Point2D_F64 expected = new Point2D_F64();

		GeometryMath_F64.mult(K_inv,pixel,expected);

		Point2D_F64 found = PerspectiveOps.convertPixelToNorm(K,pixel,null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test
	public void renderPixel_SE() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		Se3_F64 worldToCamera = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1, -0.05, 0.03, worldToCamera.getR());
		worldToCamera.getT().set(0.2,0.01,-0.03);

		DMatrixRMaj K = RandomMatrices_DDRM.triangularUpper(3, 0, -1, 1, rand);

		Point3D_F64 X_cam = SePointOps_F64.transform(worldToCamera,X,null);
		Point2D_F64 found;

		// calibrated case
		found = PerspectiveOps.renderPixel(worldToCamera,null,X);
		assertEquals(X_cam.x/X_cam.z,found.x,1e-8);
		assertEquals(X_cam.y/X_cam.z,found.y,1e-8);

		// uncalibrated case
		Point2D_F64 expected = new Point2D_F64();
		expected.x = X_cam.x/X_cam.z;
		expected.y = X_cam.y/X_cam.z;
		GeometryMath_F64.mult(K,expected,expected);

		found = PerspectiveOps.renderPixel(worldToCamera,K,X);
		assertEquals(expected.x,found.x,1e-8);
		assertEquals(expected.y,found.y,1e-8);
	}

	@Test
	public void renderPixel_intrinsic() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		CameraPinholeRadial intrinsic = new CameraPinholeRadial(100,150,0.1,120,209,500,600);

		double normX = X.x/X.z;
		double normY = X.y/X.z;

		Point2D_F64 expected = new Point2D_F64();
		PerspectiveOps.convertNormToPixel(intrinsic,normX,normY,expected);

		Point2D_F64 found = PerspectiveOps.renderPixel(intrinsic,X);

		assertEquals(expected.x,found.x,1e-8);
		assertEquals(expected.y,found.y,1e-8);
	}

	@Test
	public void renderPixel_cameramatrix() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		Se3_F64 worldToCamera = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1,-0.05,0.03,worldToCamera.getR());
		worldToCamera.getT().set(0.2,0.01,-0.03);

		DMatrixRMaj K = RandomMatrices_DDRM.triangularUpper(3, 0, -1, 1, rand);

		Point3D_F64 X_cam = SePointOps_F64.transform(worldToCamera,X,null);
		Point2D_F64 found;

		DMatrixRMaj P = PerspectiveOps.createCameraMatrix(worldToCamera.R,worldToCamera.T,K,null);

		Point2D_F64 expected = new Point2D_F64();
		expected.x = X_cam.x/X_cam.z;
		expected.y = X_cam.y/X_cam.z;
		GeometryMath_F64.mult(K,expected,expected);

		found = PerspectiveOps.renderPixel(P,X);
		assertEquals(expected.x,found.x,1e-8);
		assertEquals(expected.y,found.y,1e-8);
	}

	@Test
	public void splitAssociated_pair() {
		List<AssociatedPair> list = new ArrayList<>();
		for( int i = 0; i < 12; i++ ) {
			AssociatedPair p = new AssociatedPair();

			p.p2.set(rand.nextDouble()*5, rand.nextDouble()*5);
			p.p1.set(rand.nextDouble()*5, rand.nextDouble()*5);

			list.add(p);
		}

		List<Point2D_F64> list1 = new ArrayList<>();
		List<Point2D_F64> list2 = new ArrayList<>();

		PerspectiveOps.splitAssociated(list,list1,list2);

		assertEquals(list.size(),list1.size());
		assertEquals(list.size(),list2.size());

		for( int i = 0; i < list.size(); i++ ) {
			assertTrue(list.get(i).p1 == list1.get(i));
			assertTrue(list.get(i).p2 == list2.get(i));
		}
	}

	@Test
	public void splitAssociated_triple() {
		List<AssociatedTriple> list = new ArrayList<>();
		for( int i = 0; i < 12; i++ ) {
			AssociatedTriple p = new AssociatedTriple();

			p.p1.set(rand.nextDouble()*5,rand.nextDouble()*5);
			p.p2.set(rand.nextDouble() * 5, rand.nextDouble() * 5);
			p.p3.set(rand.nextDouble() * 5, rand.nextDouble() * 5);

			list.add(p);
		}

		List<Point2D_F64> list1 = new ArrayList<>();
		List<Point2D_F64> list2 = new ArrayList<>();
		List<Point2D_F64> list3 = new ArrayList<>();

		PerspectiveOps.splitAssociated(list,list1,list2,list3);

		assertEquals(list.size(),list1.size());
		assertEquals(list.size(),list2.size());
		assertEquals(list.size(),list3.size());

		for( int i = 0; i < list.size(); i++ ) {
			assertTrue(list.get(i).p1 == list1.get(i));
			assertTrue(list.get(i).p2 == list2.get(i));
			assertTrue(list.get(i).p3 == list3.get(i));
		}
	}

	@Test
	public void createCameraMatrix() {
		SimpleMatrix R = SimpleMatrix.random_DDRM(3, 3, -1, 1, rand);
		Vector3D_F64 T = new Vector3D_F64(2,3,-4);
		SimpleMatrix K = SimpleMatrix.wrap(RandomMatrices_DDRM.triangularUpper(3, 0, -1, 1, rand));

		SimpleMatrix T_ = new SimpleMatrix(3,1,true,new double[]{T.x,T.y,T.z});

		// test calibrated camera
		DMatrixRMaj found = PerspectiveOps.createCameraMatrix(R.getDDRM(), T, null, null);
		for( int i = 0; i < 3; i++ ) {
			assertEquals(found.get(i,3),T_.get(i),1e-8);
			for( int j = 0; j < 3; j++ ) {
				assertEquals(found.get(i,j),R.get(i,j),1e-8);
			}
		}

		// test uncalibrated camera
		found = PerspectiveOps.createCameraMatrix(R.getDDRM(), T, K.getDDRM(), null);

		SimpleMatrix expectedR = K.mult(R);
		SimpleMatrix expectedT = K.mult(T_);

		for( int i = 0; i < 3; i++ ) {
			assertEquals(found.get(i,3),expectedT.get(i),1e-8);
			for( int j = 0; j < 3; j++ ) {
				assertEquals(found.get(i,j),expectedR.get(i,j),1e-8);
			}
		}
	}
}
