/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.calibration;

import boofcv.abst.fiducial.calib.CalibrationDetectorMultiECoCheck;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.SynchronizedCalObs;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.MultiCameraCalibParams;
import boofcv.struct.image.GrayF32;

import java.util.List;

/**
 * Demonstrates calibration of a N-camera system. This could be a three camera stereo system, 30 cameras in a ring,
 * or any similar variant.
 *
 * @author Peter Abeles
 */
public class ExampleCalibrateMulti {
	public static void main( String[] args ) {
		// Creates a detector and specifies its physical characteristics
		// Size units are not explicitly specified. You just need to be consistent.
		CalibrationDetectorMultiECoCheck detector = FactoryFiducialCalibration.ecocheck(null,
				ConfigECoCheckMarkers.parse("14x10n1", /* square size */1.0));

		String directory = UtilIO.pathExample("calibration/trinocular/");

		// Images for each camera are in their own directory
		List<String> left = UtilIO.listSmartImages(directory + "left", true);
		List<String> middle = UtilIO.listSmartImages(directory + "middle", true);
		List<String> right = UtilIO.listSmartImages(directory + "right", true);

		// Configure the calibration class for this scenario
		var calibrator = new CalibrateMultiPlanar();

		// We want to see a histogram with smaller reprojection errors than the default
		calibrator.setSummaryThresholds(new double[]{0.25, 0.5, 1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 5.0});

		// Tell it what type of camera model to use
		calibrator.getCalibratorMono().configurePinhole(true, 3, false);
		calibrator.initialize(/*num cameras*/3, /*num targets*/ detector.getTotalUniqueMarkers());
		calibrator.setTargetLayouts(detector.getLayouts());
		calibrator.setCameraProperties(0, 1224, 1024);
		calibrator.setCameraProperties(1, 1224, 1024);
		calibrator.setCameraProperties(2, 1224, 1024);

		for (int imageIdx = 0; imageIdx < left.size(); imageIdx++) {
			System.out.print("image set " + imageIdx + ", landmark count:");
			// Detect calibration targets and save results into a synchronized frame. It's assumed that each
			// set of images was taken at the exact moment in time
			GrayF32 imageLeft = UtilImageIO.loadImage(left.get(imageIdx), GrayF32.class);
			GrayF32 imageMiddle = UtilImageIO.loadImage(middle.get(imageIdx), GrayF32.class);
			GrayF32 imageRight = UtilImageIO.loadImage(right.get(imageIdx), GrayF32.class);

			var syncObs = new SynchronizedCalObs();
			addCameraObservations(0, imageLeft, detector, syncObs);
			addCameraObservations(1, imageMiddle, detector, syncObs);
			addCameraObservations(2, imageRight, detector, syncObs);
			System.out.println();

			calibrator.addObservation(syncObs);
		}

		System.out.println("Performing calibration");

		// Print out optimization results. Can help you see if something has gone wrong
		calibrator.getBundleUtils().sba.setVerbose(System.out, null);
		BoofMiscOps.checkTrue(calibrator.process(), "Calibration Failed!");

		System.out.println();
		System.out.println(calibrator.computeQualityText(false));

		MultiCameraCalibParams params = calibrator.getResults();
		CalibrationIO.save(params, "multi_camera.yaml");
		System.out.println(params.toStringFormat());
	}

	private static void addCameraObservations( int cameraID, GrayF32 image,
											   CalibrationDetectorMultiECoCheck detector, SynchronizedCalObs dst ) {

		// Find calibration targets inside the image
		detector.process(image);

		// Find the target which matches the expected target ID
		var set = dst.cameras.grow();
		set.cameraID = cameraID;
		for (int i = 0; i < detector.getDetectionCount(); i++) {
			CalibrationObservation o = detector.getDetectedPoints(i);
			if (o.target != 0)
				continue;
			set.targets.grow().setTo(o);
			break;
		}

		// Print number of calibration points it found
		System.out.print(" " + set.targets.getTail().points.size());
	}
}
