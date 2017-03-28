/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class CalibrationInfoPanel extends StandardAlgConfigPanel
	implements ActionListener
{
	JProgressBar geometryProgress;
	JProgressBar fillProgress;
	JProgressBar focusMeter;

	ImagePanel undistortedTemplate = new ImagePanel(80,80);
	ImagePanel undistortedView = new ImagePanel(80,80);
	BufferedImage imageView;
	BufferedImage imageTemplate;

	JButton forceSaveButton = new JButton("Debug Save");
	JButton finishedButton = new JButton("Finished");

	double focusMax = 1.0;

	// when true that means the user has requested to save the results and compute intrinsic parameters
	boolean saveRequested = false;
	boolean forceSaveImage = false;

	public CalibrationInfoPanel() {
		geometryProgress = new JProgressBar(0, 100);
		geometryProgress.setValue(0);
		geometryProgress.setStringPainted(true);

		fillProgress = new JProgressBar(0, 100);
		fillProgress.setValue(0);
		fillProgress.setStringPainted(true);

		focusMeter = new JProgressBar(0, 100);
		focusMeter.setValue(0);
		focusMeter.setStringPainted(true);

		// scale the images up
		undistortedTemplate.setScaling(ScaleOptions.ALL);
		undistortedView.setScaling(ScaleOptions.ALL);
		JPanel imageRow = new JPanel();
		imageRow.setLayout(new BoxLayout(imageRow, BoxLayout.X_AXIS));
		imageRow.add(undistortedTemplate);
		imageRow.add(Box.createRigidArea(new Dimension(10,10)));
		imageRow.add(undistortedView);
		imageRow.setMaximumSize(imageRow.getPreferredSize());

		finishedButton.addActionListener(this);
		finishedButton.setEnabled(false);

		forceSaveButton.addActionListener(this);

		addCenterLabel("Geometry", this);
		addAlignCenter(geometryProgress, this);
		addCenterLabel("Edge Fill",this);
		addAlignCenter(fillProgress, this);
		add(Box.createRigidArea(new Dimension(5, 5)));
		addAlignCenter(imageRow, this);
		addAlignCenter(focusMeter, this);
		add(Box.createRigidArea(new Dimension(5, 50)));
		add(finishedButton);
		add(Box.createVerticalGlue());
		add(forceSaveButton);
	}

	public void updateTemplate( GrayF32 image ) {
		if( imageTemplate == null ) {
			imageTemplate = new BufferedImage(image.width,image.height,BufferedImage.TYPE_INT_BGR);
		}
		ConvertBufferedImage.convertTo(image,imageTemplate);
		undistortedTemplate.setImageUI(imageTemplate);
	}

	public void updateView( GrayF32 image ) {
		if( imageView == null ) {
			imageView = new BufferedImage(image.width,image.height,BufferedImage.TYPE_INT_BGR);
		}
		ConvertBufferedImage.convertTo(image,imageView);
		undistortedView.setImageUI(imageView);
	}

	public void updateGeometry( final double process ) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				geometryProgress.setValue((int)(100*process));
			}
		});
	}

	public void updateEdgeFill(final double process) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fillProgress.setValue((int)(100*process));
			}
		});
	}

	public void updateFocusScore( final double score ) {

		this.focusMax = Math.max(focusMax,score);
		final double value = score/focusMax;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				focusMeter.setValue((int)(100*value));
			}
		});
	}

	public boolean isFinished() {
		return saveRequested;
	}

	public void enabledFinishedButton() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				finishedButton.setEnabled(true);
			}
		});
	}

	public void resetForceSave() {
		forceSaveImage = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == finishedButton) {
			saveRequested = true;
		} else if( e.getSource() == forceSaveButton ) {
			forceSaveImage = true;
		}
	}
}
