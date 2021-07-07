/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.trifocal;

import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTrifocalTransfer extends CommonTrifocalChecks {
	@Test void transfer13() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(),X, null);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2,K,X, null);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3,K,X, null);

		Point3D_F64 found = new Point3D_F64();
		TrifocalTransfer alg = new TrifocalTransfer();
		alg.setTrifocal(tensor);
		alg.transfer_1_to_2(x1.x,x1.y,x3.x,x3.y,found);

		found.x /= found.z;
		found.y /= found.z;

		assertEquals(x2.x,found.x, UtilEjml.TEST_F64);
		assertEquals(x2.y,found.y, UtilEjml.TEST_F64);
	}

	@Test void transfer12() {
		Point3D_F64 X = new Point3D_F64(0.1,-0.05,2);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(),X, null);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2,K,X, null);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3,K,X, null);

		Point3D_F64 found = new Point3D_F64();
		TrifocalTransfer alg = new TrifocalTransfer();
		alg.setTrifocal(tensor);
		alg.transfer_1_to_3(x1.x,x1.y,x2.x,x2.y,found);

		found.x /= found.z;
		found.y /= found.z;

		assertEquals(x3.x,found.x, UtilEjml.TEST_F64);
		assertEquals(x3.y,found.y, UtilEjml.TEST_F64);
	}
}
