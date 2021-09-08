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

package boofcv.demonstrations.calibration;

import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.wrapper.DefaultMediaManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Computes intrinsic camera calibration parameters from a set of calibration images. Results
 * are displayed in a window allowing their accuracy to be easily seen.
 *
 * @author Peter Abeles
 */
public class CalibrateFisheyePlanarGuiAppV2 extends BaseCalibrateCameraApp {

	// needed by ProcessThread for displaying its dialog
	JPanel owner;

	MediaManager media = DefaultMediaManager.INSTANCE;

	public CalibrateFisheyePlanarGuiAppV2( ) {
		setLayout(new BorderLayout());

		imagePanel.setPreferredSize(new Dimension(600, 720));
		this.owner = this;

		createMenuBar();
		add(configurePanel, BorderLayout.WEST);
		add(imageListPanel, BorderLayout.EAST);
		add(imagePanel, BorderLayout.CENTER);

		createAlgorithms();
	}

	public static void main( String[] args ) {
		File directory = new File(UtilIO.pathExample("calibration/fisheye/chessboard"));
//		File directory = new File(UtilIO.pathExample("calibration/fisheye/square_grid"));

		SwingUtilities.invokeLater(() -> {
			var app = new CalibrateFisheyePlanarGuiAppV2();

			JFrame frame = new JFrame("Fisheye Monocular Calibration");
			frame.add(app, BorderLayout.CENTER);
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

			app.window = frame;
			app.window.setJMenuBar(app.menuBar);

			new Thread(() -> app.processDirectory(directory)).start();
		});
	}
}
