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

package boofcv.demonstrations.fiducial;

import boofcv.demonstrations.shapes.DetectBlackPolygonControlPanel;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeGuiListener;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.fiducial.ConfigFiducialBinary;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Controls for {@link DetectFiducialSquareBinaryApp}.
 *
 * @author Peter Abeles
 */
public class DetectFiducialSquareBinaryPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener {
	ShapeGuiListener owner;

	// selects which image to view
	JComboBox<String> imageView;

	JCheckBox showSquares;
	JCheckBox showOrientation;
	JCheckBox showContour;
	JCheckBox showLabels;

	boolean bShowSquares = true;
	boolean bShowOrienation = true;
	boolean bShowContour = false;
	boolean bShowlabels = false;

	DetectBlackPolygonControlPanel polygonPanel;

	ConfigFiducialBinary config = new ConfigFiducialBinary(1);

	public DetectFiducialSquareBinaryPanel( ShapeGuiListener owner ) {
		this.owner = owner;

		polygonPanel = new DetectBlackPolygonControlPanel(owner, config.squareDetector, null);
		polygonPanel.removeControlNumberOfSides();

		imageView = combo(0, "Input", "Binary", "Black");
		selectZoom = spinner(1, MIN_ZOOM, MAX_ZOOM, 0.1);
		showSquares = checkbox("Squares", bShowSquares);
		showOrientation = checkbox("Orientation", bShowOrienation);
		showContour = checkbox("Contour", bShowContour);
		showLabels = checkbox("Labels", bShowlabels);

		addLabeled(processingTimeLabel, "Time (ms)");
		addLabeled(imageSizeLabel, "Size");
		addLabeled(imageView, "View");
		addLabeled(selectZoom, "Zoom");
		addAlignLeft(showOrientation);
		addAlignLeft(showLabels);
		addAlignLeft(showSquares);
		addAlignLeft(showContour);
		add(polygonPanel);
		addVerticalGlue(this);
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == imageView) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if (e.getSource() == showSquares) {
			bShowSquares = showSquares.isSelected();
			owner.viewUpdated();
		} else if (e.getSource() == showOrientation) {
			bShowOrienation = showOrientation.isSelected();
			owner.viewUpdated();
		} else if (e.getSource() == showContour) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
		} else if (e.getSource() == showLabels) {
			bShowlabels = showLabels.isSelected();
			owner.viewUpdated();
		}
	}

	@Override
	public void stateChanged( ChangeEvent e ) {
		if (e.getSource() == selectZoom) {
			zoom = ((Number)selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		}
		owner.configUpdate();
	}

	public ThresholdControlPanel getThreshold() {
		return polygonPanel.thresholdPanel;
	}

	public ConfigFiducialBinary getConfig() {
		config.squareDetector = polygonPanel.getConfigPolygon();
		return config;
	}
}
