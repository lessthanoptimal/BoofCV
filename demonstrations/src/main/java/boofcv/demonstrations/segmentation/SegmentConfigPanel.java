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

package boofcv.demonstrations.segmentation;

import boofcv.factory.segmentation.ConfigFh04;
import boofcv.factory.segmentation.ConfigSegmentMeanShift;
import boofcv.factory.segmentation.ConfigSlic;
import boofcv.factory.segmentation.ConfigWatershed;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.struct.ConnectRule;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel which allows the user to configure segmentation algorithms and select the visualization
 *
 * @author Peter Abeles
 */
public class SegmentConfigPanel extends ViewedImageInfoPanel implements ActionListener {

	JComboBox<String> comboVisual = combo(0,"Color","Border","Regions","Input");
	JComboBox<String> comboAlgorithm = combo(0,"FH04","SLIC Superpixel","Mean-Shift","Watershed");

	JProgressBar progress = new JProgressBar();

	VisualizeImageSegmentationApp owner;

	JButton recompute = new JButton("Recompute");

	ConfigFh04 configFh = new ConfigFh04();
	ConfigSlic configSlic = new ConfigSlic(800);
	ConfigSegmentMeanShift configMeanShift = new ConfigSegmentMeanShift();
	ConfigWatershed configWatershed = new ConfigWatershed();

	JPanel panelConfig = new JPanel();
	PanelConfigFH panelFh = new PanelConfigFH();
	PanelConfigSlic panelSlic = new PanelConfigSlic();
	PanelConfigMeanShift panelMS = new PanelConfigMeanShift();
	PanelConfigWatershed panelWater = new PanelConfigWatershed();

	public int selectedAlgorithm = 0;
	public int selectedVisual=0;

	public SegmentConfigPanel(VisualizeImageSegmentationApp owner) {
		super(BoofSwingUtil.MIN_ZOOM,BoofSwingUtil.MAX_ZOOM,0.5,false);
		this.owner = owner;

		recompute.addActionListener(this);

		panelConfig.setLayout(new BorderLayout());

		setMethodControls();

		addLabeled(comboVisual, "Visualize");
		addSeparator(100);
		addLabeled(comboAlgorithm, "Method");
		addAlignCenter(recompute,this);
		addAlignCenter(progress, this);
		addAlignCenter(panelConfig,this);
	}

