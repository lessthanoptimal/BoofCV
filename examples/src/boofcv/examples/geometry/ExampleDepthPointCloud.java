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

package boofcv.examples.geometry;

import boofcv.alg.depth.VisualDepthOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.d3.PointCloudViewer;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Example of how to create a point cloud from a RGB-D (Kinect) sensor.  Data is loaded from two files, one for the
 * visual image and one for the depth image.
 *
 * @author Peter Abeles
 */
public class ExampleDepthPointCloud {

	public static void main( String args[] ) throws IOException {
		String baseDir = "../data/applet/kinect/basket/";

		String nameRgb = baseDir+"basket_rgb.png";
		String nameDepth = baseDir+"basket_depth.png";
		String nameCalib = baseDir+"visualdepth.xml";

		VisualDepthParameters param = UtilIO.loadXML(nameCalib);

		BufferedImage buffered = UtilImageIO.loadImage(nameRgb);
		MultiSpectral<ImageUInt8> rgb = ConvertBufferedImage.convertFromMulti(buffered,null,true,ImageUInt8.class);
		ImageUInt16 depth =
				ConvertBufferedImage.convertFrom(UtilImageIO.loadImage(nameDepth),null,ImageUInt16.class);

		FastQueue<Point3D_F64> cloud = new FastQueue<Point3D_F64>(Point3D_F64.class,true);
		FastQueueArray_I32 cloudColor = new FastQueueArray_I32(3);

		VisualDepthOps.depthTo3D(param.visualParam, rgb, depth, cloud, cloudColor);

		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(param.visualParam,null);

		PointCloudViewer viewer = new PointCloudViewer(K, 15);
		viewer.setPreferredSize(new Dimension(rgb.width,rgb.height));

		for( int i = 0; i < cloud.size; i++ ) {
			Point3D_F64 p = cloud.get(i);
			int[] color = cloudColor.get(i);
			int c = (color[0] << 16 ) | (color[1] << 8) | color[2];
			viewer.addPoint(p.x,p.y,p.z,c);
		}

		// ---------- Display depth image
		// use the actual max value in the image to maximize its appearance
		int maxValue = ImageStatistics.max(depth);
		BufferedImage depthOut = VisualizeImageData.disparity(depth, null, 0, maxValue, 0);
		ShowImages.showWindow(depthOut,"Depth Image");

		// ---------- Display colorized point cloud
		ShowImages.showWindow(viewer,"Point Cloud");
		System.out.println("Total points = "+cloud.size);
	}
}
