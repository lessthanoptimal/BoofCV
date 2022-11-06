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

import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.CalibrationObservationSet;
import boofcv.alg.geo.calibration.ScoreCalibrationFill;
import boofcv.alg.geo.calibration.SynchronizedCalObs;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.MultiCameraCalibParams;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import org.ddogleg.stats.StatisticsDogArray;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static boofcv.abst.geo.calibration.CalibrateMonoPlanar.computeQuality;

/**
 * <p> Multi camera calibration using multiple planar targets. It's assumed that all cameras are rigidly attach and
 * that the calibration targets are static. Camera[0] is always the sensor reference frame. The world
 * reference frame is defined as calibration target[0]. </p>
 *
 * Usage:
 * <ol>
 *     <li>Specify camera model via {@link #getCalibratorMono} and configure* functions</li>
 *     <li>Call {@link #initialize}</li>
 *     <li>Specify shape of all cameras {@link #setCameraProperties}</li>
 *     <li>Specify target layouts using {@link #setTargetLayout}</li>
 *     <li>Call {@link #process} perform calibration. Check results to see if it succeeded</li>
 *     <li>Get found calibration with {@link #getResults()}</li>
 * </ol>
 *
 * Algorithm overview:
 * <ol>
 *     <li>Calibrate each camera independently using monocular approach</li>
 *     <li>Estimate extrinsic relationship between cameras using common observed targets</li>
 *     <li>Estimate extrinsic relationship between sensor and world for each frame</li>
 *     <li>Run bundle adjustment to improve results</li>
 * </ol>
 *
 * <p> Internally it assumes that the targets are stationary and the camera system is moving. It will work just fine
 * if the opposite is true as these are mathematically identical. </p>
 *
 * TODO add support for multiple targets. Current version has been simplified for one target.
 *
 * @author Peter Abeles
 */
public class CalibrateMultiPlanar {
	/** Used to calibrate each camera independently */
	@Getter final CalibrateMonoPlanar calibratorMono = new CalibrateMonoPlanar();

	/** Makes bundle adjustment easier */
	@Getter MetricBundleAdjustmentUtils bundleUtils = new MetricBundleAdjustmentUtils(null, false);

	/** Storage for found calibration parameters */
	@Getter MultiCameraCalibParams results = new MultiCameraCalibParams();

	/** Calibration quality statistics */
	@Getter DogArray<CameraStatistics> statistics = new DogArray<>(CameraStatistics::new, CameraStatistics::reset);

	// Specifies locations of landmarks are calibration targets
	final FastArray<List<Point2D_F64>> layouts = new FastArray<>((Class)(ArrayList.class));

	// Monocular calibration results
	final DogArray<CameraPriors> cameras = new DogArray<>(CameraPriors::new);

	// Observations from the camera system taken at a single time step
	final List<SynchronizedCalObs> frameObs = new ArrayList<>();

	/** Must call this function first. Specifies the number of cameras and calibration targets */
	public void initialize( int numCameras, int numTargets ) {
		if (numTargets != 1)
			throw new RuntimeException("Currently only supports one target");

		statistics.reset().resize(numCameras);
		results.reset();
		layouts.resize(numTargets);
		cameras.reset().resize(numCameras);
		for (int i = 0; i < cameras.size; i++) {
			cameras.get(i).index = i;
			results.camerasToSensor.add(new Se3_F64());
		}

		frameObs.clear();
	}

	/** Specifies the shape o images from a specific camera */
	public void setCameraProperties( int which, int width, int height ) {
		cameras.get(which).width = width;
		cameras.get(which).height = height;
	}

	/** Specifies location of calibration points for a target. */
	public void setTargetLayout( int which, List<Point2D_F64> layout ) {
		layouts.set(which, layout);
	}

	/**
	 * Adds an observation. Order does not matter. All cameras are assumed to have synchronized shutters
	 * and observed the world at the exact same time.
	 *
	 * @param observations Observed calibration targets in a single fram.
	 */
	public void addObservation( SynchronizedCalObs observations ) {
		frameObs.add(observations);
	}

	/**
	 * Processes the inputs and estimates the camera system's intrinsic and extrinsic calibration. If true is
	 * returned, then {@link #getResults()} will return the found calibration.
	 *
	 * @return true if successful or false if it failed
	 */
	public boolean process() {
		// Assume there's only one target for now. This should be changed in the future
		int targetID = 0;

		var frames = new ArrayList<FrameState>();
		for (int i = 0; i < frameObs.size(); i++) {
			frames.add(new FrameState());
		}

		// Do monocular calibration first
		monocularCalibration(targetID, frames);

		// Find extrinsic relationship between all the cameras
		estimateCameraToSensor(frames);

		// Find relationship of sensor reference frame to world reference frame
		estimateSensorToWorldInAllFrames(frames);

		setupSbaScene(targetID, frames);

		if (!bundleUtils.process())
			return false;

		sbaToOutput();
		computeReprojectionErrors();
		return true;
	}

