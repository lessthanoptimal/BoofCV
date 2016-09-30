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
import boofcv.struct.image.ImageBase;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCreateSyntheticOverheadView {

	int width = 800;
	int height = 850;
	CameraPinholeRadial param = new CameraPinholeRadial(200,201,0,width/2,height/2,width,height).fsetRadial( 0.002,0);

	@Test
	public void checkPrecomputedTransform() {
		// Easier to make up a plane in this direction
		Se3_F64 cameraToPlane = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,UtilAngle.degreeToRadian(0), 0, 0, cameraToPlane.getR());
		cameraToPlane.getT().set(0,-5,0);

		Se3_F64 planeToCamera = cameraToPlane.invert(null);

		CreateSyntheticOverheadView alg = new CreateSyntheticOverheadView() {
			@Override
			public void process(ImageBase input, ImageBase output) {
			}
		};

		int overheadW = 500;
		int overheadH = 600;
		double cellSize = 0.05;
		double centerX = 1;
		double centerY = overheadH*cellSize/2.0;

		alg.configure(param,planeToCamera,centerX,centerY,cellSize,overheadW,overheadH);

		//  directly below camera, should not be in view
		assertTrue(null == alg.getOverheadToPixel(0, 300));

		//  point at the end of the map should be in view
		assertTrue(null!=alg.getOverheadToPixel(overheadW-1,300));

		// check the value at one point by doing the reverse transform
		Point2D_F32 found = alg.getOverheadToPixel(400,320);

		CameraPlaneProjection proj = new CameraPlaneProjection();
		proj.setPlaneToCamera(planeToCamera,true);
		proj.setIntrinsic(param);


		Point2D_F64 expected = new Point2D_F64();
		proj.pixelToPlane(found.x, found.y, expected);

		// put into overhead pixels
		expected.x = (expected.x+centerX)/cellSize;
		expected.y = (expected.y+centerY)/cellSize;

		assertEquals(400,expected.x,1e-4);
		assertEquals(320,expected.y,1e-4);
	}
}
