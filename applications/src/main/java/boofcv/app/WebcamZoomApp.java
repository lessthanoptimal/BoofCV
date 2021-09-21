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

package boofcv.app;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.webcamcapture.OpenWebcamDialog;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Application which lets you open a webcam and zoom in to it. Software zoom.
 *
 * @author Peter Abeles
 */
public class WebcamZoomApp {
	public static void main( String[] args ) {
		final OpenWebcamDialog.Selection s = OpenWebcamDialog.showDialog(null);
		if (s == null)
			return;

		int windowWidth, windowHeight;
		windowWidth = 200;
		windowHeight = 200;

		ImageZoomPanel gui = new ImageZoomPanel();
		gui.setPreferredSize(new Dimension(windowWidth, windowHeight));
		gui.getImagePanel().addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved( MouseWheelEvent e ) {
				gui.setScale(BoofSwingUtil.mouseWheelImageZoom(gui.getScale(), e));
			}
		});

		gui.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				Point2D_F64 p = gui.pixelToPoint(e.getX(), e.getY());

				gui.centerView(p.x, p.y);
				gui.requestFocus();
			}
		});

		gui.addKeyListener(new KeyAdapter() {
			boolean visible = true;

			@Override
			public void keyPressed( KeyEvent e ) {
				if (e.getKeyCode() == KeyEvent.VK_Q) {
					System.out.println("Pressed q. Exiting");
					System.exit(0);
				} else if (e.getKeyCode() == KeyEvent.VK_H) {
					visible = !visible;
					System.out.println("Toggle hide scroll bar " + visible);
					gui.setScrollbarsVisible(visible);
				}
			}
		});

		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame();
			frame.setUndecorated(true);
			frame.setAlwaysOnTop(true);
			frame.add(gui, BorderLayout.CENTER);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		});

		UtilWebcamCapture.adjustResolution(s.camera, s.width, s.height);
		if (!s.camera.open()) {
			System.err.println("Failed to open camera " + s.camera.getName());
			System.exit(-1);
		}
		while (s.camera.isOpen()) {
			BufferedImage image = s.camera.getImage();
			SwingUtilities.invokeLater(() -> {
				gui.setImage(image);
				gui.repaint();
			});
		}
	}
}
