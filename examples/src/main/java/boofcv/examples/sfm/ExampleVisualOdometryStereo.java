/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.tracker.PointTrackerTwoPass;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.FactoryPointTrackerTwoPass;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
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

	public static void main( String args[] ) {

		MediaManager media = DefaultMediaManager.INSTANCE;

		String directory = UtilIO.pathExample("vo/backyard/");

		// load camera description and the video sequence
		StereoParameters stereoParam = CalibrationIO.load(media.openFile(new File(directory , "stereo.yaml").getPath()));
		SimpleImageSequence<GrayU8> video1 = media.openVideo(
				new File(directory , "left.mjpeg").getPath(), ImageType.single(GrayU8.class));
		SimpleImageSequence<GrayU8> video2 = media.openVideo(
				new File(directory,"right.mjpeg").getPath(), ImageType.single(GrayU8.class));

		// specify how the image features are going to be tracked
		ConfigPKlt configKlt = new ConfigPKlt();
		configKlt.templateRadius = 3;

		PointTrackerTwoPass<GrayU8> tracker =
				FactoryPointTrackerTwoPass.klt(configKlt, new ConfigGeneralDetector(600, 3, 1),
						GrayU8.class, GrayS16.class);

		// computes the depth of each point
		var configBM = new ConfigDisparityBM();
		configBM.disparityMin = 0;
		configBM.disparityRange = 150;
		configBM.regionRadiusX = 3;
		configBM.regionRadiusY = 3;
		configBM.maxPerPixelError = 30;
		configBM.texture = -1;
		configBM.subpixel = true;
		StereoDisparitySparse<GrayU8> disparity = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayU8.class);

		// declares the algorithm
		StereoVisualOdometry<GrayU8> visualOdometry = FactoryVisualOdometry.stereoDepth(1.5,120, 2,200,50,true,
				disparity, tracker, GrayU8.class);

		// Pass in intrinsic/extrinsic calibration.  This can be changed in the future.
		visualOdometry.setCalibration(stereoParam);

		// Process the video sequence and output the location plus number of inliers
		while( video1.hasNext() ) {
			GrayU8 left = video1.next();
			GrayU8 right = video2.next();

			if( !visualOdometry.process(left,right) ) {
				throw new RuntimeException("VO Failed!");
			}

			Se3_F64 leftToWorld = visualOdometry.getCameraToWorld();
			Vector3D_F64 T = leftToWorld.getT();

			System.out.printf("Location %8.2f %8.2f %8.2f      inliers %s\n", T.x, T.y, T.z, inlierPercent(visualOdometry));
		}
	}

	/**
	 * If the algorithm implements AccessPointTracks3D, then count the number of inlier features
	 * and return a string.
	 */
	public static String inlierPercent(StereoVisualOdometry alg) {
		if( !(alg instanceof AccessPointTracks3D))
			return "";

		AccessPointTracks3D access = (AccessPointTracks3D)alg;

		int count = 0;
		int N = access.getTotalTracks();
		for( int i = 0; i < N; i++ ) {
			if( access.isTrackInlier(i) )
				count++;
		}

		return String.format("%%%5.3f", 100.0 * count / N);
	}
}
