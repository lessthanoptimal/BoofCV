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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.fiducial.calib.RenderSquareBinaryGridFiducial;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorSquareFiducialGrid extends GenericPlanarCalibrationDetectorChecks {

	private static final int numRows = 3;
	private static final int numCols = 2;

	public TestCalibrationDetectorSquareFiducialGrid() {
		width = 500;
		height = 600;
	}

	@Test
	public void createLayout() {
		List<Point2D_F64> l = createDetector().getLayout();

		int pointCols = numCols*2;
		int pointRows = numRows*2;

		assertEquals(pointCols*pointRows,l.size());

		double w = l.get(1).x - l.get(0).x;
		double h = l.get(0).y - l.get(pointCols).y ;

		assertEquals(1,w,1e-8);
		assertEquals(1,h,1e-8);

		double s = l.get(2).x - l.get(1).x;

		assertEquals(1, s, 1e-8);
	}

	@Override
	public void renderTarget(GrayF32 original, List<CalibrationObservation> solutions) {
		RenderSquareBinaryGridFiducial renderer = new RenderSquareBinaryGridFiducial();
		renderer.squareWidth = 50;

		GrayF32 rendered = renderer.generate(numRows,numCols);

		original.subimage(0,0,rendered.width,rendered.height).setTo(rendered);

		CalibrationObservation set = new CalibrationObservation();
		List<Point2D_F64> points = renderer.getOrderedExpectedPoints(numRows, numCols);
		for (int i = 0; i < points.size(); i++) {
			set.add( points.get(i), i);
		}

		solutions.add(set);
	}

	@Override
	public DetectorFiducialCalibration createDetector() {
		ConfigSquareGridBinary config = new ConfigSquareGridBinary(numRows,numCols,1.0,1.0);
		config.squareWidth = 1;
		config.spaceWidth = 1;

		return FactoryFiducialCalibration.binaryGrid(config);
	}
}
