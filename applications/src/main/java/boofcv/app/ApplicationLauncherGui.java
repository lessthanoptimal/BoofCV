/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.app.qrcode.CreateQrCodeGui;
import boofcv.gui.BoofLogo;
import boofcv.gui.BoofSwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * GUI for running different applications.
 *
 * @author Peter Abeles
 */
public class ApplicationLauncherGui extends JPanel {

	JFrame frame;

	public ApplicationLauncherGui() {
		SpringLayout layout = new SpringLayout();
		setLayout(layout);

		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(620,550));

		// set a border so that the window is distinctive in Windows
		setBorder(BorderFactory.createLineBorder(Color.GRAY));

		BoofLogo logo = new BoofLogo();
		logo.setPreferredSize(new Dimension(500,250));

		JButton bCreateQR = createButton("QR Code",CreateQrCodeGui::new);
		JButton bCreateCalib = createButton("Calibration",CreateCalibrationTargetGui::new);
		JButton bCreateFidBin = createButton("Square Binary",CreateFiducialSquareBinaryGui::new);
		JButton bCreateFidImage = createButton("Square Image",CreateFiducialSquareImageGui::new);

		JButton bUtilCalib = createButton("Calibration",CameraCalibrationGui::new);
		JButton bUtilScanQrCode = createButton("Batch QR Code",BatchScanQrCodesGui::new);
		JButton bUtilDown = createButton("Batch Downsize",BatchDownsizeImageGui::new);
		JButton bUtilUndist = createButton("Batch Undistort",BatchRemoveLensDistortionGui::new);

		// open the man website for help
		JButton bHelp = new JButton("Help");
		bHelp.setBackground(new Color(245,245,250));
		bHelp.addActionListener(e->{
			if( Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI("https://boofcv.org/index.php?title=Applications#Utilities_and_Applications"));
				} catch (IOException | URISyntaxException e1) {
					BoofSwingUtil.warningDialog(this,e1);
				}
			} else {
				JOptionPane.showMessageDialog(this, "Sorry, can't open browser");
			}
		});

		JButton bQuit = new JButton("Quit");
		bQuit.setBackground(new Color(245,245,250));
		bQuit.addActionListener(e->System.exit(0));

		// Logo on top
		layout.putConstraint(SpringLayout.WEST, logo, 40, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, logo, -40, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.NORTH, logo, 0, SpringLayout.NORTH, this);

		JPanel panelLeft = buttonPanel("Create / Print",bCreateQR,bCreateCalib,bCreateFidBin,bCreateFidImage);
		JPanel panelRight = buttonPanel("Tools",bUtilCalib,bUtilScanQrCode,bUtilDown,bUtilUndist);

		layout.putConstraint(SpringLayout.WEST, panelLeft, 60, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, panelLeft, 0, SpringLayout.SOUTH, logo);

		layout.putConstraint(SpringLayout.EAST, panelRight, -60, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.NORTH, panelRight, 0, SpringLayout.SOUTH, logo);

		layout.putConstraint(SpringLayout.SOUTH, bHelp, -10, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, bHelp, 10, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.SOUTH, bQuit, -10, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, bQuit, 10, SpringLayout.EAST, bHelp);

		add(logo);
		add(panelLeft);
		add(panelRight);
		add(bHelp);
		add(bQuit);
		addKeyListener(new ListenQuit());

		frame = new JFrame("Application Launcher");
		frame.add(this, BorderLayout.CENTER);
		frame.setUndecorated(true);
		frame.pack();
		frame.setLocationRelativeTo(null);

		// Don't start animating it until it's shown. On windows the initial opening
		// from a jar can be very slow
		logo.radius = 1;
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				logo.animate(4000);
			}
		});

		frame.setVisible(true);
		requestFocus();
	}

	public JPanel buttonPanel( String title, JButton ...buttons ) {
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		panel.setPreferredSize(new Dimension(200,240));
		panel.setMaximumSize(panel.getPreferredSize());
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		panel.add(Box.createRigidArea(new Dimension(0,10)));
		JLabel label = new JLabel(title);
		Font font = label.getFont();
		label.setFont(new Font(font.getName(),Font.PLAIN,5*font.getSize()/3));

		label.setAlignmentX( Component.CENTER_ALIGNMENT );
		panel.add(label);
		for (int i = 0; i < buttons.length; i++) {
			panel.add(Box.createRigidArea(new Dimension(0,10)));
			panel.add(buttons[i]);
		}
		panel.add(Box.createVerticalGlue());
		return panel;
	}

	public class ListenQuit implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {}

		@Override
		public void keyPressed(KeyEvent e) {
			if( e.isAltDown() || e.isControlDown() ) {
				if( e.getKeyCode() == KeyEvent.VK_Q ) {
					System.exit(0);
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {}
	}

	private JButton createButton( String text , Runnable action ) {
		JButton b = new JButton(text);
		b.addActionListener(e->{action.run();frame.setVisible(false);});
		b.setBorder(BorderFactory.createEmptyBorder());
		b.setPreferredSize(new Dimension(200,40));
		b.setMinimumSize(b.getPreferredSize());
		b.setMaximumSize(b.getPreferredSize());
		b.setBackground(new Color(240,240,240));
		b.setAlignmentX( Component.CENTER_ALIGNMENT );
		return b;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(ApplicationLauncherGui::new);
	}
}
