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

package boofcv.alg.distort.spherical;

import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Vector3D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestEquirectangularTools_F32 {

	/**
	 * Test converting back and forth between equirectangular coordinates and lat-lon using different
	 * centers
	 */
	@Test
	public void equiToLatlon_reverse() {

		EquirectangularTools_F32 tools = new EquirectangularTools_F32();

		// change the focus to several different locations
		for (int i = 0; i < 20; i++) {
			float lat = (float)Math.PI*i/19.0f - GrlConstants.F_PId2;
			for (int j = 0; j < 20; j++) {
				float lon = 2.0f * (float)Math.PI * i/19.0f - GrlConstants.F_PI;

				tools.configure(300,250,lon,lat);

				// test back and forth conversations at different points
				testCoordinate(tools, 150, 125);
				testCoordinate(tools, 0, 125);
				testCoordinate(tools, 150, 0);
				testCoordinate(tools, 150, 249);
			}
		}

	}

	private void testCoordinate(EquirectangularTools_F32 tools, float x , float y) {
		Point2D_F32 ll = new Point2D_F32();
		Point2D_F32 r = new Point2D_F32();

		tools.equiToLatlon(x,y,ll);
		tools.latlonToRect(ll.x,ll.y,r);

		assertEquals(x,r.x, GrlConstants.FLOAT_TEST_TOL);
		assertEquals(y,r.y, GrlConstants.FLOAT_TEST_TOL);
	}

	/**
	 * Test one very simple case with a known answer
	 */
	@Test
	public void equiToNorm() {
		EquirectangularTools_F32 tools = new EquirectangularTools_F32();

		tools.configure(300,250,0,0);

		Vector3D_F32 found = new Vector3D_F32();
		tools.equiToNorm(299.0f/2.0f, 249/2.0f, found);

		assertEquals(1.0f,found.x, GrlConstants.FLOAT_TEST_TOL);
		assertEquals(0.0f,found.y, GrlConstants.FLOAT_TEST_TOL);
		assertEquals(0.0f,found.z, GrlConstants.FLOAT_TEST_TOL);

	}
}