	/**
	 * Calibrate each camera independently. Save extrinsic relationship between targets and each camera
	 * in all the views
	 *
	 * @param targetID Which target is going to be used to calibrate the cameras
	 * @param frames All the time step frames
	 */
	void monocularCalibration( int targetID, List<FrameState> frames ) {
		// used to compute calibration quality metrics
		var fillScore = new ScoreCalibrationFill();

		// Store the index of each frame that had observations from a camera and the specified target
		var usedFrames = new DogArray_I32();

		// Go through all cameras
		for (int cameraIdx = 0; cameraIdx < cameras.size; cameraIdx++) {
			usedFrames.reset();
			CameraPriors c = cameras.get(cameraIdx);

			// Tell it information about the camera and target
			calibratorMono.initialize(c.width, c.height, layouts.get(targetID));

			// Go through all the data frames
			for (int frameIdx = 0; frameIdx < frameObs.size(); frameIdx++) {
				SynchronizedCalObs synch = frameObs.get(frameIdx);

				// See if this frame has observations from the target camera
				for (int camIdx = 0; camIdx < synch.cameras.size; camIdx++) {
					CalibrationObservationSet os = synch.cameras.get(cameraIdx);
					if (os.cameraID != c.index)
						continue;

					// Add the observations, record which frame it came from, escape the loop
					var monoObs = new CalibrationObservation();
					monoObs.points.addAll(os.targets.get(0).points); // NOTE: assumes only one target
					calibratorMono.addImage(monoObs);

					usedFrames.add(frameIdx);
					break;
				}
			}

			// Compute and save results
			results.getIntrinsics().add(calibratorMono.process());

			// Compute quality of image coverage
			computeQuality(calibratorMono.foundIntrinsic, fillScore, layouts.get(0),
					calibratorMono.observations, statistics.get(cameraIdx).quality);

			// Save the extrinsic relationship between the camera in each frame and the targets it observed
			for (int usedIdx = 0; usedIdx < usedFrames.size(); usedIdx++) {
				int frameID = usedFrames.get(usedIdx);
				CalibrationObservationSet obs = frameObs.get(frameID).findCamera(c.index);
				Objects.requireNonNull(obs, "BUG!");

				FrameState frame = frames.get(frameID);
				FrameCamera cam = frame.cameras.get(cameraIdx);
				if (cam == null) {
					cam = new FrameCamera();
					frame.cameras.put(cameraIdx, cam);
				}

				var target = new TargetExtrinsics();
				target.targetID = targetID;
				target.targetToCamera.setTo(calibratorMono.getTargetToView(usedIdx));
				cam.observations.add(target);
			}
		}
	}

	/**
	 * Given known target to cameras from mono calibration, determine the camera to sensor
	 * reference frame by finding cases where common cameras observed the same target. This is done by
	 * iteratively going through the list of cameras with unknown relationships to the sensor frame and seeing
	 * if they share an observation with a known camera.
	 */
	void estimateCameraToSensor( List<FrameState> frames ) {
		// Known contains cameras where the extrinsics is known
		List<CameraPriors> known = new ArrayList<>();
		// Unknown contains cameras with unknown extrinsics
		List<CameraPriors> unknown = new ArrayList<>();
		for (int i = 1; i < cameras.size; i++) {
			unknown.add(cameras.get(i));
		}

		// This effectively sets camera[0] as the origin of the sensor frame
		known.add(cameras.get(0));

		// Iterate untol it's solved or doesn't change and failed
		while (unknown.size() > 0) {
			boolean change = false;
			for (int unknownIdx = unknown.size() - 1; unknownIdx >= 0; unknownIdx--) {
				CameraPriors unknownCam = unknown.get(unknownIdx);

				// Go through each frame and see it can determine all the views
				solved:
				for (int frameIdx = 0; frameIdx < frames.size(); frameIdx++) {
					FrameState frame = frames.get(frameIdx);

					for (int knownIdx = 0; knownIdx < known.size(); knownIdx++) {
						CameraPriors knownCam = known.get(knownIdx);

						// Find relationship between camera and sensor frame
						Se3_F64 camI_to_sensor = extrinsicFromKnownCamera(frame, knownCam.index, unknownCam.index);
						if (camI_to_sensor == null) {
							continue;
						}
						// Save the results
						results.camerasToSensor.get(unknownCam.index).setTo(camI_to_sensor);

						// Let it know there has been a change
						change = true;

						// Update bookkeeping
						unknown.remove(unknownIdx);
						known.add(unknownCam);

						// No longer need to go through all the frames
						break solved;
					}
				}
			}

			// If there is no change, that means there are no common observatiosn and nothing more can be done
			if (!change) {
				break;
			}
		}

		if (!unknown.isEmpty())
			throw new RuntimeException("Not all cameras have known extrinsics");
	}

