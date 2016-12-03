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

import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCylinderToEquirectangular_F64 {

	/**
	 * The latitude and longitude should be zero when sampling the middle of the cylindrical image
	 */
	@Test
	public void pointingAtZero() {

		CylinderToEquirectangular_F64 alg = new CylinderToEquirectangular_F64();
		alg.setEquirectangularShape(400,501); // even division to make sure math works out nicely

		// height has an odd number to make it evenly divisible, e.g. (301-1)/2
		alg.configure(200,301, UtilAngle.radian(100));

		// center of rendered image
		alg.compute(100,150);

		// center of output image with y-axis inverted
		assertEquals(200,alg.distX, GrlConstants.DOUBLE_TEST_TOL);
		assertEquals(501-250-1,alg.distY, GrlConstants.DOUBLE_TEST_TOL);
	}

	/**
	 * Make sure the requested VFOV is the actual
	 */
	@Test
	public void checkFOV() {
		CylinderToEquirectangular_F64 alg = new CylinderToEquirectangular_F64();
		alg.setEquirectangularShape(400,500);

		alg.configure(200,300, UtilAngle.radian(100));

		Point2D_F64 ll = new Point2D_F64();

		alg.compute(100,0);
		alg.getTools().equiToLonlat(alg.distX,alg.distY,ll);
		double lat0 = ll.y;
		alg.compute(100,299);
		alg.getTools().equiToLonlat(alg.distX,alg.distY,ll);
		double lat1 = ll.y;


		assertEquals(UtilAngle.radian(100),lat1-lat0, GrlConstants.DOUBLE_TEST_TOL);
	}

	/**
	 * Crude vector check.  Make sure it's pointing -z at top of image and +z at bottom
	 */
	@Test
	public void checkVectors() {
		CylinderToEquirectangular_F64 alg = new CylinderToEquirectangular_F64();
		alg.setEquirectangularShape(400, 500);
		alg.configure(200, 300, UtilAngle.radian(100));

		assertTrue( alg.vectors[0].z < 0 );
		assertTrue( alg.vectors[299*200].z > 0 );

	}
}
