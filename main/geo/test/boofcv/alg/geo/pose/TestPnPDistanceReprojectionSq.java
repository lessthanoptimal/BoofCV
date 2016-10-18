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
import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPDistanceReprojectionSq {

	Random rand = new Random(234);

	/**
	 * Provide an observation with a known solution and see if it is computed correctly
	 */
	@Test
	public void checkErrorSingle() {
		DenseMatrix64F K = new DenseMatrix64F(3,3,true,100,0.01,200,0,150,200,0,0,1);

		Se3_F64 worldToCamera = new Se3_F64();
		worldToCamera.getT().set(0.1,-0.1,0.2);

		// Point location in world frame
		Point3D_F64 X = new Point3D_F64(0.1,-0.04,2.3);

		double deltaX = 0.1;
		double deltaY = -0.2;

		// create a noisy observed
		Point2D_F64 observed = PerspectiveOps.renderPixel(worldToCamera, K, X);

		observed.x += deltaX;
		observed.y += deltaY;

		// convert to normalized image coordinates
		PerspectiveOps.convertPixelToNorm(K,observed,observed);

		PnPDistanceReprojectionSq alg = new PnPDistanceReprojectionSq(K.get(0,0),K.get(1,1),K.get(0,1));
		alg.setModel(worldToCamera);

		double found = alg.computeDistance(new Point2D3D(observed,X));
		double expected = deltaX*deltaX + deltaY*deltaY;

		assertEquals(expected,found,1e-8);
	}

	/**
	 * A very large error should be returned if the point appears behind the second camera
	 */
	@Test
	public void checkBehindCamera() {
		DenseMatrix64F K = new DenseMatrix64F(3,3,true,100,0.01,200,0,150,200,0,0,1);

		Se3_F64 worldToCamera = new Se3_F64();
		worldToCamera.getT().set(0.1,-0.1,-2.5);

		// Point location in world frame
		Point3D_F64 X = new Point3D_F64(0.1,-0.04,2.3);

		Point2D_F64 observed = PerspectiveOps.renderPixel(worldToCamera, K, X);

		PnPDistanceReprojectionSq alg = new PnPDistanceReprojectionSq(K.get(0,0),K.get(1,1),K.get(0,1));
		alg.setModel(worldToCamera);

		double found = alg.computeDistance(new Point2D3D(observed,X));

		assertTrue(Double.MAX_VALUE == found );
	}

	/**
	 * Check the array error function for accuracy
	 */
	@Test
	public void checkErrorArray() {
		double expected[] = new double[5];

		DenseMatrix64F K = new DenseMatrix64F(3,3,true,100,0.01,200,0,150,200,0,0,1);

		Se3_F64 worldToCamera = new Se3_F64();
		worldToCamera.getT().set(0.1,-0.1,0.2);

		List<Point2D3D> obs = new ArrayList<>();
		for( int i = 0; i < expected.length; i++ ) {
			Point3D_F64 X =
					new Point3D_F64(rand.nextGaussian()*0.2,rand.nextGaussian()*0.2,2.3+rand.nextGaussian()*0.2);

			// create a noisy observed
			Point2D_F64 observed = PerspectiveOps.renderPixel(worldToCamera, K, X);

			double deltaX = rand.nextGaussian()*0.2;
			double deltaY = rand.nextGaussian()*0.2;

			observed.x += deltaX;
			observed.y += deltaY;

			// convert to normalized image coordinates
			PerspectiveOps.convertPixelToNorm(K,observed,observed);

			obs.add( new Point2D3D(observed,X));

			expected[i] = deltaX*deltaX + deltaY*deltaY;
		}

		PnPDistanceReprojectionSq alg = new PnPDistanceReprojectionSq(K.get(0,0),K.get(1,1),K.get(0,1));
		alg.setModel(worldToCamera);
		double found[] = new double[5];
		alg.computeDistance(obs,found);

		for( int i = 0; i < found.length; i++ ) {
			assertEquals(expected[i],found[i],1e-8);
		}
	}

}