	/**
	 * Find the sensor to world transform for every frame. For each frame, it selects an arbitrary target
	 * observation and uses the known camera to sensor and target to world transform, to find sensor to world.
	 */
	void estimateSensorToWorldInAllFrames( List<FrameState> frames ) {
		for (int frameIdx = 0; frameIdx < frames.size(); frameIdx++) {
			FrameState f = frames.get(frameIdx);

			// If true then a transform was found for this frame
			boolean found = false;

			// Go through all cameras
			frameEscape:
			for (int camIdx = 0; camIdx < cameras.size; camIdx++) {
				FrameCamera fc = f.cameras.get(camIdx);
				if (fc == null)
					continue;

				// see if this camera has an observation that can be used
				for (int obsIdx = 0; obsIdx < fc.observations.size(); obsIdx++) {

					// NOTE: This code assumes there is only one target and that target is the world frame
					TargetExtrinsics te = fc.observations.get(obsIdx);
					Se3_F64 cameraToTarget = te.targetToCamera.invert(null);
					results.getCameraToSensor(camIdx).invertConcat(cameraToTarget, f.sensorToWorld);

					found = true;
					break frameEscape;
				}
			}

			if (!found)
				throw new RuntimeException("This frame should be removed");
		}
	}

	/**
	 * Given the initial estimates already found, create a scene graph that can be optimized by SBA
	 */
	void setupSbaScene( int targetID, List<FrameState> frames ) {
		// Refine everything with bundle adjustment
		final SceneStructureMetric structure = bundleUtils.getStructure();
		final SceneObservations observations = bundleUtils.getObservations();
		final List<Point2D_F64> layout = layouts.get(targetID);

		// There will be a view for every camera in every frame, even if a camera did not observe anything in
		// that frame as it makes the structure much easier to manage
		int totalViews = frames.size()*cameras.size;
		int totalMotions = cameras.size;

		structure.initialize(cameras.size, totalViews, totalMotions, 0, 1);

		// Configure the cameras
		for (int i = 0; i < cameras.size; i++) {
			structure.setCamera(i, false, (CameraPinholeBrown)results.getIntrinsics().get(i));
		}

		// Specify the relationships of each camera to the sensor frame, a.k.a. camera[0]
		for (int camIdx = 0; camIdx < cameras.size; camIdx++) {
			structure.addMotion(camIdx == 0, results.getCameraToSensor(camIdx).invert(null));
		}

		// Specific the views
		int viewIdx = 0;
		for (int frameIdx = 0; frameIdx < frames.size(); frameIdx++) {
			// view index of camera zero in this frame
			int cameraZeroIdx = viewIdx;
			// view for camera[0] will be relative to world coordinate system
			Se3_F64 worldToSensor = frames.get(frameIdx).sensorToWorld.invert(null);
			structure.setView(viewIdx++, 0, false, worldToSensor);

			// All the other views are relative to the camera[0] view in this frame
			for (int camIdx = 1; camIdx < cameras.size; camIdx++) {
				structure.setView(viewIdx++, camIdx, camIdx, cameraZeroIdx);
			}
		}

		// NOTE: Only one target is currently allowed and it's the world of the world frame
		structure.setRigid(0, true, new Se3_F64(), layout.size());
		SceneStructureMetric.Rigid rigid = structure.rigids.data[0];
		for (int i = 0; i < layout.size(); i++) {
			rigid.setPoint(i, layout.get(i).x, layout.get(i).y, 0);
		}

		// Add observations to bundle adjustment
		observations.initialize(totalViews, true);
		for (int frameIdx = 0; frameIdx < frameObs.size(); frameIdx++) {
			SynchronizedCalObs f = frameObs.get(frameIdx);

			for (int camIdx = 0; camIdx < f.cameras.size; camIdx++) {
				CalibrationObservationSet c = f.cameras.get(camIdx);

				// Remember, one view for every camera in every frame
				int sbaViewIndex = frameIdx*cameras.size + c.cameraID;
				SceneObservations.View sbaView = observations.getViewRigid(sbaViewIndex);

				for (int obsIdx = 0; obsIdx < c.targets.size; obsIdx++) {
					CalibrationObservation o = c.targets.get(obsIdx);
					for (int featIdx = 0; featIdx < o.size(); featIdx++) {
						PointIndex2D_F64 p = o.get(featIdx);
						sbaView.add(p.index, (float)p.p.x, (float)p.p.y);
						rigid.connectPointToView(sbaViewIndex, p.index);
					}
				}
			}
		}
	}

