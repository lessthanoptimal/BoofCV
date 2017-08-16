/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.depth.VisualDepthOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.gui.d3.PointCloudViewer;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Loads kinect data from two files and displays the cloud in a 3D simple viewer.
 *
 * @author Peter Abeles
 */
public class DisplayKinectPointCloudApp {

	public static void main( String args[] ) throws IOException {
		String baseDir = UtilIO.pathExample("kinect/basket");

		String nameRgb = "basket_rgb.png";
		String nameDepth = "basket_depth.png";
		String nameCalib = "intrinsic.yaml";

		CameraPinholeRadial param = CalibrationIO.load(new File(baseDir,nameCalib));

		GrayU16 depth = UtilImageIO.loadImage(new File(baseDir,nameDepth),false, ImageType.single(GrayU16.class));
		Planar<GrayU8> rgb = UtilImageIO.loadImage(new File(baseDir,nameRgb),true, ImageType.pl(3, GrayU8.class));


		FastQueue<Point3D_F64> cloud = new FastQueue<Point3D_F64>(Point3D_F64.class,true);
		FastQueueArray_I32 cloudColor = new FastQueueArray_I32(3);

		VisualDepthOps.depthTo3D(param, rgb, depth, cloud, cloudColor);

		DMatrixRMaj K = PerspectiveOps.calibrationMatrix(param, (DMatrixRMaj)null);

		PointCloudViewer viewer = new PointCloudViewer(K, 10.0);
		viewer.setPreferredSize(new Dimension(rgb.width,rgb.height));

		for( int i = 0; i < cloud.size; i++ ) {
			Point3D_F64 p = cloud.get(i);
			int[] color = cloudColor.get(i);
			int c = (color[0] << 16 ) | (color[1] << 8) | color[2];
			viewer.addPoint(p.x,p.y,p.z,c);
		}

		ShowImages.showWindow(viewer,"Point Cloud", true);
		System.out.println("Total points = "+cloud.size);

//		BufferedImage depthOut = VisualizeImageData.disparity(depth, null, 0, UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE, 0);
//		ShowImages.showWindow(depthOut,"Depth Image", true);
	}
}
