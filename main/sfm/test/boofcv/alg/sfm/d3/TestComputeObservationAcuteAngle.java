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

package boofcv.alg.sfm.d3;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestComputeObservationAcuteAngle {

	@Test
	public void simpleNoRotation() {
		ComputeObservationAcuteAngle alg = new ComputeObservationAcuteAngle();

		Se3_F64 fromAtoB = new Se3_F64();
		fromAtoB.getT().set(-2,0,0);

		Point2D_F64 a = new Point2D_F64(0,0);
		Point2D_F64 b = new Point2D_F64(0,0);

		alg.setFromAtoB(fromAtoB);

		assertEquals(0, alg.computeAcuteAngle(a, b), 1e-8);

		b.set(-1,0);

		assertEquals(Math.PI/4.0,alg.computeAcuteAngle(a,b),1e-8);
	}

	@Test
	public void simpleWithRotation() {
		ComputeObservationAcuteAngle alg = new ComputeObservationAcuteAngle();

		Se3_F64 fromAtoB = new Se3_F64();
		fromAtoB.getT().set(-2,0,0);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,-Math.PI/4.0,0,fromAtoB.getR());

		Point2D_F64 a = new Point2D_F64(0,0);
		Point2D_F64 b = new Point2D_F64(0,0);

		alg.setFromAtoB(fromAtoB);

		assertEquals(Math.PI/4.0,alg.computeAcuteAngle(a,b),1e-8);
	}
}
