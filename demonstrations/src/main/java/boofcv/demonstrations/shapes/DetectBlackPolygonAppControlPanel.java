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

import javax.swing.*;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Control panel for configuring a black polygon detector
 *
 * @author Peter Abeles
 */
public class DetectBlackPolygonAppControlPanel extends DetectBlackShapePanel {
	ShapeGuiListener owner;

	// selects which image to view
	JComboBox<String> imageView = combo(0, "Input", "Binary", "Black");

	boolean bShowCorners = true;
	boolean bShowLines = true;
	boolean bShowContour = false;

	JCheckBox showCorners = checkbox("Corners", bShowCorners);
	JCheckBox showLines = checkbox("Lines", bShowLines);
	JCheckBox showContour = checkbox("Contour", bShowContour);

	DetectBlackPolygonControlPanel polygonPanel;

	public DetectBlackPolygonAppControlPanel( ShapeGuiListener owner ) {
		this.owner = owner;

		selectZoom = spinner(1, MIN_ZOOM, MAX_ZOOM, 1);
		polygonPanel = new DetectBlackPolygonControlPanel(owner);

		JPanel visualsPanel = new JPanel();
		visualsPanel.setLayout(new BoxLayout(visualsPanel, BoxLayout.X_AXIS));
		visualsPanel.add(showCorners);
		visualsPanel.add(showLines);
		visualsPanel.add(showContour);

		addLabeled(processingTimeLabel, "Time (ms)");
		addLabeled(imageSizeLabel, "Size");
		addLabeled(imageView, "View: ");
		addLabeled(selectZoom, "Zoom");
		add(visualsPanel);
		add(polygonPanel);
		addVerticalGlue(this);
	}

	@Override public void controlChanged( Object source ) {
		if (source == imageView) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
			return;
		} else if (source == showCorners) {
			bShowCorners = showCorners.isSelected();
			owner.viewUpdated();
			return;
		} else if (source == showLines) {
			bShowLines = showLines.isSelected();
			owner.viewUpdated();
			return;
		} else if (source == showContour) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
			return;
		} else if (source == selectZoom) {
			zoom = ((Number)selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		}
		owner.configUpdate();
	}
}
