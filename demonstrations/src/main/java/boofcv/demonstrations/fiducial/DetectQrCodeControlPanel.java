/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * @author Peter Abeles
 */
public class DetectQrCodeControlPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener
{
	DetectQrCodeApp owner;

	DetectQrCodeMessagePanel messagePanel;

	// selects which image to view
	JComboBox imageView;

	JButton bRunAgain = new JButton("(ms)");

	JSpinner spinnerMinimumVersion;
	JSpinner spinnerMaximumVersion;

	JCheckBox showMarkers;
	JCheckBox showFailures;
	JCheckBox showBits;
	JCheckBox showSquares;
	JCheckBox showPositionPattern;
	JCheckBox showAlignmentPattern;
	JCheckBox showContour;

	boolean bShowMarkers = true;
	boolean bShowBits = false;
	boolean bShowSquares = false;
	boolean bShowPositionPattern = true;
	boolean bShowAlignmentPattern = true;
	boolean bShowContour = false;
	boolean bShowFailures = true;

	DetectBlackPolygonControlPanel polygonPanel;

	ConfigQrCode config = new ConfigQrCode();

	public DetectQrCodeControlPanel(DetectQrCodeApp owner) {
		this.owner = owner;

		messagePanel = new DetectQrCodeMessagePanel(owner);

		polygonPanel = new DetectBlackPolygonControlPanel(owner,config.polygon,config.threshold);
		polygonPanel.thresholdPanel.addHistogramGraph();

		bRunAgain.addActionListener(actionEvent -> DetectQrCodeControlPanel.this.owner.reprocessImageOnly());
		bRunAgain.setMaximumSize(bRunAgain.getPreferredSize());
		bRunAgain.setBorder(BorderFactory.createEmptyBorder(1,4,1,4));

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		spinnerMinimumVersion = spinner(config.versionMinimum,1,40,1);
		spinnerMaximumVersion = spinner(config.versionMaximum,1,40,1);

		showMarkers = checkbox("Markers",bShowMarkers);
		showFailures = checkbox("Failures",bShowFailures);
		showBits = checkbox("Bits",bShowBits);
		showSquares = checkbox("Squares", bShowSquares);
		showPositionPattern = checkbox("Pos. Pattern",bShowPositionPattern);
		showAlignmentPattern = checkbox("Align. Pattern", bShowAlignmentPattern);
		showContour = checkbox("Contour",bShowContour);

		JPanel togglePanel = new JPanel( new GridLayout(0,2));
		togglePanel.add(showMarkers);
		togglePanel.add(showFailures);
		togglePanel.add(showSquares);
		togglePanel.add(showPositionPattern);
		togglePanel.add(showAlignmentPattern);
		togglePanel.add(showBits);
		togglePanel.add(showContour);
		togglePanel.setMaximumSize(togglePanel.getPreferredSize());

		JTabbedPane tabbedPanel = new JTabbedPane();
		tabbedPanel.addTab("Message", messagePanel);
		tabbedPanel.addTab("Controls", polygonPanel);

		JPanel timePanel = new JPanel();
		timePanel.setLayout(new BoxLayout(timePanel,BoxLayout.X_AXIS));
		timePanel.add(processingTimeLabel);
		timePanel.add(bRunAgain);

		addLabeled(timePanel,"Time");
		addLabeled(imageSizeLabel,"Size");
		addLabeled(imageView, "View: ");
		addLabeled(selectZoom,"Zoom");
		add(togglePanel);
		addLabeled(spinnerMinimumVersion,"Min. Version");
		addLabeled(spinnerMaximumVersion,"Max. Version");
		add(tabbedPanel);
		addVerticalGlue();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == showMarkers ) {
			bShowMarkers = showMarkers.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showFailures ) {
			bShowFailures = showFailures.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showBits ) {
			bShowBits = showBits.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == imageView ) {
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
		} else if( e.getSource() == spinnerMinimumVersion ) {
			config.versionMinimum = ((Number) spinnerMinimumVersion.getValue()).intValue();
		} else if( e.getSource() == spinnerMaximumVersion ) {
			config.versionMaximum = ((Number) spinnerMaximumVersion.getValue()).intValue();
		}
		owner.configUpdate();
	}

	public ThresholdControlPanel getThreshold() {
		return polygonPanel.thresholdPanel;
	}

	public ConfigQrCode getConfigQr() {
		config.polygon = polygonPanel.getConfigPolygon();
		return config;
	}
}
