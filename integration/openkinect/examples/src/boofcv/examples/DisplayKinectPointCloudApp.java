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

package boofcv.examples;

import boofcv.alg.depth.VisualDepthOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.gui.d3.PointCloudViewer;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;

import java.awt.*;
import java.io.IOException;

/**
 * Loads kinect data from two files and displays the cloud in a 3D simple viewer.
 *
 * @author Peter Abeles
 */
public class DisplayKinectPointCloudApp {

	public static void main( String args[] ) throws IOException {
		String baseDir = "../data/evaluation/kinect/";

		String nameRgb = baseDir+"basket.ppm";
		String nameDepth = baseDir+"basket.depth";
		String nameCalib = baseDir+"intrinsic.xml";

		IntrinsicParameters param = UtilIO.loadXML(nameCalib);

		ImageUInt16 depth = new ImageUInt16(1,1);
		MultiSpectral<ImageUInt8> rgb = new MultiSpectral<ImageUInt8>(ImageUInt8.class,1,1,3);

		UtilImageIO.loadPPM_U8(nameRgb, rgb, null);
		UtilOpenKinect.parseDepth(nameDepth,depth,null);

		FastQueue<Point3D_F64> cloud = new FastQueue<Point3D_F64>(Point3D_F64.class,true);
		FastQueueArray_I32 cloudColor = new FastQueueArray_I32(3);

		VisualDepthOps.depthTo3D(param, rgb, depth, cloud, cloudColor);

		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(param,null);

		PointCloudViewer viewer = new PointCloudViewer(K, 0.05);
		viewer.setPreferredSize(new Dimension(rgb.width,rgb.height));

		for( int i = 0; i < cloud.size; i++ ) {
			Point3D_F64 p = cloud.get(i);
			int[] color = cloudColor.get(i);
			int c = (color[0] << 16 ) | (color[1] << 8) | color[2];
			viewer.addPoint(p.x,p.y,p.z,c);
		}

		ShowImages.showWindow(viewer,"Point Cloud");
		System.out.println("Total points = "+cloud.size);

//		BufferedImage depthOut = VisualizeImageData.disparity(depth, null, 0, UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE, 0);
//		ShowImages.showWindow(depthOut,"Depth Image");
	}
}
