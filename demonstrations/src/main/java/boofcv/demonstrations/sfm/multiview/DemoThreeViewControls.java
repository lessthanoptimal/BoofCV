/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
	// TODO gray out compute until a change has happened
	// TODO note what has changed so that the whole thing isn't recomputed

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

	int view=0;
	int maxImageSize=500;
	double inliers = 1.0;
	int prune = 10;
	boolean autoFocal=true;
	int focal = 500;
	int minDisparity = 0;
	int maxDisparity = 255;

	DemoThreeViewStereoApp owner;

	public DemoThreeViewControls( DemoThreeViewStereoApp owner ) {
		this.owner = owner;
		imageView = combo(view,"Image 1","Matches","Rectified","Disparity","3D");
		sMaxSize = spinner(maxImageSize,50,1200,50);
		sInliers = spinner(inliers,0.1,10.0,0.1);
		sPrune = spinner(prune,0,100,5);
		cFocalAuto = checkbox("Auto Focal",autoFocal);
		sFocal = spinner(focal,100,3000,50);
		sMinDisparity = spinner(minDisparity,0,255,10);
		sMaxDisparity = spinner(maxDisparity,20,255,10);
		bCompute.addActionListener(this);

		addLabeled(imageView,"View");
		addLabeled(sMaxSize,"Image Size");
		addLabeled(sInliers,"Inliers");
		addLabeled(sPrune,"Prune");
		addAlignLeft(cFocalAuto);
		addLabeled(sFocal,"Focal");
		addLabeled(sMinDisparity,"Min Disparity");
		addLabeled(sMaxDisparity,"Max Disparity");
		addVerticalGlue();
		addAlignCenter(bCompute);
	}

	public void setViews( int which ) {
		imageView.setSelectedIndex(which);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == sMinDisparity ) {
			minDisparity = ((Number)sMinDisparity.getValue()).intValue();
		} else if( e.getSource() == sMaxDisparity ) {
			maxDisparity = ((Number)sMaxDisparity.getValue()).intValue();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imageView ) {
			view = imageView.getSelectedIndex();
			owner.updateVisibleGui();
		}
	}
}
