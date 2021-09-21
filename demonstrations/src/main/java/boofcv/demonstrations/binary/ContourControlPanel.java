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

package boofcv.demonstrations.binary;

import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.ConnectRule;
import lombok.Getter;

import javax.swing.*;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Control panel for adjusting {@link VisualizeBinaryContourApp}.
 *
 * @author Peter Abeles
 */
public class ContourControlPanel extends StandardAlgConfigPanel
{
	public int selectedView;
	public double zoom = 1.0;
	@Getter private ConnectRule connectRule = ConnectRule.FOUR;

	VisualizeBinaryContourApp<?> owner;

	@Getter ThresholdControlPanel threshold;

	// selects which image to view
	JComboBox<String> imageView = combo(selectedView,"Input","Binary","Black");
	// connection rule
	JComboBox<String> connectCombo = combo(connectRule.ordinal(),"4-Connect","8-Connect");
	// Image scale
	JSpinner selectZoom = spinner(zoom,MIN_ZOOM,MAX_ZOOM,1);

	public ContourControlPanel(VisualizeBinaryContourApp<?> owner) {
		this.owner = owner;

		threshold = new ThresholdControlPanel(owner);
		threshold.setMaximumSize(threshold.getPreferredSize());

		addLabeled(imageView,"View");
		addLabeled(selectZoom,"Zoom");
		addLabeled(connectCombo, "Connect");
		add(threshold);
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == connectCombo ) {
			connectRule = ConnectRule.values()[connectCombo.getSelectedIndex()];
			owner.contourAlgUpdated();
		} else if( source == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if ( source == selectZoom) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
		}
	}

	public void setZoom(double zoom) {
		selectZoom.setValue(zoom);
	}
}
