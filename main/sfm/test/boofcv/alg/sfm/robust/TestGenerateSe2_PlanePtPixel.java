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

package boofcv.alg.sfm.robust;

import boofcv.alg.sfm.overhead.CameraPlaneProjection;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.sfm.PlanePtPixel;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGenerateSe2_PlanePtPixel {

	Random rand = new Random(93948);

	CameraPinholeRadial intrinsic = new CameraPinholeRadial(200,210,0,320,240,640,480).fsetRadial(0,0);

	GenerateSe2_PlanePtPixel alg = new GenerateSe2_PlanePtPixel();
	Se3_F64 planeToCamera;
	Se2_F64 motion2D;

	List<PlanePtPixel> observations = new ArrayList<>();

	public TestGenerateSe2_PlanePtPixel() {
		// Easier to make up a plane in this direction
		Se3_F64 cameraToPlane = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,UtilAngle.degreeToRadian(-75), 0.1, 0.0, cameraToPlane.getR());
		cameraToPlane.getT().set(0, -2, 0);

		planeToCamera = cameraToPlane.invert(null);
		motion2D = new Se2_F64(0.5,-0.05,0.15);

	}

	@Test
	public void perfect() {
		alg.setExtrinsic(planeToCamera);

		CameraPlaneProjection planeProjection = new CameraPlaneProjection();
		planeProjection.setConfiguration(planeToCamera, intrinsic);

		for( int i = 0; i < alg.getMinimumPoints(); i++ ) {
			PlanePtPixel s = new PlanePtPixel();

			double x = rand.nextDouble()*intrinsic.width;
			double y = rand.nextDouble()*intrinsic.height;

			Point2D_F64 pixelA = new Point2D_F64(x,y);
			Point2D_F64 planePtA = new Point2D_F64();
			planeProjection.pixelToPlane(pixelA.x, pixelA.y, planePtA);

			Point2D_F64 planePtB = new Point2D_F64();
			SePointOps_F64.transform(motion2D,planePtA,planePtB);
			planeProjection.planeToNormalized(planePtB.x,planePtB.y,s.normalizedCurr);

			s.planeKey.set(planePtA);
			observations.add(s);
		}

		Se2_F64 found = new Se2_F64();
		assertTrue(alg.generate(observations,found));

		assertEquals(motion2D.T.x, found.T.x, 1e-8);
		assertEquals(motion2D.T.y, found.T.y, 1e-8);
		assertEquals(motion2D.getYaw(), found.getYaw(), 1e-8);
	}


}
