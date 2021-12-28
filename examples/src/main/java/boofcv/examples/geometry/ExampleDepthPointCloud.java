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

package boofcv.examples.geometry;

import boofcv.alg.depth.VisualDepthOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Example of how to create a point cloud from a RGB-D (Kinect) sensor. Data is loaded from two files, one for the
 * visual image and one for the depth image.
 *
 * @author Peter Abeles
 */
public class ExampleDepthPointCloud {
	public static void main( String[] args ) {
		String nameRgb = UtilIO.pathExample("kinect/basket/basket_rgb.png");
		String nameDepth = UtilIO.pathExample("kinect/basket/basket_depth.png");
		String nameCalib = UtilIO.pathExample("kinect/basket/visualdepth.yaml");

		VisualDepthParameters param = CalibrationIO.load(nameCalib);

		BufferedImage buffered = UtilImageIO.loadImageNotNull(nameRgb);
		Planar<GrayU8> rgb = ConvertBufferedImage.convertFromPlanar(buffered, null, true, GrayU8.class);
		GrayU16 depth = ConvertBufferedImage.convertFrom(UtilImageIO.loadImageNotNull(nameDepth), null, GrayU16.class);

		var cloud = new DogArray<>(Point3D_F64::new);
		var cloudColor = new DogArray<>(() -> new int[3]);

		VisualDepthOps.depthTo3D(param.visualParam, rgb, depth, cloud, cloudColor);

		PointCloudViewer viewer = VisualizeData.createPointCloudViewer();
		viewer.setCameraHFov(PerspectiveOps.computeHFov(param.visualParam));
		viewer.setTranslationStep(15);

		for (int i = 0; i < cloud.size; i++) {
			Point3D_F64 p = cloud.get(i);
			int[] color = cloudColor.get(i);
			int c = (color[0] << 16) | (color[1] << 8) | color[2];
			viewer.addPoint(p.x, p.y, p.z, c);
		}
		viewer.getComponent().setPreferredSize(new Dimension(rgb.width, rgb.height));

		// ---------- Display depth image
		// use the actual max value in the image to maximize its appearance
		int maxValue = ImageStatistics.max(depth);
		BufferedImage depthOut = VisualizeImageData.disparity(depth, null, maxValue, 0);
		ShowImages.showWindow(depthOut, "Depth Image", true);

		// ---------- Display colorized point cloud
		ShowImages.showWindow(viewer.getComponent(), "Point Cloud", true);
		System.out.println("Total points = " + cloud.size);
	}
}
