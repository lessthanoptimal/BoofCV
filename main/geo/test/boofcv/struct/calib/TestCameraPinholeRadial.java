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

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestCameraPinholeRadial {

	@Test
	public void fsetRadial() {
		CameraPinholeRadial p = new CameraPinholeRadial(200,210,1,320,240,640,380);

		assertTrue(p == p.fsetRadial(1.1,2.2,3.3));

		assertTrue(p.radial.length==3);
		assertEquals(1.1,p.radial[0],1e-8);
		assertEquals(2.2,p.radial[1],1e-8);
		assertEquals(3.3,p.radial[2],1e-8);

		assertEquals(200,p.fx,1e-8);
		assertEquals(210,p.fy,1e-8);
		assertEquals(1,p.skew,1e-8);
		assertEquals(320,p.cx,1e-8);
		assertEquals(240,p.cy,1e-8);
		assertEquals(640,p.width,1e-8);
		assertEquals(380,p.height,1e-8);
		assertEquals(0,p.t1,1e-8);
		assertEquals(0,p.t2, 1e-8);
	}

	@Test
	public void fsetTangental() {
		CameraPinholeRadial p = new CameraPinholeRadial(200,210,1,320,240,640,380);

		assertTrue(p == p.fsetTangental(1.1, 2.2));

		assertEquals(1.1,p.t1,1e-8);
		assertEquals(2.2,p.t2, 1e-8);

		assertEquals(200,p.fx,1e-8);
		assertEquals(210,p.fy,1e-8);
		assertEquals(1,p.skew,1e-8);
		assertEquals(320,p.cx,1e-8);
		assertEquals(240,p.cy,1e-8);
		assertEquals(640,p.width,1e-8);
		assertEquals(380,p.height,1e-8);
		assertTrue(p.radial==null);
	}

	@Test
	public void isDistorted() {
		CameraPinholeRadial p = new CameraPinholeRadial(200,210,0,320,240,640,380);

		assertFalse(p.isDistorted());
		assertFalse(p.fsetRadial(0,0).isDistorted());
		assertTrue(p.fsetRadial(1,0).isDistorted());
		assertFalse(p.fsetRadial(0,0).isDistorted());
		assertTrue(p.fsetTangental(0, 0.1).isDistorted());
	}
}
