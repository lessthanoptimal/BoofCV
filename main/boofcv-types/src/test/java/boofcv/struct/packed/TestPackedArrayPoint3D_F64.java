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

package boofcv.struct.packed;

import boofcv.struct.PackedArray;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Peter Abeles
 */
public class TestPackedArrayPoint3D_F64 extends GenericPackedArrayChecks<Point3D_F64> {

	@Override protected PackedArray<Point3D_F64> createAlg() {
		return new PackedArrayPoint3D_F64();
	}

	@Override protected Point3D_F64 createRandomPoint() {
		var point = new Point3D_F64();
		point.x = (double) rand.nextGaussian();
		point.y = (double) rand.nextGaussian();
		point.z = (double) rand.nextGaussian();
		return point;
	}

	@Override protected void checkEquals( Point3D_F64 a, Point3D_F64 b ) {
		assertEquals(0.0, a.distance(b), UtilEjml.TEST_F64);
	}

	@Override protected void checkNotEquals( Point3D_F64 a, Point3D_F64 b ) {
		assertNotEquals(0.0, a.distance(b), UtilEjml.TEST_F64);
	}
}
