/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PkltConfig;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.DepthVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.alg.distort.DoNothingPixelTransform_F32;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.io.MediaManager;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.GrowQueue_I8;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.io.IOException;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a depth camera system, e.g. Kinect.
 * Additional information on the scene can be optionally extracted from the algorithm if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleDepthVisualOdometry {

	public static void main( String args[] ) throws IOException {

		MediaManager media = DefaultMediaManager.INSTANCE;

		String directory = "/home/pja/projects/boofcv/evaluation/log/";

		// load camera description and the video sequence
		IntrinsicParameters param = BoofMiscOps.loadXML(media.openFile(directory + "intrinsic.xml"));

		// specify how the image features are going to be tracked
		PkltConfig<ImageUInt8, ImageSInt16> configKlt = PkltConfig.createDefault(ImageUInt8.class, ImageSInt16.class);
		configKlt.pyramidScaling = new int[]{1, 2, 4, 8};
		configKlt.templateRadius = 3;

		PointTrackerTwoPass<ImageUInt8> tracker =
				FactoryPointTrackerTwoPass.klt(configKlt, new ConfigGeneralDetector(600, 3, 1));

		DepthSparse3D<ImageUInt16> sparseDepth = new DepthSparse3D.I<ImageUInt16>(1e-3);

		// declares the algorithm
		DepthVisualOdometry<ImageUInt8,ImageUInt16> visualOdometry =
				FactoryVisualOdometry.depthDepthPnP(1.5, 120, 2, 200, 50, true,
				sparseDepth, tracker, ImageUInt8.class, ImageUInt16.class);

		// Pass in intrinsic/extrinsic calibration.  This can be changed in the future.
		visualOdometry.setCalibration(param,new DoNothingPixelTransform_F32());


		// image with depth information
		ImageUInt16 depth = new ImageUInt16(1,1);
		// image with color information
		MultiSpectral<ImageUInt8> rgb = new MultiSpectral<ImageUInt8>(ImageUInt8.class,1,1,3);
		ImageUInt8 gray = new ImageUInt8(1,1);
		// work space
		GrowQueue_I8 data = new GrowQueue_I8();

		// Process the video sequence and output the location plus number of inliers
		for( int i = 0; i < 500; i++ ) {
			UtilImageIO.loadPPM_U8(String.format("%s/rgb%07d.ppm", directory, i), rgb, data);
			UtilOpenKinect.parseDepth(String.format("%s/depth%07d.depth", directory, i), depth, data);

			gray.reshape(rgb.width,rgb.height);
			ConvertImage.average(rgb,gray);

			if( !visualOdometry.process(gray,depth) ) {
				throw new RuntimeException("VO Failed!");
			}

			Se3_F64 leftToWorld = visualOdometry.getLeftToWorld();
			Vector3D_F64 T = leftToWorld.getT();

			System.out.printf("Location %8.2f %8.2f %8.2f      inliers %s\n", T.x, T.y, T.z, inlierPercent(visualOdometry));
		}
	}

	/**
	 * If the algorithm implements AccessPointTracks3D, then count the number of inlier features
	 * and return a string.
	 */
	public static String inlierPercent(VisualOdometry alg) {
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
