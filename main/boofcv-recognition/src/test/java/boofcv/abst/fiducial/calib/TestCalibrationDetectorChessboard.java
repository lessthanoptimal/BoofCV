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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static boofcv.abst.fiducial.calib.CalibrationDetectorChessboard.gridChess;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorChessboard extends GenericPlanarCalibrationDetectorChecks {


	public TestCalibrationDetectorChessboard() {
		targetConfigs.add( new ConfigChessboard(5, 4, 30) );

		// tuning made 1 fail. On real world data it does better. <shrug>
		fisheyeAllowedFails = 1;
	}

	@Test
	public void createLayout() {
		List<Point2D_F64> layout = createDetector(new ConfigChessboard(5, 4, 30) ).getLayout();

		// first control points should be the top left corner then work it's way down in a
		// grid pattern
		assertTrue(layout.get(0).y == layout.get(2).y);
		assertTrue(layout.get(0).x <  layout.get(2).x);
		assertTrue(layout.get(0).y >  layout.get(3).y);
	}

	@Override
	public void renderTarget(Object layout, double length3D , GrayF32 image, List<Point2D_F64> points2D) {
		ConfigChessboard config = (ConfigChessboard)layout;

		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(40,1);

		renderer.chessboard(config.numRows,config.numCols,40);

//		ShowImages.showWindow(renderer.getBufferred(),"Rendered",true);
//		BoofMiscOps.sleep(100000);
		image.setTo(renderer.getGrayF32());

		double lengthPattern = length3D*config.numCols/(config.numCols+2);

		points2D.clear();
		points2D.addAll( gridChess(config.numRows, config.numCols, lengthPattern/config.numCols ));

	}

	@Override
	public DetectorFiducialCalibration createDetector(Object layout) {
		return FactoryFiducialCalibration.chessboard((ConfigChessboard)layout);
	}
}