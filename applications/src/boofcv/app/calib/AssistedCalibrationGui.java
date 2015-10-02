/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.app.calib;

import boofcv.gui.image.ImagePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class AssistedCalibrationGui extends JPanel {
	JLabel messageLabel;
	ImagePanel imagePanel;

	BufferedImage workImage;

	public AssistedCalibrationGui( Dimension dimension ) {
		this(dimension.width,dimension.height);
	}

	public AssistedCalibrationGui( int imageWidth , int imageHeight ) {
//		super(new GridLayout(2,1));
		super(new BorderLayout());

		Font font = new Font("Serif", Font.BOLD, 24);
		messageLabel = new JLabel();
		messageLabel.setFont(new Font("Serif", Font.BOLD, 24));
		messageLabel.setText("Initial text string to fill it up");
		messageLabel.setMinimumSize(new Dimension(imageWidth,30));
		messageLabel.setPreferredSize(new Dimension(imageWidth, 30));

		imagePanel = new ImagePanel(imageWidth,imageHeight);

		add(messageLabel,BorderLayout.NORTH);
		add(imagePanel,BorderLayout.CENTER);


		workImage = new BufferedImage(imageWidth,imageHeight,BufferedImage.TYPE_INT_BGR);
	}

	public void setMessage( final String message ) {
		if( message == null )
			return;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				messageLabel.setText(message);
				messageLabel.repaint();
//				messageLabel.invalidate();
			}
		});
	}

	public synchronized void setImage( BufferedImage image ) {
		workImage.createGraphics().drawImage(image,0,0,image.getWidth(),image.getHeight(),null);
		imagePanel.setBufferedImage(workImage);
	}
}
