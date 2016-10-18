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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPStereoDistanceReprojectionSq extends CommonStereoMotionNPoint{

	@Test
	public void checkErrorSingle() {
		// Point location in world frame
		Point3D_F64 X = new Point3D_F64(0.1,-0.04,2.3);

		DenseMatrix64F K_left = PerspectiveOps.calibrationMatrix(param.left,null);
		DenseMatrix64F K_right = PerspectiveOps.calibrationMatrix(param.right,null);

		// errors
		double deltaX0 = 0.1;
		double deltaY0 = -0.2;
		double deltaX1 = -0.3;
		double deltaY1 = 0.05;

		// create a noisy observed
		Point2D_F64 obsLeft = PerspectiveOps.renderPixel(worldToLeft, K_left, X);
		obsLeft.x += deltaX0;
		obsLeft.y += deltaY0;
		Point2D_F64 obsRight = PerspectiveOps.renderPixel(worldToRight, K_right, X);
		obsRight.x += deltaX1;
		obsRight.y += deltaY1;

		// convert to normalized image coordinates
		PerspectiveOps.convertPixelToNorm(K_left,obsLeft,obsLeft);
		PerspectiveOps.convertPixelToNorm(K_right,obsRight,obsRight);

		PnPStereoDistanceReprojectionSq alg = new PnPStereoDistanceReprojectionSq();
		alg.setStereoParameters(param);
		alg.setModel(worldToLeft);

		double found = alg.computeDistance(new Stereo2D3D(obsLeft,obsRight,X));
		double expected = deltaX0*deltaX0 + deltaY0*deltaY0 + deltaX1*deltaX1 + deltaY1*deltaY1;

		assertEquals(expected,found,1e-8);
	}

	/**
	 * Have the observation be behind the left camera but not the right
	 */
	@Test
	public void checkBehindCamera_Left() {
		checkBehind(-0.1,-0.05);
	}

	/**
	 * Have the observation be behind the right camera but not the left
	 */
	@Test
	public void checkBehindCamera_Right() {
		checkBehind(0.1,0.05);
	}

	public void checkBehind( double Tz , double Pz ) {
			// Point location in world frame
			Point3D_F64 X = new Point3D_F64(0.1,-0.04,Pz);

			DenseMatrix64F K_left = PerspectiveOps.calibrationMatrix(param.left,null);
			DenseMatrix64F K_right = PerspectiveOps.calibrationMatrix(param.right,null);

			// create a noisy observed
			Point2D_F64 obsLeft = PerspectiveOps.renderPixel(worldToLeft, K_left, X);
			Point2D_F64 obsRight = PerspectiveOps.renderPixel(worldToRight, K_right, X);

			// convert to normalized image coordinates
			PerspectiveOps.convertPixelToNorm(K_left,obsLeft,obsLeft);
			PerspectiveOps.convertPixelToNorm(K_right,obsRight,obsRight);

			PnPStereoDistanceReprojectionSq alg = new PnPStereoDistanceReprojectionSq();
			StereoParameters param = new StereoParameters(this.param.left,this.param.right,new Se3_F64());
			param.rightToLeft.getT().set(0.1,0,Tz);

			alg.setStereoParameters(param);
			alg.setModel(new Se3_F64());

			double found = alg.computeDistance(new Stereo2D3D(obsLeft,obsRight,X));

			assertTrue(Double.MAX_VALUE == found);
	}

	@Test
	public void checkErrorArray() {
		DenseMatrix64F K_left = PerspectiveOps.calibrationMatrix(param.left,null);
		DenseMatrix64F K_right = PerspectiveOps.calibrationMatrix(param.right,null);

		PnPStereoDistanceReprojectionSq alg = new PnPStereoDistanceReprojectionSq();
		alg.setStereoParameters(param);
		alg.setModel(worldToLeft);

		int N = 10;
		double expected[] = new double[N*4];
		List<Stereo2D3D> obs = new ArrayList<>();

		for( int i = 0; i < N; i++ ) {
			// Point location in world frame
			Point3D_F64 X = new Point3D_F64(rand.nextGaussian(),rand.nextGaussian(),2.3);

			// create a noisy observed
			Point2D_F64 obsLeft = PerspectiveOps.renderPixel(worldToLeft, K_left, X);
			obsLeft.x += rand.nextGaussian()*0.05;
			obsLeft.y += rand.nextGaussian()*0.05;
			Point2D_F64 obsRight = PerspectiveOps.renderPixel(worldToRight, K_right, X);
			obsRight.x += rand.nextGaussian()*0.05;
			obsRight.y += rand.nextGaussian()*0.05;

			// convert to normalized image coordinates
			PerspectiveOps.convertPixelToNorm(K_left,obsLeft,obsLeft);
			PerspectiveOps.convertPixelToNorm(K_right,obsRight,obsRight);

			Stereo2D3D p = new Stereo2D3D(obsLeft,obsRight,X);

			expected[i] = alg.computeDistance(p);
			obs.add( p );
		}



		double found[] = new double[ N ];
		alg.computeDistance(obs,found);

		for( int i = 0; i < N; i++ )
			assertEquals(expected[i],found[i],1e-8);
	}
}
