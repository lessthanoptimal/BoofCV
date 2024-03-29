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
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.MultiCameraCalibParams;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.AverageRotationMatrix_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.optimization.ConfigLoss;
import org.ddogleg.optimization.ConfigNonLinearLeastSquares;
import org.ddogleg.stats.StatisticsDogArray;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static boofcv.abst.geo.calibration.CalibrateMonoPlanar.computeQuality;
import static boofcv.abst.geo.calibration.CalibrateMonoPlanar.generateReprojectionErrorHistogram;

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
	final FastArray<List<Point2D_F64>> layouts = new FastArray<>((Class)ArrayList.class);

	// Monocular calibration results
	final DogArray<CameraPriors> cameras = new DogArray<>(CameraPriors::new);

	// Observations from the camera system taken at a single time step
	final List<SynchronizedCalObs> frameObs = new ArrayList<>();

	// Stores information about each captured data frame
	final DogArray<FrameState> frames = new DogArray<>(FrameState::new, FrameState::reset);

	/** Reprojection error thresholds for histogram in performance summary */
	@Getter @Setter protected double[] summaryThresholds = new double[]{
			0.25, 0.5, 1.0, 2.0, 3.0, 5.0, 7.5, 10.0, 20.0, 50.0};

	/** Must call this function first. Specifies the number of cameras and calibration targets */
	public void initialize( int numCameras, int numTargets ) {
		statistics.reset().resize(numCameras);
		results.reset();
		layouts.resize(numTargets);
		cameras.reset().resize(numCameras);
		for (int i = 0; i < cameras.size; i++) {
			cameras.get(i).index = i;
			results.camerasToSensor.add(new Se3_F64());
		}

		frameObs.clear();

		// We will turn on a loss function with a conservative cut off point. For camera calibration
		// we should be getting sub-pixel precision
		var configSBA = new ConfigBundleAdjustment();
		configSBA.optimizer.type = ConfigNonLinearLeastSquares.Type.LEVENBERG_MARQUARDT;
		configSBA.optimizer.lm.hessianScaling = false;
		configSBA.optimizer.robustSolver = false;
		configSBA.loss.type = ConfigLoss.Type.HUBER;
		configSBA.loss.parameter = 10;

		bundleUtils.sba = FactoryMultiView.bundleSparseMetric(configSBA);

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

	public void setTargetLayouts( List<List<Point2D_F64>> layouts ) {
		this.layouts.clear();
		this.layouts.addAll(layouts);
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
		frames.reset().resize(frameObs.size());

		// Do monocular calibration first
		monocularCalibration();

		// Find extrinsic relationship between all the cameras
		estimateCameraToSensor();

		// Find relationship of sensor reference frame to world reference frame
		estimateSensorToWorldInAllFrames();

		setupSbaScene();

		if (!bundleUtils.process())
			return false;

		sbaToOutput();
		computeReprojectionErrors();
		return true;
	}

	/**
	 * Calibrate each camera independently. Save extrinsic relationship between targets and each camera
	 * in all the views
	 */
	void monocularCalibration() {
		// used to compute calibration quality metrics
		var fillScore = new ScoreCalibrationFill();

		// A frame in the mono calibrate does not correspond to the original frame because
		// observations of multiple targets in the same image gets split up into independent observations.
		// This data structure is a lookup table that goes from mono image to original frame index
		var monoViewToInputFrame = new DogArray_I32();

		// Go through all cameras
		for (int cameraIdx = 0; cameraIdx < cameras.size; cameraIdx++) {
			monoViewToInputFrame.reset();

			CameraPriors c = cameras.get(cameraIdx);

			// Tell it information about the camera and target
			calibratorMono.initialize(c.width, c.height, layouts.toList());

			// Go through all the data frames
			for (int frameIdx = 0; frameIdx < frameObs.size(); frameIdx++) {
				SynchronizedCalObs synch = frameObs.get(frameIdx);

				// See if this frame has observations from the target camera
				for (int frameCamIdx = 0; frameCamIdx < synch.cameras.size; frameCamIdx++) {
					CalibrationObservationSet os = synch.cameras.get(frameCamIdx);
					if (os.cameraID != c.index)
						continue;

					// Each calibration target observation is treated as being its own image
					for (int targetIdx = 0; targetIdx < os.targets.size; targetIdx++) {
						calibratorMono.addImage(os.targets.get(targetIdx));
						monoViewToInputFrame.add(frameIdx);
					}
					break;
				}
			}

			// Compute and save results
			results.getIntrinsics().add(calibratorMono.process());

			// Compute quality of image coverage
			computeQuality(calibratorMono.foundIntrinsic, fillScore, layouts.toList(),
					calibratorMono.observations, statistics.get(cameraIdx).quality);

			// Save the extrinsic relationship between the camera in each frame and the targets it observed
			for (int monoViewIdx = 0; monoViewIdx < monoViewToInputFrame.size(); monoViewIdx++) {

				// Look up the original frame that this target observation came from
				int frameID = monoViewToInputFrame.get(monoViewIdx);

				CalibrationObservationSet os = frameObs.get(frameID).findCamera(c.index);
				Objects.requireNonNull(os, "BUG!");

				FrameState frame = frames.get(frameID);
				FrameCamera cam = frame.cameras.get(cameraIdx);
				if (cam == null) {
					cam = new FrameCamera();
					frame.cameras.put(cameraIdx, cam);
				}

				// Get the target observation
				CalibrationObservation calObs = calibratorMono.getObservations().get(monoViewIdx);

				// Save the estimated extrinsics
				// Remember that all targets have been put at the origin of the world coordinate system
				var target = new TargetExtrinsics();
				target.targetID = calObs.target;
				target.targetToCamera.setTo(calibratorMono.getTargetToView(monoViewIdx));

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
	void estimateCameraToSensor() {
		// Known contains cameras where the extrinsics is known
		var known = new ArrayList<CameraPriors>();
		// Unknown contains cameras with unknown extrinsics
		var unknown = new ArrayList<CameraPriors>();
		for (int i = 1; i < cameras.size; i++) {
			unknown.add(cameras.get(i));
		}

		// This effectively sets camera[0] as the origin of the sensor frame
		known.add(cameras.get(0));

		// Iterate until it's solved or doesn't change and failed
		while (!unknown.isEmpty()) {
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

			// If there is no change, that means there are no common observations and nothing more can be done
			if (!change) {
				break;
			}
		}

		if (!unknown.isEmpty())
			throw new RuntimeException("Not all cameras have known extrinsics");
	}

	/**
	 * Estimates the location of a target in the world frame. This uses the current estimated camera location.
	 */
	Se3_F64 estimateWorldToTarget( int targetID ) {
		// All the estimated locations for this target
		var listTargetToWorld = new DogArray<>(Se3_F64::new);

		// Target to sensor reference frame
		var targetToSensor = new Se3_F64();

		for (int frameIdx = 0; frameIdx < frames.size(); frameIdx++) {
			FrameState frame = frames.get(frameIdx);
			frame.cameras.forEachEntry(( cameraID, frameCamera ) -> {
				for (int obsIdx = 0; obsIdx < frameCamera.observations.size(); obsIdx++) {
					TargetExtrinsics t = frameCamera.observations.get(obsIdx);
					if (t.targetID != targetID)
						continue;

					// Find transform from target to common sensor frame
					t.targetToCamera.concat(results.camerasToSensor.get(cameraID), targetToSensor);

					// Now transform it to world reference frame
					targetToSensor.concat(frame.sensorToWorld, listTargetToWorld.grow());
				}
				return true;
			});
		}

		return computeAverageSe3(listTargetToWorld.toList());
	}

	static Se3_F64 computeAverageSe3( List<Se3_F64> listTargetToWorld ) {
		if (listTargetToWorld.isEmpty()) {
			// This target has never been observed. Just return identity since what's returned doesn't matter.
			return new Se3_F64();
		}

		// Storage for the average transform
		var average = new Se3_F64();

		// Find average location
		for (int i = 0; i < listTargetToWorld.size(); i++) {
			average.T.plusIP(listTargetToWorld.get(i).T);
		}
		average.T.divideIP(listTargetToWorld.size());

		// Find average rotation matrix
		var listR = new ArrayList<DMatrixRMaj>();
		for (int i = 0; i < listTargetToWorld.size(); i++) {
			listR.add(listTargetToWorld.get(i).R);
		}
		if (!new AverageRotationMatrix_F64().process(listR, average.R)) {
			throw new RuntimeException("Average rotation computation failed");
		}

		return average;
	}

	/**
	 * Find the sensor to world transform for every frame. For each frame, it selects an arbitrary target
	 * observation and uses the known camera to sensor and target to world transform, to find sensor to world.
	 */
	void estimateSensorToWorldInAllFrames() {
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
	void setupSbaScene() {
		// Refine everything with bundle adjustment
		final SceneStructureMetric structure = bundleUtils.getStructure();
		final SceneObservations observations = bundleUtils.getObservations();

		// There will be a view for every camera in every frame, even if a camera did not observe anything in
		// that frame as it makes the structure much easier to manage
		int totalViews = frames.size()*cameras.size;
		int totalMotions = cameras.size;

		structure.initialize(cameras.size, totalViews, totalMotions, 0, layouts.size);

		// Configure the cameras
		for (int i = 0; i < cameras.size; i++) {
			structure.setCamera(i, false, (CameraPinholeBrown)results.getIntrinsics().get(i));
		}

		// Specify the relationships of each camera to the sensor frame, a.k.a. camera[0]
		for (int camIdx = 0; camIdx < cameras.size; camIdx++) {
			structure.addMotion(camIdx == 0, results.getCameraToSensor(camIdx).invert(null));
		}

		// Specify the structure of calibration targets
		for (int layoutID = 0; layoutID < layouts.size(); layoutID++) {
			List<Point2D_F64> layout = layouts.get(layoutID);

			// Use the estimated camera location to estimate the location of each target
			structure.setRigid(layoutID, false, estimateWorldToTarget(layoutID), layout.size());

			// Where the points are on the calibration target
			SceneStructureMetric.Rigid srigid = structure.rigids.get(layoutID);
			for (int i = 0; i < layout.size(); i++) {
				srigid.setPoint(i, layout.get(i).x, layout.get(i).y, 0);
			}
		}
		structure.assignIDsToRigidPoints();

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

		// Add observations to bundle adjustment
		observations.initialize(totalViews, true);
		for (int frameIdx = 0; frameIdx < frameObs.size(); frameIdx++) {
			SynchronizedCalObs f = frameObs.get(frameIdx);

			for (int camIdx = 0; camIdx < f.cameras.size; camIdx++) {
				CalibrationObservationSet c = f.cameras.get(camIdx);

				// Remember, one view for every camera in every frame
				int sbaViewIndex = frameIdx*cameras.size + c.cameraID;

				for (int targetIdx = 0; targetIdx < c.targets.size; targetIdx++) {
					CalibrationObservation obs = c.targets.get(targetIdx);
					SceneStructureMetric.Rigid srigid = structure.rigids.get(obs.target);

					for (int featIdx = 0; featIdx < obs.size(); featIdx++) {
						PointIndex2D_F64 p = obs.get(featIdx);
						srigid.connectPointToView(p.index, sbaViewIndex, (float)p.p.x, (float)p.p.y, observations);
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

		var w2p = new WorldToCameraToPixel();
		var targetPt = new Point3D_F64();
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

				// Get information about the target in this frame
				Se3_F64 targetToWorld = structure.getRigid(camObs.target).object_to_world;
				List<Point2D_F64> layout = layouts.get(camObs.target);

				// Every image will have stats to make to make it easier to process later
				var imageStats = new ImageResults(camObs.points.size());
				statistics.get(camIdx).residuals.add(imageStats);

				// Compute reprojection error from landmark observations on the fiducial
				errors.reset();
				double sumX = 0.0;
				double sumY = 0.0;
				for (int obsIdx = 0; obsIdx < camObs.points.size(); obsIdx++) {
					PointIndex2D_F64 o = camObs.points.get(obsIdx);
					Point2D_F64 landmarkX = layout.get(o.index);

					targetPt.x = landmarkX.x;
					targetPt.y = landmarkX.y;

					targetToWorld.transform(targetPt, worldPt);

					w2p.transform(worldPt, predictedPixel);
					double dx = predictedPixel.x - o.p.x;
					double dy = predictedPixel.y - o.p.y;

					double reprojectionError = Math.sqrt(dx*dx + dy*dy);
					imageStats.pointError[obsIdx] = reprojectionError;
					imageStats.residuals[obsIdx*2] = dx;
					imageStats.residuals[obsIdx*2 + 1] = dy;

					errors.add(reprojectionError);
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
	 *
	 * @param showImageStats If true it will print stats for individual images.
	 */
	public String computeQualityText( boolean showImageStats ) {
		var builder = new StringBuilder();

		// Compute a histogram of how many observations have a residual error less than these values
		var counts = new int[summaryThresholds.length];
		int totalObservations = 0;
		for (int camId = 0; camId < statistics.size; camId++) {
			CameraStatistics cam = statistics.get(camId);
			for (int imageIdx = 0; imageIdx < cam.residuals.size(); imageIdx++) {
				ImageResults r = cam.residuals.get(imageIdx);
				totalObservations += r.pointError.length;
				for (int obsIdx = 0; obsIdx < r.pointError.length; obsIdx++) {
					double e = r.pointError[obsIdx];
					for (int iterThresh = summaryThresholds.length - 1; iterThresh >= 0; iterThresh--) {
						if (summaryThresholds[iterThresh] < e)
							break;
						counts[iterThresh]++;
					}
				}
			}
		}

		builder.append("Overall Calibration Quality Metrics:\n");
		generateReprojectionErrorHistogram(summaryThresholds, counts, totalObservations, builder);

		builder.append("Camera Calibration Quality Metrics:\n");
		for (int camId = 0; camId < statistics.size; camId++) {
			CameraStatistics cam = statistics.get(camId);
			builder.append(String.format("  camera[%d] fill_border=%5.3f fill_inner=%5.3f geometric=%5.3f\n",
					camId, cam.quality.borderFill, cam.quality.innerFill, cam.quality.geometric));
		}
		builder.append('\n');
		builder.append("Camera Summary Residual Metrics:\n");
		for (int camId = 0; camId < statistics.size; camId++) {
			CameraStatistics cam = statistics.get(camId);
			builder.append(String.format("  camera[%3d] mean=%6.2f max=%6.2f\n", camId, cam.overallMean, cam.overallMax));
		}
		builder.append('\n');

		if (!showImageStats)
			return builder.toString();

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

		public void reset() {
			cameras.clear();
			sensorToWorld.reset();
		}
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
