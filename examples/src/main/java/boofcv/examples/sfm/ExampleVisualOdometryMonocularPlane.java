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

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.d3.MonocularPlaneVisualOdometry;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.FactoryPointTracker;
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

		// specify how the image features are going to be tracked
		ConfigPKlt configKlt = new ConfigPKlt();
		configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);
		configKlt.templateRadius = 3;
		ConfigPointDetector configDetector = new ConfigPointDetector();
		configDetector.type = PointDetectorTypes.SHI_TOMASI;
		configDetector.general.maxFeatures = 600;
		configDetector.general.radius = 3;
		configDetector.general.threshold = 1;

		PointTracker<GrayU8> tracker = FactoryPointTracker.klt(configKlt, configDetector, GrayU8.class, null);

		// declares the algorithm
		MonocularPlaneVisualOdometry<GrayU8> visualOdometry =
				FactoryVisualOdometry.monoPlaneInfinity(75, 2, 1.5, 200, tracker, ImageType.single(GrayU8.class));

		// Pass in intrinsic/extrinsic calibration. This can be changed in the future.
		visualOdometry.setCalibration(calibration);

		// Process the video sequence and output the location plus number of inliers
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
	}
}
