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

package boofcv.demonstrations.sfm.multiview;

import boofcv.demonstrations.feature.disparity.ControlPanelDisparityDense;
import boofcv.factory.feature.disparity.ConfigDisparityBMBest5;
import boofcv.factory.feature.disparity.ConfigDisparitySGM;
import boofcv.factory.feature.disparity.DisparityError;
import boofcv.factory.transform.census.CensusVariants;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.GrayU8;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Peter Abeles
 */
public class DemoThreeViewControls extends StandardAlgConfigPanel
	implements ChangeListener, ActionListener
{

	JComboBox imageView;

	// TODO select features, e.g. sift, surf, ShiTomasi, BRIEF
	JSpinner sMaxSize;
	JSpinner sInliers;
	JSpinner sPrune;
	JCheckBox cFocalAuto;
	JSpinner sFocal;
	JButton bCompute = new JButton("Compute");

	JTextArea textInfo = new JTextArea();

	int view=0;
	int maxImageSize=800;
	double inliers = 1.0;
	int prune = 30; // percentage of features it will prune at the very end
	boolean autoFocal=true;
	int focal = 500;

	ControlPanelDisparityDense controlDisparity;

	DemoThreeViewStereoApp owner;

	boolean scaleChanged = true;
	boolean assocChanged = true;
	boolean stereoChanged = true;

	public DemoThreeViewControls( DemoThreeViewStereoApp owner ) {
		this.owner = owner;
		imageView = combo(view,"Image 1","Matches","Rectified","Disparity","3D");
		sMaxSize = spinner(maxImageSize,50,1200,50);
		sInliers = spinner(inliers,0.1,10.0,0.1);
		sPrune = spinner(prune,0,100,5);
		cFocalAuto = checkbox("Auto Focal",autoFocal);
		sFocal = spinner(focal,100,3000,50);
		bCompute.addActionListener(this);
		bCompute.setMinimumSize(bCompute.getPreferredSize());
		addDisparityControls();

		textInfo.setEditable(false);

		if( autoFocal ) {
			sFocal.setEnabled(false);
		}

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Self",createSelfCalibPanel());
		tabs.addTab("Stereo",controlDisparity);
		tabs.setMaximumSize(tabs.getPreferredSize());

		addLabeled(imageView,"View");
		add(tabs);
		add(new JScrollPane(textInfo));
		addAlignCenter(bCompute);

		disableComputeButton();
	}

	private void addDisparityControls() {
		ConfigDisparityBMBest5 configBM = new ConfigDisparityBMBest5();
		ConfigDisparitySGM configSGM = new ConfigDisparitySGM();

		configBM.disparityMin = configSGM.disparityMin = 0;
		configBM.disparityRange = configSGM.disparityRange = 200;
		configBM.regionRadiusX = configBM.regionRadiusY = 4;
		configBM.errorType = DisparityError.CENSUS;
		configBM.configCensus.variant = CensusVariants.BLOCK_7_7;

		controlDisparity = new ControlPanelDisparityDense(configBM,configSGM, GrayU8.class);
		controlDisparity.setListener(this::handleStereoChanged);
	}

	private JPanel createSelfCalibPanel() {
		StandardAlgConfigPanel panel = new StandardAlgConfigPanel();
		panel.addLabeled(sMaxSize,"Image Size");
		panel.addLabeled(sInliers,"Inliers");
		panel.addLabeled(sPrune,"Prune");
		panel.addAlignLeft(cFocalAuto);
		panel.addLabeled(sFocal,"Focal");
		return panel;
	}

	public void clearText() {
		textInfo.setText("");
	}

	public void addText( final String text ) {
		String a = textInfo.getText()+text;
		textInfo.setText(a);
	}

	public void setViews( int which ) {
		imageView.setSelectedIndex(which);
	}

	public void disableComputeButton() {
		bCompute.setEnabled(false);
	}

	void handleStereoChanged() {
		stereoChanged = true;
		bCompute.setEnabled(true);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		boolean compute = true;
		if( e.getSource() == sInliers ) {
			inliers = ((Number)sInliers.getValue()).doubleValue();
			stereoChanged = true;
		} else if( e.getSource() == sPrune ) {
			prune = ((Number)sInliers.getValue()).intValue();
			stereoChanged = true;
		} else if( e.getSource() == sFocal ) {
			focal = ((Number)sFocal.getValue()).intValue();
			stereoChanged = true;
		} else if( e.getSource() == sMaxSize ) {
			maxImageSize = ((Number)sMaxSize.getValue()).intValue();
			scaleChanged = true;
		}
		if( compute )
			bCompute.setEnabled(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imageView ) {
			view = imageView.getSelectedIndex();
			owner.updateVisibleGui();
		} else if( e.getSource() == cFocalAuto ) {
			autoFocal = cFocalAuto.isSelected();
			sFocal.setEnabled(!autoFocal);
			stereoChanged = true;
			bCompute.setEnabled(true);
		} else if( e.getSource() == bCompute ) {
			owner.handleComputePressed();
		}
	}
}
