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

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorSquareGrid extends GenericPlanarCalibrationDetectorChecks {

	private final static ConfigSquareGrid config = new ConfigSquareGrid(3, 2, 30,30);

	public TestCalibrationDetectorSquareGrid() {
		targetConfigs.add(  new ConfigSquareGrid(3, 2, 30,30) );
		fisheyeMatchTol = 5.0;
		fisheyeAllowedFails = 1;
	}

	@Test
	public void createLayout() {
		List<Point2D_F64> l = CalibrationDetectorSquareGrid.createLayout(3, 2, 0.1, 0.2);

		assertEquals(4*6,l.size());

		double w = l.get(1).x - l.get(0).x;
		double h = l.get(0).y - l.get(4).y ;

		assertEquals(0.1,w,1e-8);
		assertEquals(0.1,h,1e-8);

		double s = l.get(2).x - l.get(1).x;

		assertEquals(0.2, s, 1e-8);
	}

	@Override
	public void renderTarget(Object layout, double length3D , GrayF32 image, List<Point2D_F64> points2D) {
		ConfigSquareGrid config = (ConfigSquareGrid)layout;

		double squareWidthPixels = 20;
		double spacingPixels = squareWidthPixels*(config.spaceWidth/config.squareWidth);

		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(20,1);
		renderer.squareGrid(config.numRows,config.numCols,squareWidthPixels,spacingPixels);

		image.setTo(renderer.getGrayF32());

		double squareWidthWorld = length3D*squareWidthPixels/(double)image.getWidth();
		double spacingWorld = length3D*spacingPixels/(double)image.getWidth();

		points2D.clear();
		points2D.addAll( CalibrationDetectorSquareGrid.
				createLayout(config.numRows, config.numCols, squareWidthWorld,spacingWorld ));
	}

	@Override
	public DetectorFiducialCalibration createDetector(Object layout) {
		return FactoryFiducialCalibration.squareGrid((ConfigSquareGrid)layout);
	}
}