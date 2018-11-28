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

package boofcv.alg.geo.structure;

import boofcv.alg.geo.PerspectiveOps;
import georegression.geometry.UtilPoint2D_F64;
import georegression.geometry.UtilPoint4D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestRefineProjectiveStructureAllVisible {

	private Random rand = new Random(234);

	/**
	 * Add noise to landmarks but keep everything else perfect. See if it can recover
	 */
	@Test
	public void littleBitOfNoise() {
		RefineProjectiveStructureAllVisible alg = new RefineProjectiveStructureAllVisible();

		List<Point4D_F64> landmarks = UtilPoint4D_F64.random(-1,1,30,rand);
		List<Point2D_F64> obs0 = new ArrayList<>();
		List<Point2D_F64> obs1 = new ArrayList<>();

		DMatrixRMaj P0 = new DMatrixRMaj(new double[][]{{0,1,2,3},{4,5,6,7},{8,9,0,1}});
		DMatrixRMaj P1 = new DMatrixRMaj(new double[][]{{9,8,7,6},{5,4,3,2},{1,0,1,2}});

		for (int i = 0; i < landmarks.size(); i++) {
			obs0.add(PerspectiveOps.renderPixel(P0,landmarks.get(i),(Point2D_F64)null));
			obs1.add(PerspectiveOps.renderPixel(P1,landmarks.get(i),(Point2D_F64)null));

			landmarks.get(i).x += rand.nextGaussian()*0.1;
			landmarks.get(i).y += rand.nextGaussian()*0.1;
			landmarks.get(i).z += rand.nextGaussian()*0.1;
		}

		// one fixed and one not fixed to test if that is handled correctly
		alg.addView(false, P0.copy() , obs0);
		alg.addView(true , P1.copy() , obs1);

		alg.setLandmarks(landmarks);

//		alg.setVerbose(System.out,0);
		assertTrue(alg.process());

		assertEquals(0,alg.getErrorScore(), UtilEjml.TEST_F64);

		assertTrue(MatrixFeatures_DDRM.isIdentical(P0,alg.getView(0), UtilEjml.TEST_F64));
		assertTrue(MatrixFeatures_DDRM.isIdentical(P1,alg.getView(1), UtilEjml.TEST_F64));
	}

	@Test
	public void encode() {
		RefineProjectiveStructureAllVisible alg = new RefineProjectiveStructureAllVisible();

		List<Point2D_F64> obs = UtilPoint2D_F64.random(-1,1,3,rand);
		List<Point4D_F64> landmarks = UtilPoint4D_F64.random(-1,1,3,rand);

		DMatrixRMaj P0 = new DMatrixRMaj(new double[][]{{0,1,2,3},{4,5,6,7},{8,9,0,1}});
		DMatrixRMaj P1 = new DMatrixRMaj(new double[][]{{9,8,7,6},{5,4,3,2},{1,0,1,2}});

		// one fixed and one not fixed to test if that is handled correctly
		alg.addView(false, P0 , obs);
		alg.addView(true , P1 , obs);

		alg.setLandmarks(landmarks);

		double parameters[] = new double[alg.computeNumberOfParameters()];
		alg.encode(parameters);

		// see if computeNumberOfParameters was correct
		assertEquals(12+4*3, parameters.length);

		for (int i = 0; i < 12; i++) {
			assertEquals(P0.data[i],parameters[i], UtilEjml.TEST_F64);
		}
		int idx = 12;
		for (int i = 0; i < landmarks.size(); i++) {
			Point4D_F64 l = landmarks.get(i);
			assertEquals(l.x,parameters[idx++]);
			assertEquals(l.y,parameters[idx++]);
			assertEquals(l.z,parameters[idx++]);
			assertEquals(l.w,parameters[idx++]);
		}
	}

	@Test
	public void decode() {
		RefineProjectiveStructureAllVisible alg = new RefineProjectiveStructureAllVisible();

		List<Point2D_F64> obs = UtilPoint2D_F64.random(-1,1,3,rand);
		List<Point4D_F64> landmarks = UtilPoint4D_F64.random(-1,1,3,rand);

		DMatrixRMaj P0 = new DMatrixRMaj(new double[][]{{6,2,1,8},{0,7,3,3},{6,2,7,8}});
		DMatrixRMaj P1 = new DMatrixRMaj(new double[][]{{9,8,7,6},{5,4,3,2},{1,0,1,2}});

		alg.addView(false, P0 , obs);
		alg.addView(true , P1.copy() , obs);
		alg.setLandmarks(landmarks);

		double[] parameters = new double[12+4*3];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = i;
		}
		alg.decode(parameters);
		for (int i = 0; i < 12; i++) {
			assertEquals(parameters[i],alg.views.get(0).P.data[i], UtilEjml.TEST_F64);
		}
		for (int i = 0; i < 12; i++) {
			assertEquals(alg.views.get(1).P.data[i],P1.data[i], UtilEjml.TEST_F64);
		}
		int idx = 12;
		for (int i = 0; i < landmarks.size(); i++) {
			Point4D_F64 l = landmarks.get(i);
			assertEquals(parameters[idx++],l.x);
			assertEquals(parameters[idx++],l.y);
			assertEquals(parameters[idx++],l.z);
			assertEquals(parameters[idx++],l.w);
		}
	}

	/**
	 * Perfect settings
	 */
	@Test
	public void Residuals_perfect() {
		RefineProjectiveStructureAllVisible alg = new RefineProjectiveStructureAllVisible();

		List<Point4D_F64> landmarks = UtilPoint4D_F64.random(-1,1,3,rand);
		List<Point2D_F64> obs0 = new ArrayList<>();
		List<Point2D_F64> obs1 = new ArrayList<>();

		DMatrixRMaj P0 = new DMatrixRMaj(new double[][]{{0,1,2,3},{4,5,6,7},{8,9,0,1}});
		DMatrixRMaj P1 = new DMatrixRMaj(new double[][]{{9,8,7,6},{5,4,3,2},{1,0,1,2}});

		for (int i = 0; i < landmarks.size(); i++) {
			obs0.add(PerspectiveOps.renderPixel(P0,landmarks.get(i),(Point2D_F64)null));
			obs1.add(PerspectiveOps.renderPixel(P1,landmarks.get(i),(Point2D_F64)null));
		}

		// one fixed and one not fixed to test if that is handled correctly
		alg.addView(false, P0 , obs0);
		alg.addView(true , P1 , obs1);

		alg.setLandmarks(landmarks);

		double param[] = new double[alg.computeNumberOfParameters()];
		double residuals[] = new double[2*landmarks.size()*2];

		alg.encode(param);

		alg.function.process(param,residuals);

		for (int i = 0; i < residuals.length; i++) {
			assertEquals(0,residuals[i], UtilEjml.TEST_F64);
		}
	}

	/**
	 * Known errors
	 */
	@Test
	public void Residuals_error() {
		RefineProjectiveStructureAllVisible alg = new RefineProjectiveStructureAllVisible();

		List<Point4D_F64> landmarks = UtilPoint4D_F64.random(-1,1,3,rand);
		List<Point2D_F64> obs0 = new ArrayList<>();
		List<Point2D_F64> obs1 = new ArrayList<>();

		DMatrixRMaj P0 = new DMatrixRMaj(new double[][]{{0,1,2,3},{4,5,6,7},{8,9,0,1}});
		DMatrixRMaj P1 = new DMatrixRMaj(new double[][]{{9,8,7,6},{5,4,3,2},{1,0,1,2}});

		for (int i = 0; i < landmarks.size(); i++) {
			obs0.add(PerspectiveOps.renderPixel(P0,landmarks.get(i),(Point2D_F64)null));
			obs1.add(PerspectiveOps.renderPixel(P1,landmarks.get(i),(Point2D_F64)null));

//			System.out.println("obs0[ "+i+" ] = "+obs0.get(i));
			obs0.get(i).x += -0.5;
			obs0.get(i).y += 0.1;
			obs1.get(i).x += 0.2;
			obs1.get(i).y += 0.3;
		}

		// one fixed and one not fixed to test if that is handled correctly
		alg.addView(false, P0 , obs0);
		alg.addView(true , P1 , obs1);

		alg.setLandmarks(landmarks);

		double[] param = new double[alg.computeNumberOfParameters()];
		double[] residuals = new double[2 * landmarks.size() * 2];

		alg.encode(param);

		alg.function.process(param,residuals);

		int idx = 0;
		for (int i = 0; i < landmarks.size(); i++) {
			assertEquals( 0.5,residuals[idx++], UtilEjml.TEST_F64);
			assertEquals(-0.1,residuals[idx++], UtilEjml.TEST_F64);
		}
		for (int i = 0; i < landmarks.size(); i++) {
			assertEquals(-0.2,residuals[idx++], UtilEjml.TEST_F64);
			assertEquals(-0.3,residuals[idx++], UtilEjml.TEST_F64);
		}
	}

	/**
	 * Compare to numerical Jacobian
	 */
	@Test
	public void Jacobian_compareNumerical() {
		RefineProjectiveStructureAllVisible alg = new RefineProjectiveStructureAllVisible();

		List<Point4D_F64> landmarks = UtilPoint4D_F64.random(-1,1,3,rand);
		List<Point2D_F64> obs0 = new ArrayList<>();
		List<Point2D_F64> obs1 = new ArrayList<>();

		DMatrixRMaj P0 = new DMatrixRMaj(new double[][]{{0,1,2,3},{4,5,6,7},{8,9,0,1}});
		DMatrixRMaj P1 = new DMatrixRMaj(new double[][]{{9,8,7,6},{5,4,3,2},{1,0,1,2}});

		for (int i = 0; i < landmarks.size(); i++) {
			obs0.add(PerspectiveOps.renderPixel(P0,landmarks.get(i),(Point2D_F64)null));
			obs1.add(PerspectiveOps.renderPixel(P1,landmarks.get(i),(Point2D_F64)null));

			obs0.get(i).x += -0.5;
			obs0.get(i).y += 0.1;
			obs1.get(i).x += 0.2;
			obs1.get(i).y += 0.3;
		}

		// one fixed and one not fixed to test if that is handled correctly
		alg.addView(false, P0 , obs0);
		alg.addView(true , P1 , obs1);

		alg.setLandmarks(landmarks);

		double[] param = new double[alg.computeNumberOfParameters()];

		alg.encode(param);

//		DerivativeChecker.jacobianPrintR(alg.function, alg.jacobian, param, UtilEjml.TEST_F64_SQ);
		assertTrue(DerivativeChecker.jacobianR(alg.function, alg.jacobian, param, UtilEjml.TEST_F64_SQ));
	}
}