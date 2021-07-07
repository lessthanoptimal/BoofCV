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

package boofcv.examples.pointclouds;

import boofcv.alg.cloud.PointCloudWriter;
import boofcv.gui.PointCloudViewerPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.points.PointCloudIO;
import boofcv.struct.Point3dRgbI_F32;
import boofcv.visualize.PointCloudViewer;
import georegression.metric.UtilAngle;
import georegression.struct.ConvertFloatType;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Shows you how to open a point cloud file and display it in a simple window.
 *
 * @author Peter Abeles
 */
public class ExampleViewPointCloud {
	public static void main( String[] args ) throws IOException {
		String filePath = UtilIO.pathExample("mvs/stone_sign.ply");

		// Load the PLY file
		var cloud = new DogArray<>(Point3dRgbI_F32::new);
		PointCloudIO.load(PointCloudIO.Format.PLY, new FileInputStream(filePath), PointCloudWriter.wrapF32RGB(cloud));

		System.out.println("Total Points " + cloud.size);

		// Create the 3D viewer as a Swing panel that will have some minimal controls for adjusting the clouds
		// appearance. Since a software render is used it will get a bit sluggish (on my computer)
		// around 1,000,000 points
		PointCloudViewerPanel viewerPanel = new PointCloudViewerPanel();
		viewerPanel.setPreferredSize(new Dimension(800, 600));

		PointCloudViewer viewer = viewerPanel.getViewer();
		// Change the camera's Field-of-View
		viewer.setCameraHFov(UtilAngle.radian(60));
		// So many formats to store a 3D point and color that a functional API is used here
		viewer.addCloud(
				( idx, p ) -> ConvertFloatType.convert(cloud.get(idx), p),
				( idx ) -> cloud.get(idx).rgb, cloud.size);

		// There are a ton of options for the viewer, but we will let the GUI handle most of them
		// Alternatively, you could use VisualizeData.createPointCloudViewer(). No controls are
		// provided if you use that.

		SwingUtilities.invokeLater(() -> {
			viewerPanel.handleControlChange();
			viewerPanel.repaint();
			ShowImages.showWindow(viewerPanel, "Point Cloud", true);
		});
	}
}
