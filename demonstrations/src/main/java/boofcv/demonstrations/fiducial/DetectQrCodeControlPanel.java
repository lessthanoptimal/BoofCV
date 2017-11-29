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

package boofcv.demonstrations.fiducial;

import boofcv.demonstrations.shapes.DetectBlackPolygonControlPanel;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.fiducial.ConfigQrCode;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * @author Peter Abeles
 */
public class DetectQrCodeControlPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener
{
	DetectQrCodeApp owner;

	// selects which image to view
	JComboBox imageView;

	JButton bRunAgain = new JButton("Run Again");

	JCheckBox showSquares;
	JCheckBox showPositionPattern;
	JCheckBox showAlignmentPattern;
	JCheckBox showContour;

	boolean bShowSquares = true;
	boolean bShowPositionPattern = true; // show position patterns
	boolean bShowAlignmentPattern = true; // show position patterns
	boolean bShowContour = false;

	boolean bRefineGray = true;

	DetectBlackPolygonControlPanel polygonPanel;

	ConfigQrCode config = new ConfigQrCode();

	public DetectQrCodeControlPanel(DetectQrCodeApp owner) {
		this.owner = owner;

		polygonPanel = new DetectBlackPolygonControlPanel(owner,config.polygon);

		bRunAgain.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				DetectQrCodeControlPanel.this.owner.reprocessImageOnly();
			}
		});

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		showSquares = new JCheckBox("Squares");
		showSquares.addActionListener(this);
		showSquares.setSelected(bShowSquares);
		showPositionPattern = new JCheckBox("Position Pattern");
		showPositionPattern.setSelected(bShowPositionPattern);
		showPositionPattern.addActionListener(this);
		showAlignmentPattern = new JCheckBox("Alignment Pattern");
		showAlignmentPattern.setSelected(bShowAlignmentPattern);
		showAlignmentPattern.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);


		addLabeled(processingTimeLabel,"Time (ms)", this);
		addLabeled(imageSizeLabel,"Size", this);
		add(bRunAgain);
		addLabeled(imageView, "View: ", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignLeft(showSquares, this);
		addAlignLeft(showAlignmentPattern, this);
		addAlignLeft(showPositionPattern, this);
		addAlignLeft(showContour, this);
		add(polygonPanel);
		addVerticalGlue(this);
	}

	private void configureSpinnerFloat( JSpinner spinner ) {
		JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
		DecimalFormat format = editor.getFormat();
		format.setMinimumFractionDigits(3);
		format.setMinimumIntegerDigits(1);
		editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
		Dimension d = spinner.getPreferredSize();
		d.width = 60;
		spinner.setPreferredSize(d);
		spinner.addChangeListener(this);
		spinner.setMaximumSize(d);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if( e.getSource() == showSquares ) {
			bShowSquares = showSquares.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showPositionPattern ) {
			bShowPositionPattern = showPositionPattern.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showAlignmentPattern ) {
			bShowAlignmentPattern = showAlignmentPattern.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showContour ) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		}
		owner.configUpdate();
	}

	public ThresholdControlPanel getThreshold() {
		return polygonPanel.threshold;
	}

	public ConfigQrCode getConfigQr() {
		config.polygon = polygonPanel.getConfigPolygon();
		return config;
	}
}
