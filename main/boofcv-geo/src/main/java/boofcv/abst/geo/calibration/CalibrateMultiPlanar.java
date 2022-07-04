/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.calibration;

import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.ObservedCalTargets;
import boofcv.struct.calib.MultiCameraCalib;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi camera calibration using planar targets
 *
 * @author Peter Abeles
 */
public class CalibrateMultiPlanar {

	@Getter final CalibrateMonoPlanar calibratorMono = new CalibrateMonoPlanar();

	final DogArray<Camera> cameras = new DogArray<>(Camera::new);
	final List<List<Point2D_F64>> layouts = new ArrayList<>();

	MultiCameraCalib results = new MultiCameraCalib();

	public CalibrateMultiPlanar() {

	}

	public void initialize( int numCameras, int numTargets ) {
		cameras.resetResize(numCameras);
	}

	public void setCameraProperties( int which, int width , int height ) {
		cameras.get(which).width = width;
		cameras.get(which).height = height;
	}

	public void setTargetLayout( int which, List<Point2D_F64> layout ) {

	}

	/**
	 * Adds an observation. Order does not matter.
	 *
	 * @param observations Observed calibration targets in a single fram.
	 */
	public void addObservation( ObservedCalTargets observations ) {

	}

	public void process() {
		int targetID = 0;

		// Do monocular calibration first
		for (int cameraIdx = 0; (cameraIdx < cameras.size); cameraIdx++) {
			Camera c = cameras.get(cameraIdx);

			// Tell it information about the camera and target
			calibratorMono.initialize(c.width, c.height, layouts.get(targetID));

			// Pass in all the observations
			List<List<PointIndex2D_F64>> listObs = lookupObservations(cameraIdx, targetID);
			for (int obsIdx = 0; obsIdx < listObs.size(); obsIdx++) {
				var monoObs = new CalibrationObservation();
				monoObs.points = listObs.get(obsIdx);
				calibratorMono.addImage(monoObs);
			}

			// Compute and save results
			results.getIntrinsics().add(calibratorMono.process());
		}

		// TODO estimate initial extrinsics

		// TODO refine extrinsics
	}

	private List<List<PointIndex2D_F64>> lookupObservations( int camera, int target) {
		return null;
	}

	public static class Camera {
		/** Shape of camera images */
		int width, height;

		/** All observations from this camera */
		List<ObservedCalTargets> observations = new ArrayList<>();
	}

	/**
	 * Calibration quality metrics
	 */
	public static class Metrics {

	}
}
