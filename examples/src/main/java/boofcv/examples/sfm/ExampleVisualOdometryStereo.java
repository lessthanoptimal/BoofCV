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

import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.sfm.ConfigVisOdomTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayS16;
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

		// Specify which tracker and how it will behave
		var configKlt = new ConfigPKlt();
		configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);
		configKlt.templateRadius = 4;
		configKlt.toleranceFB = 3;
		configKlt.pruneClose = true;

		var configDet = new ConfigPointDetector();
		configDet.type = PointDetectorTypes.SHI_TOMASI;
		configDet.shiTomasi.radius = 4;
		configDet.general.maxFeatures = 300;
		configDet.general.radius = 5;

		// We will estimate the location of features using block matching stereo
		var configBM = new ConfigDisparityBM();
		configBM.errorType = DisparityError.CENSUS;
		configBM.disparityMin = 0;
		configBM.disparityRange = 50;
		configBM.regionRadiusX = 3;
		configBM.regionRadiusY = 3;
		configBM.maxPerPixelError = 30;
		configBM.texture = 0.05;
		configBM.validateRtoL = 1;
		configBM.subpixel = true;

		// Configurations related to how the structure is chained together frame to frame
		var configVisOdom = new ConfigVisOdomTrackPnP();
		configVisOdom.keyframes.geoMinCoverage = 0.4;
		configVisOdom.ransac.iterations = 200;
		configVisOdom.ransac.inlierThreshold = 1.0;

		// Declare each component then visual odometry
		PointTracker<GrayU8> tracker = FactoryPointTracker.klt(configKlt, configDet, GrayU8.class, GrayS16.class);
		StereoDisparitySparse<GrayU8> disparity = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayU8.class);
		StereoVisualOdometry<GrayU8> visodom = FactoryVisualOdometry.stereoMonoPnP(configVisOdom, disparity, tracker, GrayU8.class);

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
