/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.MonocularPlaneVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a single camera and a known
 * plane. Additional information on the scene can be optionally extracted from the algorithm,
 * if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleVisualOdometryMonocularPlane {

	public static void main( String args[] ) {

		MediaManager media = DefaultMediaManager.INSTANCE;

		String directory = "../data/applet/vo/drc/";

		// load camera description and the video sequence
		MonoPlaneParameters calibration = UtilIO.loadXML(media.openFile(directory + "mono_plane.xml"));
		SimpleImageSequence<ImageUInt8> video = media.openVideo(directory + "left.mjpeg", ImageType.single(ImageUInt8.class));

		// specify how the image features are going to be tracked
		PkltConfig configKlt = new PkltConfig();
		configKlt.pyramidScaling = new int[]{1, 2, 4, 8};
		configKlt.templateRadius = 3;
		ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600,3,1);

		PointTracker<ImageUInt8> tracker = FactoryPointTracker.klt(configKlt, configDetector, ImageUInt8.class, null);

		// declares the algorithm
		MonocularPlaneVisualOdometry<ImageUInt8> visualOdometry =
				FactoryVisualOdometry.monoPlaneInfinity(75, 2, 1.5, 200, tracker, ImageType.single(ImageUInt8.class));

		// Pass in intrinsic/extrinsic calibration.  This can be changed in the future.
		visualOdometry.setCalibration(calibration);

		// Process the video sequence and output the location plus number of inliers
		while( video.hasNext() ) {
			ImageUInt8 image = video.next();

			if( !visualOdometry.process(image) ) {
				System.out.println("Fault!");
				visualOdometry.reset();
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
	public static String inlierPercent(VisualOdometry<?> alg) {
		if( !(alg instanceof AccessPointTracks3D))
			return "";

		AccessPointTracks3D access = (AccessPointTracks3D)alg;

		int count = 0;
		int N = access.getAllTracks().size();
		for( int i = 0; i < N; i++ ) {
			if( access.isInlier(i) )
				count++;
		}

		return String.format("%%%5.3f", 100.0 * count / N);
	}
}
