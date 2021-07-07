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

package boofcv.abst.sfm.d3;

import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.ImageGray;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performs empirical validation of depth visual odometry algorithms using synthetic images. Only a crude test
 *
 * @author Peter Abeles
 */
public abstract class CheckVisualOdometryMonoPlaneSim<I extends ImageGray<I>>
		extends VideoSequenceSimulator<I> {
	CameraPinholeBrown param = new CameraPinholeBrown(150, 155, 0, width/2, height/2, width, height).fsetRadial(0, 0);
	MonocularPlaneVisualOdometry<I> algorithm;

	I left;

	// really not sure what's visible, so create a lot of objects
	protected int numSquares = 5000;
//	protected int numSquares = 50;

	double tolerance = 0.02;
	double cameraAngle = -0.75;

	protected CheckVisualOdometryMonoPlaneSim( Class<I> inputType ) {
		super(320, 240, inputType);

		// Turn off threads to make results repeatable
		BoofConcurrency.USE_CONCURRENT = false;

		left = GeneralizedImageOps.createSingleBand(inputType, width, height);

		createSquares(numSquares, -10, 10);
	}

	protected CheckVisualOdometryMonoPlaneSim( Class<I> inputType, double cameraAngle, double tolerance ) {
		this(inputType);
		this.tolerance = tolerance;
		this.cameraAngle = cameraAngle;
	}

	public void setAlgorithm( MonocularPlaneVisualOdometry<I> algorithm ) {
		this.algorithm = algorithm;
	}

	@Override
	protected void createSquares( int total, double minZ, double maxZ ) {
		squares.clear();

		double t = 0.1;

		// creates squares which are entirely on the ground plane
		for (int i = 0; i < total; i++) {

			double theta = rand.nextDouble()*Math.PI*2.0;
			double r = rand.nextDouble()*(maxZ - minZ) + minZ;

			double z = r*Math.cos(theta);
			double x = r*Math.sin(theta);
			double y = 0;

			Square s = new Square();
			s.a.setTo(x, y, z);
			s.b.setTo(x + t, y, z);
			s.c.setTo(x + t, y, z + t);
			s.d.setTo(x, y, z + t);

			s.gray = rand.nextInt(255);

			squares.add(s);
		}

		// create points far away
		t = 5.0;
		for (int i = 0; i < total; i++) {
			double theta = rand.nextDouble()*Math.PI*2.0;

			double r = 50;

			double z = r*Math.cos(theta);
			double x = r*Math.sin(theta);
			double y = -rand.nextDouble()*2 - 5; // stick them a bit up in the

			Square s = new Square();
			s.a.setTo(x, y, z);
			s.b.setTo(x + t, y, z);
			s.c.setTo(x + t, y + t, z);
			s.d.setTo(x, y + t, z);

			s.gray = rand.nextInt(255);

			squares.add(s);
		}

		// sort by depth so that objects farther way are rendered first and obstructed by objects closer in view
		Collections.sort(squares, Comparator.comparingDouble(o -> o.a.z));
	}

	@Test void moveForward() {
		motionCheck(0, 0.1);
	}

	@Test void moveTurning() {
		motionCheck(0.02, 0.1);
	}

	public void motionCheck( double angleRate, double forwardRate ) {

		// Easier to make up a plane in this direction
		Se3_F64 cameraToPlane = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, UtilAngle.degreeToRadian(cameraAngle), 0.1, 0.0, cameraToPlane.getR());
		cameraToPlane.getT().setTo(0, -2, 0);

		Se3_F64 planeToCamera = cameraToPlane.invert(null);

		Se3_F64 worldToCurr = new Se3_F64();
		Se3_F64 worldToCamera = new Se3_F64();

		algorithm.reset();
		algorithm.setCalibration(new MonoPlaneParameters(param, planeToCamera));

		for (int i = 0; i < 10; i++) {
//			System.out.println("-------- Real rotY = "+angleRate*i);
			worldToCurr.getT().z = -i*forwardRate; // move forward
			ConvertRotation3D_F64.rotY(angleRate*i, worldToCurr.getR());

			worldToCurr.concat(planeToCamera, worldToCamera);

			// render the images
			setIntrinsic(param);
			left.setTo(render(worldToCamera));

			// process the images
			assertTrue(algorithm.process(left));

			// Compare to truth. Only go for a crude approximation
			Se3_F64 foundWorldToCamera = algorithm.getCameraToWorld().invert(null);
			Se3_F64 foundWorldToCurr = foundWorldToCamera.concat(cameraToPlane, null);

//			worldToCurr.getT().print();
//			foundWorldToCurr.getT().print();

//			worldToCurr.getR().print();
//			foundWorldToCurr.getR().print();

			assertTrue(MatrixFeatures_DDRM.isIdentical(foundWorldToCurr.getR(), worldToCurr.getR(), 0.1));
			assertTrue(foundWorldToCurr.getT().distance(worldToCurr.getT()) < tolerance);
		}
	}
}
