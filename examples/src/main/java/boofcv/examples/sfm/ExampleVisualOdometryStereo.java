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
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.sfm.ConfigStereoMonoTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.io.File;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a stereo camera system. Additional
 * information on the scene can be optionally extracted from the algorithm if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleVisualOdometryStereo {
	public static void main( String[] args ) {
		MediaManager media = DefaultMediaManager.INSTANCE;

		String directory = UtilIO.pathExample("vo/backyard/");

		// load camera description and the video sequence
		StereoParameters stereoParam = CalibrationIO.load(media.openFile(new File(directory, "stereo.yaml").getPath()));
		SimpleImageSequence<GrayU8> video1 = media.openVideo(
				new File(directory, "left.mjpeg").getPath(), ImageType.single(GrayU8.class));
		SimpleImageSequence<GrayU8> video2 = media.openVideo(
				new File(directory, "right.mjpeg").getPath(), ImageType.single(GrayU8.class));

		var config = new ConfigStereoMonoTrackPnP();

		// Specify which tracker and how it will behave
		config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		config.tracker.klt.pyramidLevels = ConfigDiscreteLevels.levels(4);
		config.tracker.klt.templateRadius = 4;
		config.tracker.klt.toleranceFB = 3;
		config.tracker.klt.pruneClose = true;

		config.tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.tracker.detDesc.detectPoint.shiTomasi.radius = 4;
		config.tracker.detDesc.detectPoint.general.maxFeatures = 300;
		config.tracker.detDesc.detectPoint.general.radius = 5;

		// We will estimate the location of features using block matching stereo
		config.disparity.errorType = DisparityError.CENSUS;
		config.disparity.disparityMin = 0;
		config.disparity.disparityRange = 50;
		config.disparity.regionRadiusX = 3;
		config.disparity.regionRadiusY = 3;
		config.disparity.maxPerPixelError = 30;
		config.disparity.texture = 0.05;
		config.disparity.validateRtoL = 1;
		config.disparity.subpixel = true;

		// Configurations related to how the structure is chained together frame to frame
		config.scene.keyframes.geoMinCoverage = 0.4;
		config.scene.ransac.iterations = 200;
		config.scene.ransac.inlierThreshold = 1.0;

		// Declare each component then visual odometry
		StereoVisualOdometry<GrayU8> visodom = FactoryVisualOdometry.stereoMonoPnP(config, GrayU8.class);

		// Optionally dump verbose debugging information to stdout
//		visodom.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RUNTIME, VisualOdometry.VERBOSE_TRACKING));

		// Pass in intrinsic/extrinsic calibration. This can be changed in the future.
		visodom.setCalibration(stereoParam);

		// Process the video sequence and output the location plus number of inliers
		long startTime = System.nanoTime();
		while (video1.hasNext()) {
			GrayU8 left = video1.next();
			GrayU8 right = video2.next();

			if (!visodom.process(left, right)) {
				throw new RuntimeException("VO Failed!");
			}

			Se3_F64 leftToWorld = visodom.getCameraToWorld();
			Vector3D_F64 T = leftToWorld.getT();

			System.out.printf("Location %8.2f %8.2f %8.2f, %s\n", T.x, T.y, T.z, trackStats(visodom));
		}
		System.out.printf("FPS %4.2f\n", video1.getFrameNumber()/((System.nanoTime() - startTime)*1e-9));
	}

	/**
	 * If the algorithm implements AccessPointTracks3D create a string which summarizing different tracking information
	 */
	public static String trackStats( VisualOdometry alg ) {
		if (!(alg instanceof AccessPointTracks3D))
			return "";

		AccessPointTracks3D access = (AccessPointTracks3D)alg;

		int N = access.getTotalTracks();
		int totalInliers = 0;
		int totalNew = 0;
		for (int i = 0; i < N; i++) {
			if (access.isTrackInlier(i))
				totalInliers++;

			if (access.isTrackNew(i))
				totalNew++;
		}

		return String.format("inlier: %5.1f%% new %4d total %d", 100.0*totalInliers/N, totalNew, N);
	}
}
