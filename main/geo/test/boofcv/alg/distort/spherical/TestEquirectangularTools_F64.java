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
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestEquirectangularTools_F64 {

	/**
	 * Test converting back and forth between equirectangular coordinates and lat-lon using different
	 * centers
	 */
	@Test
	public void equiToLatlon_reverse() {

		EquirectangularTools_F64 tools = new EquirectangularTools_F64();

		// change the focus to several different locations
		for (int i = 0; i < 20; i++) {
			double lat = Math.PI*i/19.0 - GrlConstants.PId2;
			for (int j = 0; j < 20; j++) {
				double lon = 2.0 * Math.PI * i/19.0 - GrlConstants.PI;

				tools.configure(300,250,lon,lat);

				// test back and forth conversations at different points
				testCoordinate(tools, 150, 125);
				testCoordinate(tools, 0, 125);
				testCoordinate(tools, 150, 0);
				testCoordinate(tools, 150, 249);
			}
		}

	}

	private void testCoordinate(EquirectangularTools_F64 tools, double x , double y) {
		Point2D_F64 ll = new Point2D_F64();
		Point2D_F64 r = new Point2D_F64();

		tools.equiToLatlon(x,y,ll);
		tools.latlonToRect(ll.x,ll.y,r);

		assertEquals(x,r.x, GrlConstants.DOUBLE_TEST_TOL);
		assertEquals(y,r.y, GrlConstants.DOUBLE_TEST_TOL);
	}

	/**
	 * Test one very simple case with a known answer
	 */
	@Test
	public void equiToNorm() {
		EquirectangularTools_F64 tools = new EquirectangularTools_F64();

		tools.configure(300,250,0,0);

		Vector3D_F64 found = new Vector3D_F64();
		tools.equiToNorm(300.0/2.0, 250.0/2.0, found);

		assertEquals(1.0,found.x, GrlConstants.DOUBLE_TEST_TOL);
		assertEquals(0.0,found.y, GrlConstants.DOUBLE_TEST_TOL);
		assertEquals(0.0,found.z, GrlConstants.DOUBLE_TEST_TOL);

	}
}
