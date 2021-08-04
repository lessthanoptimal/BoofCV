/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class GenericDetectMultiFiducialCalibrationChecks extends BoofStandardJUnit {

	protected boolean visualizeFailures = false;

	protected double simulatedTargetWidth = 0.3;

	public abstract DetectMultiFiducialCalibration createDetector();

	/**
	 * Renders a distortion free image of the calibration pattern and adds the location of 2D points on the pattern.
	 * 2D coordinates will be in the fiducial's reference frame.
	 */
	public abstract GrayF32 renderPattern( int marker, List<PointIndex2D_F64> calibrationPoints );

	@Test void basicPinhole() {
		DetectMultiFiducialCalibration detector = createDetector();

//		CameraPinholeBrown model = CalibrationIO.load(getClass().getResource("pinhole_radial.yaml"));
		CameraPinholeBrown model = new CameraPinholeBrown().fsetK(600,600,0,400,400,800,800);
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		DogArray<List<PointIndex2D_F64>> markerPoints = new DogArray<>(ArrayList::new);
		for (int i = 0; i < Math.min(2, detector.getTotalUniqueMarkers()); i++) {
			GrayF32 pattern = renderPattern(i, markerPoints.grow());
			Se3_F64 markerToWorld = SpecialEuclideanOps_F64.eulerXyz((-0.5+i)*0.32,0,.5,0,Math.PI,0, null);
			simulator.addSurface(markerToWorld, simulatedTargetWidth, pattern);
		}

		// Rotate the camera around and see if the change in orientation messes things up. It shouldn't for multi
		// marker systems since that requires encoding that should remove that ambiguity
		for (int cameraOriIdx = 0; cameraOriIdx < 10; cameraOriIdx++) {
			double roll = 2.0*Math.PI*cameraOriIdx/10;
			simulator.setWorldToCamera(SpecialEuclideanOps_F64.eulerXyz(0,0,0,0,0,roll,null));
			simulator.render();

			// Process the image and see if it detected everything
			detector.process(simulator.getOutput());

//			ShowImages.showWindow(simulator.getOutput(), "Rotated "+cameraOriIdx);
//			BoofMiscOps.sleep(2_000);

			// Number of markers which are not anonymous
			int totalIdentified = 0;
			for (int i = 0; i < detector.getDetectionCount(); i++) {
				if (detector.getMarkerID(i) >= 0)
					totalIdentified++;
			}

			if (visualizeFailures && 2 != totalIdentified) {
				UtilImageIO.saveImage(simulator.getOutput(), "failed.png");
				ShowImages.showWindow(simulator.getOutput(), "Simulated");
				BoofMiscOps.sleep(10_000);
			}
			assertEquals(2, totalIdentified);

			for (int i = 0; i < detector.getDetectionCount(); i++) {
				int markerID = detector.getMarkerID(i);

				// Ignore anonymous markers
				if (markerID < 0)
					continue;

				// See if it detected the expected number of calibration points on the marker
				assertEquals(markerPoints.get(markerID).size(), detector.getDetectedPoints(i).size());

				// TODO check the actual coordinates
			}
		}

	}
}
