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

package boofcv.alg.distort.universal;

import boofcv.struct.calib.CameraUniversalOmni;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestUniOmniPtoS_F64 {
	/**
	 * Tell it to project the center pixel forwards.  All the distortion shouldn't affect it there and it should
	 * appear to be image center exactly.
	 */
	@Test
	public void centerIsCenter() {
		centerIsCenter(1.0);
		centerIsCenter(0.5);
		centerIsCenter(3.5);

	}

	private void centerIsCenter( double mirror ) {
		CameraUniversalOmni model = createModel(mirror);

		UniOmniPtoS_F64 alg = new UniOmniPtoS_F64();
		alg.setModel(model);

		Point3D_F64 found = new Point3D_F64(10,10, 10);
		alg.compute(320,240, found);  // directly forward on unit sphere

		assertEquals(0,found.x, GrlConstants.DOUBLE_TEST_TOL);
		assertEquals(0,found.y, GrlConstants.DOUBLE_TEST_TOL);
		assertEquals(1,found.z, GrlConstants.DOUBLE_TEST_TOL);
	}

	@Test
	public void back_and_forth() {
		back_and_forth(1.0);
		back_and_forth(0.5);
		back_and_forth(3.5);
	}

	private void back_and_forth( double mirror ) {
		CameraUniversalOmni model = createModel(mirror);

		UniOmniPtoS_F64 pixelToUnit = new UniOmniPtoS_F64();
		pixelToUnit.setModel(model);

		UniOmniStoP_F64 unitToPixel = new UniOmniStoP_F64();
		unitToPixel.setModel(model);

		List<Point2D_F64> listPixels = new ArrayList<>();
		listPixels.add( new Point2D_F64(320,240));
		listPixels.add( new Point2D_F64(320,200));
		listPixels.add( new Point2D_F64(320,280));
		listPixels.add( new Point2D_F64(280,240));
		listPixels.add( new Point2D_F64(360,240));
		listPixels.add( new Point2D_F64(280,240));
		listPixels.add( new Point2D_F64(240,180));

		for( Point2D_F64 pixel : listPixels ) {
			Point3D_F64 circle = new Point3D_F64(10,10, 10);
			pixelToUnit.compute(pixel.x,pixel.y, circle);  // directly forward on unit sphere

			// it should be on the unit circle
			assertEquals(1.0, circle.norm(), GrlConstants.DOUBLE_TEST_TOL);

			Point2D_F64 found = new Point2D_F64();
			unitToPixel.compute(circle.x, circle.y, circle.z, found);

			assertEquals(pixel.x, found.x, GrlConstants.DOUBLE_TEST_TOL_SQRT);
			assertEquals(pixel.y, found.y, GrlConstants.DOUBLE_TEST_TOL_SQRT);
		}
	}

	public static CameraUniversalOmni createModel(double mirror) {
		CameraUniversalOmni model = new CameraUniversalOmni(2);

		model.fsetK(400,405,0.01,320,240,640,480);
		model.fsetMirror(mirror);
		model.fsetRadial(0.01,-0.03);
		model.fsetTangental(0.001,0.002);
		return model;
	}
}