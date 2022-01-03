/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.testing.BoofStandardJUnit;
import georegression.metric.UtilAngle;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.jupiter.api.Test;

import static boofcv.alg.fiducial.qrcode.TestQrCodePositionPatternDetector.squareNode;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestQrCodePositionPatternGraphGenerator extends BoofStandardJUnit {
	/**
	 * Simple positive example
	 */
	@Test void considerConnect_positive() {
		var alg = new QrCodePositionPatternGraphGenerator(2);

		SquareNode n0 = squareNode(40, 60, 70);
		SquareNode n1 = squareNode(140, 60, 70);

		alg.considerConnect(n0, n1);

		assertEquals(1, n0.getNumberOfConnections());
		assertEquals(1, n1.getNumberOfConnections());
	}

	/**
	 * The two patterns are rotated 45 degrees relative to each other
	 */
	@Test void considerConnect_negative_rotated() {
		var alg = new QrCodePositionPatternGraphGenerator(40);

		SquareNode n0 = squareNode(40, 60, 70);
		SquareNode n1 = squareNode(140, 60, 70);

		Se2_F64 translate = new Se2_F64(-175, -95, 0);
		Se2_F64 rotate = new Se2_F64(0, 0, UtilAngle.radian(45));

		Se2_F64 tmp = translate.concat(rotate, null);
		Se2_F64 combined = tmp.concat(translate.invert(null), null);

		for (int i = 0; i < 4; i++) {
			SePointOps_F64.transform(combined, n1.square.get(i), n1.square.get(i));
		}
		SePointOps_F64.transform(combined, n1.center, n1.center);

		alg.considerConnect(n0, n1);

		assertEquals(0, n0.getNumberOfConnections());
		assertEquals(0, n1.getNumberOfConnections());
	}

	/**
	 * Call it mul
	 */
	@Test void multipleCalls() {

	}
}
