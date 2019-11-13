/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.gui.StandardAlgConfigPanel;

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
	public static final int MAX_DISPARITY_RANGE=254;

	JComboBox imageView;

	// TODO select features, e.g. sift, surf, ShiTomasi, BRIEF
	JSpinner sMaxSize;
	JSpinner sInliers;
	JSpinner sPrune;
	JCheckBox cFocalAuto;
	JSpinner sFocal;
	JSpinner sMinDisparity;
	JSpinner sMaxDisparity;
	JButton bCompute = new JButton("Compute");

	JTextArea textInfo = new JTextArea();

	int view=0;
	int maxImageSize=800;
	double inliers = 1.0;
	int prune = 30; // percentage of features it will prune at the very end
	boolean autoFocal=true;
	int focal = 500;
	int minDisparity = 0;
	int maxDisparity = 255;

	DemoThreeViewStereoApp owner;

	boolean scaleChanged = false;
	boolean assocChanged = false;
	boolean stereoChanged = false;

	public DemoThreeViewControls( DemoThreeViewStereoApp owner ) {
		this.owner = owner;
		imageView = combo(view,"Image 1","Matches","Rectified","Disparity","3D");
		sMaxSize = spinner(maxImageSize,50,1200,50);
		sInliers = spinner(inliers,0.1,10.0,0.1);
		sPrune = spinner(prune,0,100,5);
		cFocalAuto = checkbox("Auto Focal",autoFocal);
		sFocal = spinner(focal,100,3000,50);
		sMinDisparity = spinner(minDisparity,0,1000,10);
		sMaxDisparity = spinner(maxDisparity,1,1001,10);
		bCompute.addActionListener(this);
		bCompute.setMinimumSize(bCompute.getPreferredSize());

		textInfo.setEditable(false);

		if( autoFocal ) {
			sFocal.setEnabled(false);
		}

		addLabeled(imageView,"View");
		addLabeled(sMaxSize,"Image Size");
		addLabeled(sInliers,"Inliers");
		addLabeled(sPrune,"Prune");
		addAlignLeft(cFocalAuto);
		addLabeled(sFocal,"Focal");
		addLabeled(sMinDisparity,"Min Disparity");
		addLabeled(sMaxDisparity,"Max Disparity");
		add(new JScrollPane(textInfo));
		addAlignCenter(bCompute);

		disableComputeButton();
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

		scaleChanged = false;
		assocChanged = false;
		stereoChanged = false;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		boolean compute = true;
		if( e.getSource() == sMinDisparity ) {
			minDisparity = ((Number)sMinDisparity.getValue()).intValue();
			constrainDisparity();
			stereoChanged = true;
		} else if( e.getSource() == sMaxDisparity ) {
			maxDisparity = ((Number)sMaxDisparity.getValue()).intValue();
			constrainDisparity();
			stereoChanged = true;
		} else if( e.getSource() == sInliers ) {
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

	// TODO Create a custom disparity widget and share with VisualizeStereoDisparity
	private void constrainDisparity() {
		if( maxDisparity-minDisparity>MAX_DISPARITY_RANGE ) {
			minDisparity = Math.max(0,maxDisparity-MAX_DISPARITY_RANGE);
			maxDisparity = minDisparity+MAX_DISPARITY_RANGE;
			sMinDisparity.setValue(minDisparity);
			sMaxDisparity.setValue(maxDisparity);
		}
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
