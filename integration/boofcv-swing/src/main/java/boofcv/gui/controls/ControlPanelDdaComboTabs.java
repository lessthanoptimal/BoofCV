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

package boofcv.gui.controls;

import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;

import javax.swing.*;
import java.awt.*;

import static boofcv.factory.feature.describe.ConfigDescribeRegion.Type.BRIEF;

/**
 * Provides full control over all of Detect-Describe-Associate using combo boxes that when selected will change
 * the full set of controls shown below.
 *
 * @author Peter Abeles
 */
public class ControlPanelDdaComboTabs extends ControlPanelDetDescAssocBase {
	// Containers for different sets of controls
	final JPanel panelDetector = new JPanel(new BorderLayout());
	final JPanel panelDescriptor = new JPanel(new BorderLayout());
	final JPanel panelAssociate = new JPanel(new BorderLayout());

	final Listener listener;

	public ControlPanelDdaComboTabs(Listener listener, boolean setupGUI ) {
		this.listener = listener;

		if( setupGUI ) {
			// Declare all the controls
			initializeControlsGUI();

			layoutComponents();

			handleDetectorChanged();
			handleDescriptorChanged();
			handleAssociatorChanged();
		}
	}

	protected void layoutComponents() {
		JTabbedPane tabbed = new JTabbedPane();
		tabbed.addTab("Detect",panelDetector);
		tabbed.addTab("Describe",panelDescriptor);
		tabbed.addTab("Associate",panelAssociate);

		addLabeled(comboDetect,"Detect");
		addLabeled(comboDescribe,"Describe");
		addLabeled(comboAssociate,"Associate");
		add(tabbed);
	}

	protected void handleDetectorChanged() {
		panelDetector.removeAll();
		panelDetector.add(getDetectorPanel(),BorderLayout.CENTER);
		panelDetector.invalidate();
		panelDetector.repaint();
	}

	protected void handleDescriptorChanged() {
		panelDescriptor.removeAll();
		panelDescriptor.add(getDescriptorPanel(),BorderLayout.CENTER);
		panelDescriptor.invalidate();
		panelDescriptor.repaint();

		if( configDetDesc.typeDescribe == BRIEF ) {
			// BRIEF only works with greedy association so force it to be greedy
			if( configAssociate.type != ConfigAssociate.AssociationType.GREEDY ) {
				comboAssociate.setSelectedIndex(ConfigAssociate.AssociationType.GREEDY.ordinal());
			}
			comboAssociate.setEnabled(false);
		} else {
			comboAssociate.setEnabled(true);
		}
	}

	protected void handleAssociatorChanged() {
		panelAssociate.removeAll();
		panelAssociate.add(getAssociatePanel(),BorderLayout.CENTER);
		panelAssociate.invalidate();
		panelAssociate.repaint();
	}

	@Override
	public void controlChanged(final Object source) {
		if( comboDetect == source ) {
			configDetDesc.typeDetector =
					ConfigDetectInterestPoint.Type.values()[comboDetect.getSelectedIndex()];
			handleDetectorChanged();
		} else if( comboDescribe == source ){
			configDetDesc.typeDescribe =
					ConfigDescribeRegion.Type.values()[comboDescribe.getSelectedIndex()];
			handleDescriptorChanged();
		} else if( comboAssociate == source ){
			configAssociate.type = ConfigAssociate.AssociationType.values()[comboAssociate.getSelectedIndex()];
			handleAssociatorChanged();
		}
		listener.handleDdaControlsUpdated();
	}


	@Override
	protected void handleControlsUpdated() {
		this.listener.handleDdaControlsUpdated();
	}

	public interface Listener {
		void handleDdaControlsUpdated();
	}
}
