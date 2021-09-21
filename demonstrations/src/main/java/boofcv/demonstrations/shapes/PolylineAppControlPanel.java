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

package boofcv.demonstrations.shapes;

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.ConnectRule;
import lombok.Getter;

import javax.swing.*;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Configuration polyline panel
 *
 * @author Peter Abeles
 */
public class PolylineAppControlPanel extends DetectBlackShapePanel
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

	@Getter ThresholdControlPanel threshold;
	@Getter PolylineControlPanel polylinePanel;

	JSpinner spinnerContourConnect;
	JSpinner spinnerMinContourSize;


	int minimumContourSize = 10;
	ConnectRule connectRule = ConnectRule.FOUR;

	public PolylineAppControlPanel(ShapeGuiListener owner) {
		this.owner = owner;

		this.polylinePanel = new PolylineControlPanel(owner);
		this.polylinePanel.setBorder(BorderFactory.createTitledBorder("Polyline"));

		imageView = combo(0,"Input","Binary","Black");
		selectZoom = spinner(1,MIN_ZOOM,MAX_ZOOM,1);
		showCorners = checkbox("Corners",bShowCorners,"Show corners in the polyline");
		showLines = checkbox("Lines",bShowLines,"Show lines");
		showContour = checkbox("Contour",bShowContour,"Show the input contours used to compute polyline");

		threshold = new ThresholdControlPanel(owner);
		threshold.setBorder(BorderFactory.createTitledBorder("Threshold"));

		spinnerMinContourSize = spinner(minimumContourSize,5,10000,2);
		spinnerContourConnect = spinner(connectRule.ordinal(), ConnectRule.values());

		var contourPanel = new StandardAlgConfigPanel();
		contourPanel.setBorder(BorderFactory.createTitledBorder("Contour"));
		contourPanel.addLabeled(spinnerMinContourSize, "Min Size","Minimum number of pixels in a contour allowed");
		contourPanel.addLabeled(spinnerContourConnect, "Connect Rule","Connectivity rule between pixels in the contour");

		addLabeled(processingTimeLabel,"Time (ms)");
		addLabeled(imageSizeLabel,"Size","Size of input image");
		addLabeled(imageView, "View","Which view to show in the main image panel");
		addLabeled(selectZoom,"Zoom","Zoom factor of image panel");
		addAlignLeft(showCorners);
		addAlignLeft(showLines);
		addAlignLeft(showContour);
		add(polylinePanel);
		add(contourPanel);
		add(threshold);
		addVerticalGlue();
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if( source == showCorners ) {
			bShowCorners = showCorners.isSelected();
			owner.viewUpdated();
		} else if( source == showLines ) {
			bShowLines = showLines.isSelected();
			owner.viewUpdated();
		} else if( source == showContour ) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
		} else if( source == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		} else if( source == spinnerMinContourSize ) {
			minimumContourSize = ((Number) spinnerMinContourSize.getValue()).intValue();
		} else if( source == spinnerContourConnect ) {
			connectRule = (ConnectRule)spinnerContourConnect.getValue();
		} else {
			throw new RuntimeException("Egads");
		}

		owner.configUpdate();
	}
}
