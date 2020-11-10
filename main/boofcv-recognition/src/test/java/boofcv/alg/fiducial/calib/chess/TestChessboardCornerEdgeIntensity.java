/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestChessboardCornerEdgeIntensity extends BoofStandardJUnit {

	double padding = 30;
	double length = 50;

	@Test
	void square() {
		RenderCalibrationTargetsGraphics2D render = new RenderCalibrationTargetsGraphics2D(30,1);
		render.chessboard(5,4,50);

		ChessboardCornerEdgeIntensity<GrayU8> alg = new ChessboardCornerEdgeIntensity<>(GrayU8.class);
		alg.setImage(render.getGrayU8());

		double x0 = padding+length;
		double y0 = padding+length;
		double yawA = Math.PI/4;
		double yawB = -Math.PI/4;

		ChessboardCorner a11 = create(x0,y0,yawA);
		ChessboardCorner a12 = create(x0+length,y0,yawB);
		ChessboardCorner a21 = create(x0,y0+length,yawB);
		ChessboardCorner a22 = create(x0+length,y0+length,yawA);

		double found0 = alg.process(a11,a12,0);
		double found1 = alg.process(a11,a21,Math.PI/2.0);
		double found2 = alg.process(a21,a22,0);

		assertEquals(255,found0, UtilEjml.TEST_F32);
		assertEquals(255,found1, UtilEjml.TEST_F32);
		assertEquals(255,found2, UtilEjml.TEST_F32);

		// same results in the other direction
		found0 = alg.process(a12,a11,Math.PI);
		found1 = alg.process(a21,a11,-Math.PI/2.0);
		found2 = alg.process(a22,a21,Math.PI);

		assertEquals(255,found0, UtilEjml.TEST_F32);
		assertEquals(255,found1, UtilEjml.TEST_F32);
		assertEquals(255,found2, UtilEjml.TEST_F32);
	}

	private static ChessboardCorner create( double x , double y , double yaw ) {
		ChessboardCorner c = new ChessboardCorner();
		c.setTo(x,y,yaw,0);
		return c;
	}
}
