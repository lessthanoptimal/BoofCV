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

import java.util.List;

import static boofcv.abst.fiducial.calib.CalibrationDetectorCircleHexagonalGrid.createLayout;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorCircleHexagonalGrid extends GenericPlanarCalibrationDetectorChecks {

	public TestCalibrationDetectorCircleHexagonalGrid() {
		// each configuration has a different ending that needs to be handled
		targetConfigs.add( new ConfigCircleHexagonalGrid(5, 5, 20,24) );
		targetConfigs.add( new ConfigCircleHexagonalGrid(5, 6, 20,24) );
		targetConfigs.add( new ConfigCircleHexagonalGrid(6, 6, 20,24) );

		// Does a good job detecting the ellipses, but a shit job determining with the tangent points
		// The lens distortion moves them so that they aren't even close
		fisheyeMatchTol = 10;
		fisheyeAllowedFails = 4;
	}

	@Override
	public void renderTarget(Object layout, double length3D , GrayF32 image, List<Point2D_F64> points2D) {
		ConfigCircleHexagonalGrid config = (ConfigCircleHexagonalGrid)layout;


		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(30,1);

		double radiusPixels = 20;
		double centerDistancePixels = 2*radiusPixels*config.centerDistance/config.circleDiameter;

		renderer.circleHex(config.numRows,config.numCols,radiusPixels*2,centerDistancePixels);

//		ShowImages.showWindow(renderer.getBufferred(),"Rendered",true);
//		BoofMiscOps.sleep(100000);

		image.setTo(renderer.getGrayF32());
		double centerDistanceWorld = length3D*centerDistancePixels/(double)image.getWidth();

		points2D.clear();
		points2D.addAll( createLayout(config.numRows, config.numCols, centerDistanceWorld ));
	}

	@Override
	public DetectorFiducialCalibration createDetector(Object layout) {
		return FactoryFiducialCalibration.circleHexagonalGrid((ConfigCircleHexagonalGrid)layout);
	}

}
