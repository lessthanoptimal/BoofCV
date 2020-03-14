/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
public class DetectBlackPolygonAppControlPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener
{
	ShapeGuiListener owner;

	// selects which image to view
	JComboBox imageView;

	JCheckBox showCorners;
	JCheckBox showLines;
	JCheckBox showContour;


	DetectBlackPolygonControlPanel polygonPanel;

	boolean bShowCorners = true;
	boolean bShowLines = true;
	boolean bShowContour = false;

	public DetectBlackPolygonAppControlPanel(ShapeGuiListener owner) {
		this.owner = owner;

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		showCorners = new JCheckBox("Corners");
		showCorners.addActionListener(this);
		showCorners.setSelected(bShowCorners);
		showLines = new JCheckBox("Lines");
		showLines.setSelected(bShowLines);
		showLines.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);

		polygonPanel = new DetectBlackPolygonControlPanel(owner);

		JPanel visualsPanel = new JPanel();
		visualsPanel.setLayout(new BoxLayout(visualsPanel,BoxLayout.X_AXIS));
		visualsPanel.add(showCorners);
		visualsPanel.add(showLines);
		visualsPanel.add(showContour);

		addLabeled(processingTimeLabel,"Time (ms)");
		addLabeled(imageSizeLabel,"Size");
		addLabeled(imageView, "View: ");
		addLabeled(selectZoom,"Zoom");
		add(visualsPanel);
		add(polygonPanel);
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
		}
		owner.configUpdate();
	}
}
