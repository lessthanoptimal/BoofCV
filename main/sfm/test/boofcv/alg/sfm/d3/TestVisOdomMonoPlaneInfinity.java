/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d3;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestVisOdomMonoPlaneInfinity {
	@Test
	public void stuff() {
		// This is intentionally left blank.  Tests are performed inside the abstract package
	}

	/**
	 * Look for the largest inlier in the center
	 */
	@Test
	public void maximizeCountInSpread_nominal() {

		double data[] = new double[23];

		data[0] = 1;
		data[1] = 1.1;
		data[2] = 1.2;
		data[3] = 1.2;
		data[4] = 1.3;
		data[8] = 1.45;
		data[5] = 1.5;
		data[6] = 1.51;
		data[7] = 1.5;
		data[9] = 5;
		data[10] = 5;
		data[11] = 6;
		data[12] = 0.01;
		data[13] = 0.02;

		double found = VisOdomMonoPlaneInfinity.maximizeCountInSpread(data,15,0.5);
		assertEquals(1.3,found,1e-8);

		for( int i = 0; i < data.length; i++ )
			data[i] = -data[i];

		found = VisOdomMonoPlaneInfinity.maximizeCountInSpread(data,15,0.5);
		assertEquals(-1.3,found,1e-8);
	}

	/**
	 * See if it finds the largest inlier set when it requires the angle to wrap around
	 */
	@Test
	public void maximizeCountInSpread_edge() {
		double data[] = new double[23];

		data[0] = 1;
		data[1] = 1.1;
		data[2] = 1.2;
		data[4] = 1.3;
		data[5] = 1.5;
		data[6] = 1.51;
		data[7] = 1.5;
		data[8] = 1.45;
		data[9] = 2;
		data[10] = 2.2;
		data[11] = 2.3;
		data[12] = -(Math.PI-0.01);
		data[13] = -(Math.PI-0.02);
		data[14] = -(Math.PI-0.03);
		data[15] = -(Math.PI-0.04);
		data[16] = -(Math.PI-0.05);
		data[17] = Math.PI-0.01;
		data[18] = Math.PI-0.02;
		data[19] = Math.PI-0.03;
		data[20] = Math.PI-0.04;
		data[21] = Math.PI-0.05;
		data[22] = Math.PI;



		double found = VisOdomMonoPlaneInfinity.maximizeCountInSpread(data,23,0.5);
		assertEquals(Math.PI,found,1e-8);
	}
}
