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
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.image.GrayF32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorCircleAsymmGrid extends GenericPlanarCalibrationDetectorChecks {

	private final static ConfigCircleAsymmetricGrid config =
			new ConfigCircleAsymmetricGrid(5, 4, 10,30);

	public TestCalibrationDetectorCircleAsymmGrid() {
		width = 500;
		height = 600;
	}

	@Override
	public void renderTarget(GrayF32 original, List<CalibrationObservation> solutions) {

	}

	@Override
	public DetectorFiducialCalibration createDetector() {
		return null;
	}
}
