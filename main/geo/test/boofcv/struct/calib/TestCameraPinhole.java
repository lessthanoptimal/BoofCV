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

package boofcv.struct.calib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCameraPinhole {

	@Test
	public void constructor_K() {
		CameraPinhole p = new CameraPinhole(200,210,1,320,240,640,380);

		assertEquals(200,p.fx,1e-8);
		assertEquals(210,p.fy,1e-8);
		assertEquals(1,p.skew,1e-8);
		assertEquals(320,p.cx,1e-8);
		assertEquals(240,p.cy,1e-8);
		assertEquals(640,p.width,1e-8);
		assertEquals(380,p.height,1e-8);
	}

	@Test
	public void fsetK() {
		CameraPinhole p = new CameraPinhole(200,210,1,320,240,640,380);

		assertTrue(p == p.fsetK(201, 211, 2, 321, 241, 641, 381));

		assertEquals(201,p.fx,1e-8);
		assertEquals(211,p.fy,1e-8);
		assertEquals(2,p.skew,1e-8);
		assertEquals(321,p.cx,1e-8);
		assertEquals(241,p.cy,1e-8);
		assertEquals(641,p.width,1e-8);
		assertEquals(381,p.height,1e-8);
	}

}
