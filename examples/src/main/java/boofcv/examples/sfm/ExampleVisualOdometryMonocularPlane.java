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

package boofcv.examples.sfm;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.d3.MonocularPlaneVisualOdometry;
import boofcv.factory.sfm.ConfigPlanarTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.io.File;

import static boofcv.examples.sfm.ExampleVisualOdometryStereo.trackStats;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a single camera and a known
 * plane. Additional information on the scene can be optionally extracted from the algorithm,
 * if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleVisualOdometryMonocularPlane {
	public static void main( String[] args ) {
		MediaManager media = DefaultMediaManager.INSTANCE;

		String directory = UtilIO.pathExample("vo/drc/");

		// load camera description and the video sequence
		MonoPlaneParameters calibration = CalibrationIO.load(
				media.openFile(new File(directory, "mono_plane.yaml").getPath()));
		SimpleImageSequence<GrayU8> video = media.openVideo(
				new File(directory, "left.mjpeg").getPath(), ImageType.single(GrayU8.class));

		var config = new ConfigPlanarTrackPnP();

		// specify how the image features are going to be tracked
		config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		config.tracker.klt.pyramidLevels = ConfigDiscreteLevels.levels(4);
		config.tracker.klt.templateRadius = 3;

		config.tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.tracker.detDesc.detectPoint.general.maxFeatures = 600;
		config.tracker.detDesc.detectPoint.general.radius = 3;
		config.tracker.detDesc.detectPoint.general.threshold = 1;

		// Configure how visual odometry works
		config.thresholdAdd = 75;
		config.thresholdRetire = 2;
		config.ransac.iterations = 200;
		config.ransac.inlierThreshold = 1.5;

		// declares the algorithm
		MonocularPlaneVisualOdometry<GrayU8> visualOdometry = FactoryVisualOdometry.monoPlaneInfinity(config, GrayU8.class);

		// Pass in intrinsic/extrinsic calibration. This can be changed in the future.
		visualOdometry.setCalibration(calibration);

		// Process the video sequence and output the location plus number of inliers
		long startTime = System.nanoTime();
		while (video.hasNext()) {
			GrayU8 image = video.next();

			if (!visualOdometry.process(image)) {
				System.out.println("Fault!");
				visualOdometry.reset();
			}

			Se3_F64 leftToWorld = visualOdometry.getCameraToWorld();
			Vector3D_F64 T = leftToWorld.getT();

			System.out.printf("Location %8.2f %8.2f %8.2f, %s\n", T.x, T.y, T.z, trackStats(visualOdometry));
		}
		System.out.printf("FPS %4.2f\n", video.getFrameNumber()/((System.nanoTime() - startTime)*1e-9));
	}
}