	/**
	 * Copies over camera intrinsics and extrinsics in sensor frame to output data structure
	 */
	void sbaToOutput() {
		final SceneStructureMetric structure = bundleUtils.getStructure();

		for (int camIdx = 0; camIdx < cameras.size; camIdx++) {
			CameraPriors c = cameras.get(camIdx);
			BundlePinholeBrown bb = structure.getCameraModel(camIdx);
			BundleAdjustmentOps.convert(bb, c.width, c.height, (CameraPinholeBrown)results.intrinsics.get(camIdx));
			structure.motions.get(camIdx).parent_to_view.invert(results.camerasToSensor.get(camIdx));
		}
	}

	/**
	 * Computes reprojection errors. This is good indicator for how well the camera model fits the data.
	 */
	void computeReprojectionErrors() {
		final SceneStructureMetric structure = bundleUtils.getStructure();

		List<Point2D_F64> layout = layouts.get(0);

		var w2p = new WorldToCameraToPixel();
		var worldPt = new Point3D_F64();
		var predictedPixel = new Point2D_F64();
		var errors = new DogArray_F64();

		for (int camIdx = 0; camIdx < cameras.size; camIdx++) {
			CameraPinholeBrown intrinsics = (CameraPinholeBrown)results.intrinsics.get(camIdx);

			Se3_F64 cameraToSensor = results.getCameraToSensor(camIdx);

			// Summary statistics
			double overallMean = 0.0;
			double overallMax = 0.0;
			int totalFrames = 0;

			// Compute reprojection statistics across all frames with this camera
			for (int frameIdx = 0; frameIdx < frameObs.size(); frameIdx++) {
				// Every image will have stats to make to make it easier to process later
				var imageStats = new ImageResults(errors.size);
				statistics.get(camIdx).residuals.add(imageStats);

				CalibrationObservationSet camSet = frameObs.get(frameIdx).findCamera(camIdx);
				if (camSet == null)
					continue;
				CalibrationObservation camObs = camSet.findTarget(0);
				if (camObs == null)
					continue;

				totalFrames++;
				Se3_F64 worldToSensor = structure.motions.get(cameras.size + frameIdx).parent_to_view;
				Se3_F64 worldToCamera = worldToSensor.concat(cameraToSensor.invert(null), null);
				w2p.configure(intrinsics, worldToCamera);

				// Compute reprojection error from landmark observations on the fiducial
				errors.reset();
				double sumX = 0.0;
				double sumY = 0.0;
				for (int obsIdx = 0; obsIdx < camObs.points.size(); obsIdx++) {
					PointIndex2D_F64 o = camObs.points.get(obsIdx);
					Point2D_F64 landmarkX = layout.get(o.index);

					worldPt.x = landmarkX.x;
					worldPt.y = landmarkX.y;

					w2p.transform(worldPt, predictedPixel);
					double dx = predictedPixel.x - o.p.x;
					double dy = predictedPixel.y - o.p.y;
					errors.add(Math.sqrt(dx*dx + dy*dy));
					sumX += dx;
					sumY += dy;
				}

				// Reprojection error based statistics
				errors.sort();
				imageStats.maxError = errors.getFraction(1.0);
				imageStats.meanError = StatisticsDogArray.mean(errors);
				imageStats.biasX += sumX/camObs.points.size();
				imageStats.biasY += sumY/camObs.points.size();

				overallMean += imageStats.meanError;
				overallMax = Math.max(overallMax, imageStats.maxError);
			}

			// Save summary statis across all frames
			CameraStatistics stats = statistics.get(camIdx);
			stats.overallMax = overallMax;
			stats.overallMean = overallMean/totalFrames;
		}
	}