	public void setComputing( final boolean isComputing ) {
		SwingUtilities.invokeLater(() -> {
			if( isComputing ) {
				recompute.setEnabled(false);
				progress.setIndeterminate(true);
				progress.setEnabled(true);
			} else {
				recompute.setEnabled(true);
				progress.setIndeterminate(false);
				progress.setEnabled(false);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( comboAlgorithm == e.getSource() ) {
			selectedAlgorithm = comboAlgorithm.getSelectedIndex();
			panelConfig.removeAll();
			setMethodControls();
			panelConfig.revalidate();
			repaint();
			owner.handleControlsChanged();
		} else if( comboVisual == e.getSource() ) {
			selectedVisual = comboVisual.getSelectedIndex();
			owner.handleGuiChange();
		} else if( recompute == e.getSource() ) {
			owner.handleControlsChanged();
		}
	}

	private void setMethodControls() {
		if( selectedAlgorithm == 0 ) {
			panelConfig.add(panelFh, BorderLayout.CENTER);
		} else if( selectedAlgorithm == 1 ) {
			panelConfig.add(panelSlic,BorderLayout.CENTER);
		} else if( selectedAlgorithm == 2 ) {
			panelConfig.add(panelMS,BorderLayout.CENTER);
		} else if( selectedAlgorithm == 3 ) {
			panelConfig.add(panelWater,BorderLayout.CENTER);
		}
	}

	private class PanelConfigFH extends StandardAlgConfigPanel implements ActionListener, ChangeListener {

		JComboBox selectConnect;
		JSpinner spinnerSize;
		JSpinner spinnerK;
		JCheckBox checkApproximate = checkbox("Approx. Sort",configFh.approximateSortBins!=0);

		public PanelConfigFH() {
			selectConnect = new JComboBox(new String[]{"4-Connect","8-Connect"});
			selectConnect.addActionListener(this);
			selectConnect.setMaximumSize(selectConnect.getPreferredSize());

			spinnerSize = new JSpinner(new SpinnerNumberModel(10,5,200,5));
			spinnerSize.addChangeListener(this);
			spinnerSize.setMaximumSize(spinnerSize.getPreferredSize());

			spinnerK = new JSpinner(new SpinnerNumberModel(10,10,4000,100));
			spinnerK.addChangeListener(this);
			spinnerK.setMaximumSize(spinnerK.getPreferredSize());

			configure();

			addAlignCenter(selectConnect,this);
			addLabeled(spinnerSize, "Min Size");
			addLabeled(spinnerK, "K");
			addAlignLeft(checkApproximate,this);
		}

		private void configure() {
			if( configFh.connectRule == ConnectRule.FOUR )
				selectConnect.setSelectedIndex(0);
			else
				selectConnect.setSelectedIndex(1);

			spinnerSize.setValue(configFh.minimumRegionSize);
			spinnerK.setValue(configFh.K);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( selectConnect == e.getSource() ) {
				int which = selectConnect.getSelectedIndex();
				if( which == 0 )
					configFh.connectRule = ConnectRule.FOUR;
				else
					configFh.connectRule = ConnectRule.EIGHT;
			} else if( checkApproximate == e.getSource() ) {
				if( checkApproximate.isSelected() ) {
					configFh.approximateSortBins = 2000;
				} else {
					configFh.approximateSortBins = 0;
				}
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( spinnerSize == e.getSource() ) {
				configFh.minimumRegionSize = ((Number) spinnerSize.getValue()).intValue();
			} else if( spinnerK == e.getSource() ) {
				configFh.K = ((Number) spinnerK.getValue()).floatValue();
			}
		}
	}

	private class PanelConfigSlic extends StandardAlgConfigPanel implements ActionListener, ChangeListener {

		JComboBox selectConnect;
		JSpinner spinnerTotal;
		JSpinner spinnerWeight;

		public PanelConfigSlic() {
			selectConnect = new JComboBox(new String[]{"4-Connect","8-Connect"});
			selectConnect.addActionListener(this);
			selectConnect.setMaximumSize(selectConnect.getPreferredSize());

			spinnerTotal = new JSpinner(new SpinnerNumberModel(10,10,2000,10));
			spinnerTotal.addChangeListener(this);
			spinnerTotal.setMaximumSize(spinnerTotal.getPreferredSize());

			spinnerWeight = new JSpinner(new SpinnerNumberModel(10,10,1000,10));
			spinnerWeight.addChangeListener(this);
			spinnerWeight.setMaximumSize(spinnerWeight.getPreferredSize());

			configure();

			addAlignCenter(selectConnect,this);
			addLabeled(spinnerTotal, "Regions");
			addLabeled(spinnerWeight, "Weight");
		}

		private void configure() {
			if( configSlic.connectRule == ConnectRule.FOUR )
				selectConnect.setSelectedIndex(0);
			else
				selectConnect.setSelectedIndex(1);

			spinnerTotal.setValue(configSlic.numberOfRegions);
			spinnerWeight.setValue(configSlic.spacialWeight);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( selectConnect == e.getSource() ) {
				int which = selectConnect.getSelectedIndex();
				if( which == 0 )
					configSlic.connectRule = ConnectRule.FOUR;
				else
					configSlic.connectRule = ConnectRule.EIGHT;
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( spinnerTotal == e.getSource() ) {
				configSlic.numberOfRegions = ((Number) spinnerTotal.getValue()).intValue();
			} else if( spinnerWeight == e.getSource() ) {
				configSlic.spacialWeight = ((Number) spinnerWeight.getValue()).floatValue();
			}
		}
	}

	private class PanelConfigMeanShift extends StandardAlgConfigPanel implements ActionListener, ChangeListener {

		JComboBox selectConnect;
		JSpinner spinnerSize;
		JSpinner spinnerSpacial;
		JSpinner spinnerColor;
		JCheckBox toggleFast;

		public PanelConfigMeanShift() {
			selectConnect = new JComboBox(new String[]{"4-Connect","8-Connect"});
			selectConnect.addActionListener(this);
			selectConnect.setMaximumSize(selectConnect.getPreferredSize());

			spinnerSize = new JSpinner(new SpinnerNumberModel(10,5,200,5));
			spinnerSize.addChangeListener(this);
			spinnerSize.setMaximumSize(spinnerSize.getPreferredSize());

			spinnerSpacial = new JSpinner(new SpinnerNumberModel(10,1,50,1));
			spinnerSpacial.addChangeListener(this);
			spinnerSpacial.setMaximumSize(spinnerSpacial.getPreferredSize());

			spinnerColor = new JSpinner(new SpinnerNumberModel(10,5,255,5));
			spinnerColor.addChangeListener(this);
			spinnerColor.setMaximumSize(spinnerColor.getPreferredSize());

			toggleFast = new JCheckBox("Fast");
			toggleFast.addChangeListener(this);
			toggleFast.setMaximumSize(toggleFast.getPreferredSize());

			configure();

			addAlignCenter(selectConnect,this);
			addLabeled(spinnerSize, "Min Size");
			addLabeled(spinnerSpacial, "Spacial Rad");
			addLabeled(spinnerColor, "Color Rad");
			addAlignCenter(toggleFast, this);
		}

		private void configure() {
			if( configMeanShift.connectRule == ConnectRule.FOUR )
				selectConnect.setSelectedIndex(0);
			else
				selectConnect.setSelectedIndex(1);

			spinnerSize.setValue(configMeanShift.minimumRegionSize);
			spinnerSpacial.setValue(configMeanShift.spacialRadius);
			spinnerColor.setValue(configMeanShift.colorRadius);
			toggleFast.setSelected(configMeanShift.fast);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( selectConnect == e.getSource() ) {
				int which = selectConnect.getSelectedIndex();
				if( which == 0 )
					configMeanShift.connectRule = ConnectRule.FOUR;
				else
					configMeanShift.connectRule = ConnectRule.EIGHT;
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( spinnerSize == e.getSource() ) {
				configMeanShift.minimumRegionSize = ((Number) spinnerSize.getValue()).intValue();
			} else if( spinnerSpacial == e.getSource() ) {
				configMeanShift.spacialRadius = ((Number) spinnerSpacial.getValue()).intValue();
			} else if( spinnerColor == e.getSource() ) {
				configMeanShift.colorRadius = ((Number) spinnerColor.getValue()).intValue();
			} else if( toggleFast == e.getSource() ) {
				configMeanShift.fast = toggleFast.isSelected();
			}
		}
	}

	private class PanelConfigWatershed extends StandardAlgConfigPanel implements ActionListener, ChangeListener {

		JComboBox selectConnect;
		JSpinner spinnerSize;

		PanelConfigWatershed() {
			selectConnect = new JComboBox(new String[]{"4-Connect","8-Connect"});
			selectConnect.addActionListener(this);
			selectConnect.setMaximumSize(selectConnect.getPreferredSize());

			spinnerSize = new JSpinner(new SpinnerNumberModel(10,0,500,5));
			spinnerSize.addChangeListener(this);
			spinnerSize.setMaximumSize(spinnerSize.getPreferredSize());

			configure();

			addAlignCenter(selectConnect, this);
			addLabeled(spinnerSize, "Min Size");
		}

		private void configure() {
			if( configWatershed.connectRule == ConnectRule.FOUR )
				selectConnect.setSelectedIndex(0);
			else
				selectConnect.setSelectedIndex(1);

			spinnerSize.setValue(configWatershed.minimumRegionSize);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( selectConnect == e.getSource() ) {
				int which = selectConnect.getSelectedIndex();
				if( which == 0 )
					configWatershed.connectRule = ConnectRule.FOUR;
				else
					configWatershed.connectRule = ConnectRule.EIGHT;
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( spinnerSize == e.getSource() ) {
				configWatershed.minimumRegionSize = ((Number) spinnerSize.getValue()).intValue();
			}
		}
	}

	public ConfigFh04 getConfigFh() {
		return configFh;
	}

	public ConfigSlic getConfigSlic() {
		return configSlic;
	}

	public ConfigSegmentMeanShift getConfigMeanShift() {
		return configMeanShift;
	}

	public ConfigWatershed getConfigWatershed() {
		return configWatershed;
	}
}
