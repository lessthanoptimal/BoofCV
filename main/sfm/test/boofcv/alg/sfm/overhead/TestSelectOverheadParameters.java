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

package boofcv.alg.sfm.overhead;

import boofcv.struct.calib.CameraPinholeRadial;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSelectOverheadParameters {

	protected int width=320,height=240;
	CameraPinholeRadial param = new CameraPinholeRadial(150,150,0,width/2,height/2,width,height).fsetRadial(0,0);
	Se3_F64 cameraToPlane = new Se3_F64();
	Se3_F64 planeToCamera;

	double cellSize = 0.05;

	@Test
	public void pointedStraightDown() {
		createExtrinsic(-2,-Math.PI/2,0);

		SelectOverheadParameters alg = new SelectOverheadParameters(cellSize,5,1);

		assertTrue(alg.process(param,planeToCamera));

		double mapWidth = alg.getOverheadWidth()*cellSize;
		double mapHeight = alg.getOverheadHeight()*cellSize;

		double tanCameraX = param.cx/param.fx;
		double tanCameraY = param.cy/param.fy;
		double tanWidth = mapWidth/4.0;
		double tanHeight = mapHeight/4.0;

		// crude check to see if the suggested map has about the save FOV as the camera
		assertEquals(tanCameraX,tanHeight,0.05);
		assertEquals(tanCameraY,tanWidth,0.05);

		// won't be perfectly symmetric since the image doesn't project perfectly symmetric
		assertEquals(alg.getCenterX(),mapWidth/2.0,cellSize);
		assertEquals(alg.getCenterY(),mapHeight/2.0,cellSize);
	}

	@Test
	public void checkFailure() {
		SelectOverheadParameters alg = new SelectOverheadParameters(cellSize,5,1);

		// look up away from the map, should fail
		createExtrinsic(-2,Math.PI/2,0);
		assertFalse(alg.process(param, planeToCamera));

		// try from below the plane, it should see it now
		createExtrinsic(2,Math.PI/2,0);
		assertTrue(alg.process(param, planeToCamera));
	}

	@Test
	public void pointedAngle() {
		createExtrinsic(-2,UtilAngle.degreeToRadian(-45),0);

		SelectOverheadParameters alg = new SelectOverheadParameters(cellSize,5,1);

		assertTrue(alg.process(param, planeToCamera));

		double mapHeight = alg.getOverheadHeight()*cellSize;

		// center X should be farther down the camera's pointing direction
		assertTrue(alg.getCenterX() < 0);
		// still symmetric
		assertEquals(alg.getCenterY(),mapHeight/2,cellSize);
	}

	private void createExtrinsic( double y , double rotX , double rotZ ) {
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX, 0, rotZ, cameraToPlane.getR());
		cameraToPlane.getT().set(0,y,0);
		planeToCamera = cameraToPlane.invert(null);
	}

}
