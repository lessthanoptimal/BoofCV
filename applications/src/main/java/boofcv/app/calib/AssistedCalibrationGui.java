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

package boofcv.app.calib;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.calibration.UtilCalibrationGui;
import boofcv.gui.controls.CalibrationTargetPanel;
import boofcv.gui.image.ImagePanel;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * GUI for assisted calibration
 *
 * @author Peter Abeles
 */
public class AssistedCalibrationGui extends JPanel {
	JLabel messageLabel;
	ImagePanel imagePanel;

	BufferedImage workImage;
	@Getter CalibrationInfoPanel infoPanel;

	@Getter TargetConfigurePanel targetPanel = new TargetConfigurePanel();

	Runnable handleTargetChanged = () -> {};

	public AssistedCalibrationGui( Dimension dimension ) {
		this(dimension.width, Math.max(600, dimension.height));
	}

	public AssistedCalibrationGui( int imageWidth, int imageHeight ) {
		super(new BorderLayout());

		messageLabel = new JLabel();
		messageLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		messageLabel.setFont(new Font("Serif", Font.BOLD, 22));
		messageLabel.setText("Initial text string to fill it up");
		messageLabel.setMinimumSize(new Dimension(imageWidth, 30));
		messageLabel.setPreferredSize(new Dimension(imageWidth, 30));

		infoPanel = new CalibrationInfoPanel();

		imagePanel = new ImagePanel(imageWidth, imageHeight);

		handleUpdatedTarget();

		add(messageLabel, BorderLayout.NORTH);
		add(imagePanel, BorderLayout.CENTER);
		add(infoPanel, BorderLayout.EAST);
		add(targetPanel, BorderLayout.WEST);

		workImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_BGR);
	}

	private void handleUpdatedTarget() {
		BufferedImage preview = UtilCalibrationGui.renderTargetBuffered(
				targetPanel.configPanel.selected, targetPanel.configPanel.getActiveConfig(), 40);
		targetPanel.targetPreviewPanel.setImageUI(preview);
		handleTargetChanged.run();
	}

	public void setMessage( final String message ) {
		if (message == null)
			return;

		SwingUtilities.invokeLater(() -> {
			messageLabel.setText(message);
			messageLabel.repaint();
		});
	}

	public class TargetConfigurePanel extends StandardAlgConfigPanel {
		protected JLabel imageSizeLabel = new JLabel();
		protected JLabel processingTimeLabel = new JLabel();
		public CalibrationTargetPanel configPanel = new CalibrationTargetPanel(( a, b ) -> handleUpdatedTarget());
		public ImagePanel targetPreviewPanel = new ImagePanel();

		public TargetConfigurePanel() {
			targetPreviewPanel.setCentering(true);

			addLabeled(imageSizeLabel, "Resolution");
			addLabeled(processingTimeLabel, "Process Time (ms)");
			add(configPanel);
			add(targetPreviewPanel);
		}

		public void setImageSize( final int width, final int height ) {
			BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width + " x " + height));
		}

		public void setProcessingTimeS( double seconds ) {
			BoofSwingUtil.invokeNowOrLater(() -> processingTimeLabel.setText(String.format("%7.1f", (seconds*1000))));
		}
	}

	public synchronized void setImage( BufferedImage image ) {
		workImage.createGraphics().drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
		imagePanel.setImage(workImage);
	}
}
