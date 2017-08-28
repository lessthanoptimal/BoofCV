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
import boofcv.alg.fiducial.calib.RenderSimulatedFisheye;
import boofcv.alg.fiducial.calib.RenderSquareBinaryGridFiducial;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorSquareFiducialGrid extends GenericPlanarCalibrationDetectorChecks {

	public TestCalibrationDetectorSquareFiducialGrid() {
		targetConfigs.add( new ConfigSquareGridBinary(3,4,30,30) );

		// make it bigger so that the squares are easier to decode
		simulatedTargetWidth  *= 1.2;
	}

	@Test
	public void createLayout() {
		ConfigSquareGridBinary config = new ConfigSquareGridBinary(3,4,30,20);

		List<Point2D_F64> l = createDetector(config).getLayout();

		int pointCols = config.numCols*2;
		int pointRows = config.numRows*2;

		assertEquals(pointCols*pointRows,l.size());

		double w = l.get(1).x - l.get(0).x;
		double h = l.get(0).y - l.get(pointCols).y ;

		assertEquals(30,w,1e-8);
		assertEquals(30,h,1e-8);

		double s = l.get(2).x - l.get(1).x;

		assertEquals(20, s, 1e-8);
	}

	@Override
	public void renderTarget(Object layout, double length3D , GrayF32 image, List<Point2D_F64> points2D) {
		ConfigSquareGridBinary config = (ConfigSquareGridBinary)layout;

		RenderSquareBinaryGridFiducial renderer = new RenderSquareBinaryGridFiducial();
		renderer.squareWidth = 50;

		image.setTo(renderer.generate(config.numRows,config.numCols));

		double squareWidthWorld = length3D*renderer.squareWidth/(double)image.width;
		double spacingWorld = length3D*renderer.squareWidth/(double)image.width;
		points2D.clear();
		points2D.addAll( CalibrationDetectorSquareGrid.
				createLayout(config.numRows, config.numCols, squareWidthWorld,spacingWorld ));
	}

	/**
	 * Specialized function which allows partial matches. hard to properly decode all the binary patterns at this
	 * resolution and it is designed for partial matches after all.
	 */
	@Override
	protected void checkResults(RenderSimulatedFisheye simulator, CalibrationObservation found, List<Point2D_F64> locations2D) {
		// require at least 3/4 of the corners to be detected
		if( locations2D.size() < found.size() || found.size() < locations2D.size()*3/4 ) {
			visualize(simulator, locations2D, found);
			fail("Number of detected points miss match");
		}

		Point2D_F64 truth = new Point2D_F64();

		int totalMatched = 0;
		for (int i = 0; i < locations2D.size(); i++) {
			Point2D_F64 p = locations2D.get(i);
			simulator.computePixel( 0, p.x , p.y , truth);

			// TODO ensure that the order is correct. Kinda a pain since some targets have symmetry...
			for (int j = 0; j < found.size(); j++) {
				double distance = found.get(j).distance(truth);
				if( distance <= fisheyeMatchTol ) {
					totalMatched++;
					break;
				}
			}
		}

		assertEquals(totalMatched, found.size());
	}

	@Override
	public DetectorFiducialCalibration createDetector(Object layout) {
		return FactoryFiducialCalibration.binaryGrid((ConfigSquareGridBinary)layout);
	}
}
