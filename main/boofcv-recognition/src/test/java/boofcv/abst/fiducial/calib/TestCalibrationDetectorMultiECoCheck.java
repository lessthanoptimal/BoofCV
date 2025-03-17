/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.calibration.DetectMultiFiducialCalibration;
import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckGenerator;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.GridShape;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCalibrationDetectorMultiECoCheck extends GenericDetectMultiFiducialCalibrationChecks {

	ConfigECoCheckDetector configDetector = new ConfigECoCheckDetector();
	ConfigECoCheckMarkers configMarkers = ConfigECoCheckMarkers.singleShape(4, 5, 4, 0.05);
	ECoCheckUtils utils = new ECoCheckUtils();

	public TestCalibrationDetectorMultiECoCheck() {
		configMarkers.convertToGridList(utils.markers);
		utils.dataBorderFraction = configMarkers.dataBorderFraction;
		utils.dataBitWidthFraction = configMarkers.dataBitWidthFraction;
		utils.fixate();
//		visualizeFailures = true;
	}

	@Override public DetectMultiFiducialCalibration createDetector() {
		return FactoryFiducialCalibration.ecocheck(configDetector, configMarkers);
	}

	@Override public GrayF32 renderPattern( int marker, List<PointIndex2D_F64> calibrationPoints ) {
		GridShape shape = utils.markers.get(marker);
		int squareLength = 60;

		var engine = new FiducialImageEngine();
		engine.configure(20, squareLength*(shape.cols - 1), squareLength*(shape.rows - 1));
		var generator = new ECoCheckGenerator(utils);
		generator.squareWidth = squareLength;
		generator.setRender(engine);
		generator.render(marker);

		BoofMiscOps.forIdx(generator.corners, ( idx, c ) -> calibrationPoints.add(new PointIndex2D_F64(c.x, c.y, idx)));

		return engine.getGrayF32();
	}

	@Test void getLayout_duplicate() {
		configMarkers.markerShapes.reset();
		configMarkers.firstTargetDuplicated = 1;
		configMarkers.markerShapes.add(new ConfigECoCheckMarkers.MarkerShape(3,3,1));
		configMarkers.markerShapes.add(new ConfigECoCheckMarkers.MarkerShape(3,3,2));
		configMarkers.markerShapes.add(new ConfigECoCheckMarkers.MarkerShape(3,3,4));
		configMarkers.checkValidity();

		DetectMultiFiducialCalibration alg = createDetector();

		checkLayout(alg.getLayout(0), 1.0);
		checkLayout(alg.getLayout(1), 2.0);
		checkLayout(alg.getLayout(2), 4.0);

		// Now test with duplicates
		configMarkers.firstTargetDuplicated = 2;
		configMarkers.checkValidity();

		alg = createDetector();

		checkLayout(alg.getLayout(0), 1.0);
		checkLayout(alg.getLayout(1), 1.0);
		checkLayout(alg.getLayout(2), 2.0);
		checkLayout(alg.getLayout(3), 4.0);
	}

	private void checkLayout( List<Point2D_F64> layout, double expected) {
		assertEquals(expected, layout.get(0).distance(layout.get(1)), 1e-8);
	}
}
