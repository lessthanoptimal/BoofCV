/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.javacv;

import boofcv.struct.calib.CameraPinholeRadial;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestUtilOpenCV {
	@Test
	public void loadPinholeRadial() {
		URL url = TestUtilOpenCV.class.getResource("pinhole_distorted.yml");
		CameraPinholeRadial model = UtilOpenCV.loadPinholeRadial(url.getFile());

		assertEquals(640, model.width);
		assertEquals(480, model.height);

		assertEquals(5.2626362816407709e+02, model.fx, 1e-8);
		assertEquals(0, model.skew, 1e-8);
		assertEquals(5.2830704330313858e+02, model.fy, 1e-8);
		assertEquals(3.1306715867053975e+02, model.cx, 1e-8);
		assertEquals(2.4747722332735930e+02, model.cy, 1e-8);

		assertEquals(3, model.radial.length);
		assertEquals(-3.7077854691489726e-01, model.radial[0], 1e-8);
		assertEquals(2.4308561956661329e-01, model.radial[1], 1e-8);
		assertEquals(-1.2351969388838037e-01, model.radial[2], 1e-8);

		assertEquals(4.4148972909019294e-04, model.t1, 1e-8);
		assertEquals(-6.0304229617877381e-04, model.t2, 1e-8);
	}

	@Test
	public void savePinholeRadial() {
		CameraPinholeRadial model = new CameraPinholeRadial();

		model.fsetK(1,2,3,4,0.65,100,7);
		model.fsetRadial(.1,.2,.3);
		model.fsetTangental(0.5,0.7);

		UtilOpenCV.save(model, "temp.yml");

		CameraPinholeRadial found = UtilOpenCV.loadPinholeRadial("temp.yml");

		assertEquals( model.width , found.width );
		assertEquals( model.height , found.height );
		assertEquals( model.fx , found.fx, 1e-8 );
		assertEquals( model.fy , found.fy, 1e-8 );
		assertEquals( model.skew , found.skew, 1e-8 );
		assertEquals( model.cx , found.cx, 1e-8 );
		assertEquals( model.cy , found.cy, 1e-8 );

		for (int i = 0; i < 3; i++) {
			assertEquals( model.radial[i], found.radial[i], 1e-8);
		}

		assertEquals( model.t1 , found.t1, 1e-8 );
		assertEquals( model.t2 , found.t2, 1e-8 );

		assertTrue(new File("temp.yml").delete());
	}
}