	/**
	 * Computes the SE3 for the unknown view using a view with a known SE3. This is done by finding a
	 * target that they both are observing.
	 *
	 * @param frame Frame that it's examining
	 * @param camId0 ID of known camera
	 * @param camId1 ID of unknown camera
	 * @return Transform from cam1 to sensor frame
	 */
	@Nullable Se3_F64 extrinsicFromKnownCamera( FrameState frame, int camId0, int camId1 ) {
		FrameCamera state0 = frame.cameras.get(camId0);
		FrameCamera state1 = frame.cameras.get(camId1);

		if (state0 == null || state1 == null)
			return null;

		for (int obsIdx0 = 0; obsIdx0 < state0.observations.size(); obsIdx0++) {
			TargetExtrinsics tgt0 = state0.observations.get(obsIdx0);

			TargetExtrinsics tgt1 = state1.findTarget(tgt0.targetID);
			if (tgt1 == null)
				continue;

			// Find the transform from world to camera using the relative transform of the common target
			// to each of the cameras;
			Se3_F64 cam0_to_cam1 = new Se3_F64();

			tgt0.targetToCamera.invertConcat(tgt1.targetToCamera, cam0_to_cam1);

			Se3_F64 cam0_to_sensor = results.getCameraToSensor(camId0);

			return cam0_to_cam1.invertConcat(cam0_to_sensor, null);
		}

		return null;
	}

	/**
	 * Summarizes calibration quality and residual errors in a human-readable text string
	 */
	public String computeQualityText() {
		var builder = new StringBuilder();
		builder.append("Calibration Quality Metrics:\n");
		for (int camId = 0; camId < statistics.size; camId++) {
			CameraStatistics cam = statistics.get(camId);
			builder.append(String.format("  camera[%d] fill_border=%5.3f fill_inner=%5.3f geometric=%5.3f\n",
					camId, cam.quality.borderFill, cam.quality.innerFill, cam.quality.geometric));
		}
		builder.append('\n');
		builder.append("Summary Residual Metrics:\n");
		for (int camId = 0; camId < statistics.size; camId++) {
			CameraStatistics cam = statistics.get(camId);
			builder.append(String.format("  camera[%3d] mean=%6.2f max=%6.2f\n", camId, cam.overallMean, cam.overallMax));
		}
		builder.append('\n');
		for (int camId = 0; camId < statistics.size; camId++) {
			builder.append("Residual Errors: Camera ").append(camId).append("\n");
			CameraStatistics cam = statistics.get(camId);

			for (int frameIdx = 0; frameIdx < cam.residuals.size(); frameIdx++) {
				ImageResults img = cam.residuals.get(frameIdx);
				builder.append(String.format("  img[%4d] mean=%6.2f max=%6.2f\n", frameIdx, img.meanError, img.maxError));
			}
			builder.append('\n');
		}

		return builder.toString();
	}

	/**
	 * Prior information provided about the camera by the user
	 */
	public static class CameraPriors {
		/** Index of the camera */
		int index;

		/** Shape of camera images */
		int width, height;
	}

	/**
	 * Calibration quality statistics
	 */
	public static class CameraStatistics {
		/** Summary of overall quality if input data for monocular calibration */
		public CalibrationQuality quality = new CalibrationQuality();

		/** Summary statistics for residual errors in each image */
		public List<ImageResults> residuals = new ArrayList<>();

		// overall statistics across all images
		public double overallMean = 0.0;
		public double overallMax = 0.0;

		public void reset() {
			quality.reset();
			residuals.clear();
			this.overallMax = 0.0;
			this.overallMean = 0.0;
		}
	}

	/**
	 * Workspace for information related to a single frame. A frame is the set of all observations from each camera
	 * at a single instance of time.
	 */
	public static class FrameState {
		/** Mapping from camera index to camera */
		TIntObjectMap<FrameCamera> cameras = new TIntObjectHashMap<>();

		/** Transform from the sensor coordinate system at this time step to the world frame */
		Se3_F64 sensorToWorld = new Se3_F64();
	}

	/**
	 * List of all observation from a camera in a frame. A camera can observe multiple targets at once.
	 */
	public static class FrameCamera {
		public List<TargetExtrinsics> observations = new ArrayList<>();

		/** Returns the observations which matches the specified targetID or null if there is no match */
		public @Nullable TargetExtrinsics findTarget( int targetID ) {
			for (int i = 0; i < observations.size(); i++) {
				if (observations.get(i).targetID == targetID)
					return observations.get(i);
			}
			return null;
		}
	}

	/**
	 * Specifies which target was observed and what the inferred transform was..
	 */
	public static class TargetExtrinsics {
		// Which target this is an observation of
		public int targetID;
		// Extrinsic relationship between target and camera at this instance
		public Se3_F64 targetToCamera = new Se3_F64();

		public TargetExtrinsics( int targetID ) {
			this.targetID = targetID;
		}

		public TargetExtrinsics() {}
	}
}
