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
import boofcv.abst.sfm.d3.DepthVisualOdometry;
import boofcv.factory.sfm.ConfigRgbDepthTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.io.File;

import static boofcv.examples.sfm.ExampleVisualOdometryStereo.trackStats;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a depth camera system, e.g. Kinect.
 * Additional information on the scene can be optionally extracted from the algorithm if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleVisualOdometryDepth {
	public static void main( String[] args ) {
		MediaManager media = DefaultMediaManager.INSTANCE;

		String directory = UtilIO.pathExample("kinect/straight");

		// load camera description and the video sequence
		VisualDepthParameters param = CalibrationIO.load(
				media.openFile(new File(directory, "visualdepth.yaml").getPath()));

		// specify how the image features are going to be tracked
		ConfigRgbDepthTrackPnP config = new ConfigRgbDepthTrackPnP();
		config.depthScale = 1e-3; // convert depth image distance units to meters

		config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		config.tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.tracker.detDesc.detectPoint.shiTomasi.radius = 3;
		config.tracker.detDesc.detectPoint.general.maxFeatures = 600;
		config.tracker.detDesc.detectPoint.general.radius = 3;
		config.tracker.detDesc.detectPoint.general.threshold = 1;

		// declares the algorithm
		DepthVisualOdometry<GrayU8, GrayU16> visualOdometry =
				FactoryVisualOdometry.rgbDepthPnP(config, GrayU8.class, GrayU16.class);

		// Pass in intrinsic/extrinsic calibration. This can be changed in the future.
		visualOdometry.setCalibration(param.visualParam, new DoNothing2Transform2_F32());

		// Process the video sequence and output the location plus number of inliers
		SimpleImageSequence<GrayU8> videoVisual = media.openVideo(
				new File(directory, "rgb.mjpeg").getPath(), ImageType.single(GrayU8.class));
		SimpleImageSequence<GrayU16> videoDepth = media.openVideo(
				new File(directory, "depth.mpng").getPath(), ImageType.single(GrayU16.class));

		long startTime = System.nanoTime();
		while (videoVisual.hasNext()) {
			GrayU8 visual = videoVisual.next();
			GrayU16 depth = videoDepth.next();

			if (!visualOdometry.process(visual, depth)) {
				throw new RuntimeException("VO Failed!");
			}

			Se3_F64 leftToWorld = visualOdometry.getCameraToWorld();
			Vector3D_F64 T = leftToWorld.getT();

			System.out.printf("Location %8.2f %8.2f %8.2f, %s\n", T.x, T.y, T.z, trackStats(visualOdometry));
		}
		System.out.printf("FPS %4.2f\n", videoVisual.getFrameNumber()/((System.nanoTime() - startTime)*1e-9));
	}
}
