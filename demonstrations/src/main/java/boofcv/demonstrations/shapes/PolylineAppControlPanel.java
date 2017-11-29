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

package boofcv.demonstrations.shapes;

import boofcv.struct.ConnectRule;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * @author Peter Abeles
 */
public class PolylineAppControlPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener
{
	ShapeGuiListener owner;

	// selects which image to view
	JComboBox imageView;

	JCheckBox showCorners;
	JCheckBox showLines;
	JCheckBox showContour;

	boolean bShowCorners = true;
	boolean bShowLines = true;
	boolean bShowContour = false;

	ThresholdControlPanel threshold;
	PolylineControlPanel polylinePanel;

	JSpinner spinnerContourConnect;
	JSpinner spinnerMinContourSize;


	int minimumContourSize = 10;
	ConnectRule connectRule = ConnectRule.FOUR;

	public PolylineAppControlPanel(ShapeGuiListener owner) {
		this.owner = owner;

		this.polylinePanel = new PolylineControlPanel(owner);

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		spinnerContourConnect = spinner(connectRule.ordinal(), ConnectRule.values());

		showCorners = new JCheckBox("Corners");
		showCorners.addActionListener(this);
		showCorners.setSelected(bShowCorners);
		showLines = new JCheckBox("Lines");
		showLines.setSelected(bShowLines);
		showLines.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);

		threshold = new ThresholdControlPanel(owner);

		spinnerMinContourSize = new JSpinner(new SpinnerNumberModel(minimumContourSize,
				5,10000,2));
		spinnerMinContourSize.setMaximumSize(spinnerMinContourSize.getPreferredSize());
		spinnerMinContourSize.addChangeListener(this);

		addLabeled(processingTimeLabel,"Time (ms)", this);
		addLabeled(imageSizeLabel,"Size", this);
		addLabeled(imageView, "View: ", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignLeft(showCorners, this);
		addAlignLeft(showLines, this);
		addAlignLeft(showContour, this);
		add(threshold);
		addLabeled(spinnerMinContourSize, "Min Contour Size: ", this);
		addLabeled(spinnerContourConnect, "Contour Connect: ", this);
		addCenterLabel("Polyline",this);
		add(polylinePanel);
		addVerticalGlue(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if( e.getSource() == showCorners ) {
			bShowCorners = showCorners.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showLines ) {
			bShowLines = showLines.isSelected();
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
		} else if( e.getSource() == spinnerMinContourSize ) {
			minimumContourSize = ((Number) spinnerMinContourSize.getValue()).intValue();
		} else if( e.getSource() == spinnerContourConnect ) {
			connectRule = (ConnectRule)spinnerContourConnect.getValue();
		} else {
			throw new RuntimeException("Egads");
		}

		owner.configUpdate();
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}

	public PolylineControlPanel getPolylinePanel() {
		return polylinePanel;
	}
}
