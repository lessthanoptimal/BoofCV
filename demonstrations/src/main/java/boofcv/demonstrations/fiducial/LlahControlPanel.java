/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.factory.fiducial.ConfigLlah;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Controls for configuring {@link boofcv.alg.feature.describe.llah.LlahOperations}
 *
 * @author Peter Abeles
 */
public class LlahControlPanel extends StandardAlgConfigPanel {
	ConfigLlah config;

	JCheckBox cEnable = checkbox("", false);
	JSpinner spinnerN, spinnerM, spinnerK;
	JComboBox<String> cInvariant;

	Listener listener;

	public LlahControlPanel( Listener listener, ConfigLlah config ) {
		this.listener = listener;
		this.config = config;
		setBorder(BorderFactory.createTitledBorder("LLAH"));

		spinnerN = spinner(config.numberOfNeighborsN, 3, 20, 1);
		spinnerM = spinner(config.sizeOfCombinationM, 3, 20, 1);
		spinnerK = spinner(config.quantizationK, 3, 200, 1);
		cInvariant = combo(config.hashType.ordinal(), (Object[])ConfigLlah.HashType.values());

		// disable by default to prevent accidental clicking and potential freezing
		spinnerN.setEnabled(false);
		spinnerM.setEnabled(false);
		spinnerK.setEnabled(false);
		cInvariant.setEnabled(false);


		int s = 5; // space between elements

		add(createHorizontalPanel(
				cEnable,
				new JLabel("N"), Box.createRigidArea(new Dimension(s, 8)), spinnerN,
				Box.createRigidArea(new Dimension(s, 8)),
				new JLabel("M"), Box.createRigidArea(new Dimension(s, 8)), spinnerM,
				Box.createRigidArea(new Dimension(s, 8)),
				new JLabel("K"), Box.createRigidArea(new Dimension(s, 8)), spinnerK));
//		addAlignCenter(cInvariant); TODO add this back in once it's understood why CROSS_RATIO causes a hard freeze
	}

	@Override
	public void controlChanged( Object source ) {
		if (source == spinnerN) {
			int v = (Integer)spinnerN.getValue();
			if (v == config.numberOfNeighborsN) return;
			config.numberOfNeighborsN = v;
		} else if (source == spinnerM) {
			int v = (Integer)spinnerM.getValue();
			if (v == config.sizeOfCombinationM) return;
			config.sizeOfCombinationM = v;
		} else if (source == spinnerK) {
			int v = (Integer)spinnerK.getValue();
			if (v == config.quantizationK) return;
			config.quantizationK = v;
		} else if (source == cInvariant) {
			int ordinal = cInvariant.getSelectedIndex();
			if (config.hashType.ordinal() == ordinal) return;
			config.hashType = ConfigLlah.HashType.values()[ordinal];
		} else if (source == cEnable) {
			if (cEnable.isSelected()) {
				JOptionPane.showMessageDialog(this, "You can easily freeze the app by changing these settings");
			}
			spinnerN.setEnabled(cEnable.isSelected());
			spinnerM.setEnabled(cEnable.isSelected());
			spinnerK.setEnabled(cEnable.isSelected());
			cInvariant.setEnabled(cEnable.isEnabled());
			return;
		} else {
			System.err.println("Unknown command");
			return;
		}

		listener.configLlahChanged();
	}

	public interface Listener {
		void configLlahChanged();
	}
}
