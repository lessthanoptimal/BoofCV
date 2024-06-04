/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.app;

import boofcv.alg.cloud.PointCloudWriter;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.PointCloudViewerPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.points.PointCloudIO;
import georegression.geometry.UtilPoint3D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_F32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Open and view point cloud files
 *
 * @author Peter Abeles
 */
public class PointCloudViewerApp {
	// TODO set camera FOV
	// TODO better controls
	// TODO better initial state
	// TODO recent file list
	// TODO control sprite size

	PointCloudViewerPanel viewer = new PointCloudViewerPanel();
	JFrame frame;

	JMenuBar menuBar;

	public PointCloudViewerApp() {
		viewer.setPreferredSize(new Dimension(800, 600));

		menuBar = new JMenuBar();
		JMenu menuFile = new JMenu("File");
		menuFile.add(BoofSwingUtil.createMenuItem("Open File", KeyEvent.VK_O, KeyEvent.VK_O, this::openFile));
		menuFile.add(BoofSwingUtil.createMenuItem("Quit", () -> System.exit(0)));
		menuBar.add(menuFile);

		frame = ShowImages.setupWindow(viewer, "Point Cloud Viewer", true);
		frame.setJMenuBar(menuBar);
		frame.setVisible(true);
	}

	public void openFile() {
		File selected = BoofSwingUtil.fileChooser("PointCloudViewer", frame, true,
				".", null, BoofSwingUtil.FileTypes.FILES);
		if (selected == null)
			return;

		// Make it so open file can't be called twice
		BoofSwingUtil.recursiveEnable(menuBar, false);

		// Load it in a thread as to not lock the GUI
		new Thread(() -> {
			PointCloudWriter.CloudArraysF32 cloud = new PointCloudWriter.CloudArraysF32();
			try {
				InputStream input = new FileInputStream(selected);
				PointCloudIO.load(PointCloudIO.Format.PLY, input, cloud);
			} catch (IOException e) {
				BoofSwingUtil.warningDialog(frame, e);
				BoofSwingUtil.recursiveEnable(menuBar, true);
				return;
			}

			// Select an initial view location based on the point cloud
			final int N = cloud.cloudXyz.size/3;
			float x = 0, y = 0, z = 0;
			for (int i = 0; i < N; i++) {
				x += cloud.cloudXyz.data[i*3];
				y += cloud.cloudXyz.data[i*3 + 1];
				z += cloud.cloudXyz.data[i*3 + 2];
			}
			x /= N;
			y /= N;
			z /= N;

			var distances = new DogArray_F32();
			distances.resize(N);
			for (int i = 0; i < N; i++) {
				float X = cloud.cloudXyz.data[i*3];
				float Y = cloud.cloudXyz.data[i*3 + 1];
				float Z = cloud.cloudXyz.data[i*3 + 2];

				distances.data[i] = UtilPoint3D_F32.distance(x, y, z, X, Y, Z);
			}

			double hfov = UtilAngle.radian(100);

			// Used to compute how far away the camera needs to be to fil the view
			double radius = distances.getFraction(0.95);

			Se3_F64 worldToCamera = new Se3_F64();
			worldToCamera.T.setTo(0, 0, -radius/Math.tan(hfov/2.0));
			PerspectiveOps.pointAt(x, y, z, worldToCamera.R);

			// TODO pick a better method for selecting the initial step size
			distances.sort();

			double baseline = distances.getFraction(0.001);
			System.out.println("baseline = " + baseline);

			viewer.periodBaseline = baseline*10;
			viewer.translateBaseline = baseline;
			viewer.getViewer().setCameraHFov(hfov);

			SwingUtilities.invokeLater(() -> {
				viewer.handleControlChange();
				viewer.clearPoints();
				viewer.getViewer().addCloud(cloud.cloudXyz, cloud.cloudRgb);
				viewer.getViewer().setCameraToWorld(worldToCamera);
				viewer.repaint();
				BoofSwingUtil.recursiveEnable(menuBar, true);
			});
		}).start();
	}

	public static void main( String[] args ) {
		SwingUtilities.invokeLater(PointCloudViewerApp::new);
	}
}
