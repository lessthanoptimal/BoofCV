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

package boofcv.demonstrations.sfm.multiview;

import boofcv.abst.disparity.ConfigSpeckleFilter;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.ConfigDisparitySGM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.transform.census.CensusVariants;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.ControlPanelDdaComboTabs;
import boofcv.gui.controls.ControlPanelDisparityDense;
import boofcv.struct.image.GrayU8;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Controls for {@link DemoThreeViewStereoApp}
 *
 * @author Peter Abeles
 */
public class DemoThreeViewControls extends StandardAlgConfigPanel
		implements ChangeListener, ActionListener {

	int view = 0;
	int maxImageSize = 800;
	double inliers = 1.0;
	int prune = 30; // percentage of features it will prune at the very end
	boolean autoFocal = true;
	int focal = 500;

	JComboBox<String> imageView = combo(view, "Image 1", "Matches", "Inliers", "Rectified", "Disparity", "3D");

	// TODO select features, e.g. sift, surf, ShiTomasi, BRIEF
	JSpinner sMaxSize = spinner(maxImageSize, 50, 1200, 50);
	JSpinner sInliers = spinner(inliers, 0.1, 10.0, 0.1);
	JSpinner sPrune = spinner(prune, 0, 100, 5);
	JCheckBox cFocalAuto = checkbox("Auto Focal", autoFocal,
			"Automatic initial guess for focal length or user specified");
	JSpinner sFocal = spinner(focal, 100, 3000, 50);
	JButton bCompute = button("Compute", true);

	JTextArea textInfo = new JTextArea();

	ControlPanelDdaComboTabs controlsDetDescAssoc = new ControlPanelCustomDDA();
	ControlPanelDisparityDense controlDisparity;

	DemoThreeViewStereoApp owner;

	boolean scaleChanged = true;
	boolean featuresChanged = true;
	boolean assocChanged = true;
	boolean stereoChanged = true;

	public DemoThreeViewControls( DemoThreeViewStereoApp owner ) {
		this.owner = owner;

		addDisparityControls();

		textInfo.setEditable(false);

		if (autoFocal) {
			sFocal.setEnabled(false);
		}

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Calib", createSelfCalibPanel());
		tabs.addTab("Feats", controlsDetDescAssoc);
		tabs.addTab("Stereo", controlDisparity);
		tabs.setMaximumSize(tabs.getPreferredSize());

		addLabeled(imageView, "View");
		add(tabs);
		add(new JScrollPane(textInfo));
		addAlignCenter(bCompute);

		disableComputeButton();

		addKeyActions();
	}

	/**
	 * Change the view using a keyboard command
	 */
	public void addKeyActions() {
		for (int index = 0; index < imageView.getItemCount(); index++) {
			InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
			ActionMap actionMap = getActionMap();

			KeyStroke wStroke = KeyStroke.getKeyStroke(KeyEvent.VK_1 + index, Event.CTRL_MASK);
			inputMap.put(wStroke, wStroke.toString());
			int _index = index;
			actionMap.put(wStroke.toString(), new AbstractAction() {
				@Override
				public void actionPerformed( ActionEvent e ) {
					if (!isEnabled())
						return;
					imageView.setSelectedIndex(_index);
				}
			});
		}
	}

	private void addDisparityControls() {
		ConfigDisparityBMBest5 configBM = new ConfigDisparityBMBest5();
		ConfigDisparitySGM configSGM = new ConfigDisparitySGM();
		ConfigSpeckleFilter configSpeckle = new ConfigSpeckleFilter();

		configBM.disparityMin = configSGM.disparityMin = 0;
		configBM.disparityRange = configSGM.disparityRange = 200;
		configBM.regionRadiusX = configBM.regionRadiusY = 4;
		configBM.errorType = DisparityError.CENSUS;
		configBM.configCensus.variant = CensusVariants.BLOCK_7_7;

		controlDisparity = new ControlPanelDisparityDense(configBM, configSGM, configSpeckle, GrayU8.class);
		controlDisparity.setListener(this::handleStereoChanged);
	}

	private JPanel createSelfCalibPanel() {
		StandardAlgConfigPanel panel = new StandardAlgConfigPanel();
		panel.addLabeled(sMaxSize, "Max Image Size",
				"Maximum width/height of input image. Image is scaled down if larger");
		panel.addLabeled(sInliers, "Inliers (px)", "RANSAC inlier threshold in pixels for reprojection error.");
		panel.addLabeled(sPrune, "Prune %",
				"Prunes this percent of the worse matches after computing the solution once.");
		panel.addAlignLeft(cFocalAuto);
		panel.addLabeled(sFocal, "Focal", "User specified value for focal length in pixels.");
		return panel;
	}

	public void clearText() {
		textInfo.setText("");
	}

	public void addText( final String text ) {
		String a = textInfo.getText() + text;
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
	public void controlChanged( final Object source ) {
		boolean compute = true;
		if (source == sInliers) {
			inliers = ((Number)sInliers.getValue()).doubleValue();
			stereoChanged = true;
		} else if (source == sPrune) {
			prune = ((Number)sInliers.getValue()).intValue();
			stereoChanged = true;
		} else if (source == sFocal) {
			focal = ((Number)sFocal.getValue()).intValue();
			stereoChanged = true;
		} else if (source == sMaxSize) {
			maxImageSize = ((Number)sMaxSize.getValue()).intValue();
			scaleChanged = true;
		} else if (source == imageView) {
			view = imageView.getSelectedIndex();
			owner.updateVisibleGui();
			compute = false;
		} else if (source == cFocalAuto) {
			autoFocal = cFocalAuto.isSelected();
			sFocal.setEnabled(!autoFocal);
			stereoChanged = true;
		} else if (source == bCompute) {
			owner.handleComputePressed();
			compute = false;
		}
		if (compute)
			bCompute.setEnabled(true);
	}

	private class ControlPanelCustomDDA extends ControlPanelDdaComboTabs {

		public ControlPanelCustomDDA() {
			super(() -> {
				featuresChanged = true;
				bCompute.setEnabled(true);
			}, true);
		}

		@Override
		public void initializeControlsGUI() {
			configDetDesc.typeDetector = ConfigDetectInterestPoint.Type.FAST_HESSIAN;
			configDetDesc.detectFastHessian.extract.radius = 4;
			configDetDesc.detectFastHessian.maxFeaturesPerScale = 1000;

			configDetDesc.typeDescribe = ConfigDescribeRegion.Type.SURF_STABLE;

			configAssociate.type = ConfigAssociate.AssociationType.GREEDY;
			configAssociate.greedy.scoreRatioThreshold = 1.0;
			configAssociate.greedy.forwardsBackwards = true;
			configAssociate.greedy.maxErrorThreshold = -1.0;

			// Give it reasonable settings for non-defaults
			// limit memory consumption
			configDetDesc.detectPoint.general.radius = 5;
			configDetDesc.detectPoint.general.maxFeatures = 1000;
			configDetDesc.detectPoint.scaleRadius = 14;

			// it will pick better maximums with this
			configDetDesc.detectPoint.shiTomasi.radius = 4;
			configDetDesc.detectPoint.harris.radius = 4;

			super.initializeControlsGUI();
		}
	}
}
