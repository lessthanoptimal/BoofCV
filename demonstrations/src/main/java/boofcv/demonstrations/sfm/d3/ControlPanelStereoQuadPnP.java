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

package boofcv.demonstrations.sfm.d3;

import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.sfm.ConfigStereoQuadPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.ControlPanelAssociateGreedy;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Control Panel for {@link ConfigStereoQuadPnP}
 *
 * @author Peter Abeles
 */
public class ControlPanelStereoQuadPnP extends JTabbedPane {

	final ConfigStereoQuadPnP config;

	final ControlPanelDetDescAssocBase panelFeatures;
	final ControlPanelMotion panelMotion;
	final ControlPanelAssociate panelAssociate;

	final Listener listener;

	public ControlPanelStereoQuadPnP(ConfigStereoQuadPnP config, Listener listener ) {
		setBorder(BorderFactory.createEmptyBorder());

		this.listener = listener;
		this.config = config;

		panelFeatures = new ControlPanelFeatures();
		panelMotion = new ControlPanelMotion();
		panelAssociate = new ControlPanelAssociate();

		addTab("Motion", panelMotion);
		addTab("Feat", panelFeatures);
		addTab("Assoc",panelAssociate);
	}

	public <T extends ImageGray<T>>
	StereoVisualOdometry<T> createVisOdom(Class<T> imageType ) {

		StereoVisualOdometry<T>  alg = FactoryVisualOdometry.stereoQuadPnP(config,imageType);

		Set<String> configuration = new HashSet<>();
		configuration.add(VisualOdometry.VERBOSE_RUNTIME);
		configuration.add(VisualOdometry.VERBOSE_TRACKING);
		alg.setVerbose(System.out,configuration);

		return alg;
	}

	public class ControlPanelAssociate extends StandardAlgConfigPanel {
		private final ControlPanelAssociateGreedy panelAssociateF2F;
		private final ControlPanelAssociateGreedy panelAssociateL2R;
		private final JSpinner spinnerEpipolarTol;
		public ControlPanelAssociate() {
			setBorder(BorderFactory.createEmptyBorder());
			panelAssociateL2R = new ControlPanelAssociateGreedy(config.associateL2R,()->listener.changedStereoQuadPnP());
			panelAssociateF2F = new ControlPanelAssociateGreedy(config.associateF2F,()->listener.changedStereoQuadPnP());
			panelAssociateL2R.setBorder(BorderFactory.createTitledBorder("Left to Right"));
			panelAssociateF2F.setBorder(BorderFactory.createTitledBorder("Frame to Frame"));

			// disable since stereo doesn't support these features yet
			panelAssociateL2R.getCheckForwardsBackwards().setEnabled(false);
			panelAssociateL2R.getSpinnerRatio().setEnabled(false);

			spinnerEpipolarTol = spinner(config.epipolarTol,0.0,999.9,0.1);

			add(panelAssociateL2R);
			add(panelAssociateF2F);
			addLabeled(spinnerEpipolarTol,"Stereo Tol",
					"How far away a point can be from the epipolar line to be considered for a match");
		}
		@Override
		public void controlChanged(final Object source) {
			if( source == spinnerEpipolarTol ) {
				config.epipolarTol = ((Number)spinnerEpipolarTol.getValue()).doubleValue();
				listener.changedStereoQuadPnP();
			}
		}
	}

	public class ControlPanelFeatures extends ControlPanelDetDescAssocBase
	{
		private final JPanel controlPanel = new JPanel(new BorderLayout());

		public ControlPanelFeatures() {
			super.configDetDesc = config.detectDescribe;

			initializeControlsGUI();

			addLabeled(comboDetect,"Detect","Point feature detectors");
			addLabeled(comboDescribe,"Describe","Point feature Descriptors");
			add(controlPanel);
			updateActiveControls(0);
		}

		private void updateActiveControls( int which ) {
			controlPanel.removeAll();
			String title;
			JPanel inside;
			switch( which ) {
				case 0: inside = getDetectorPanel(); title = "Detect";break;
				case 1: inside = getDescriptorPanel(); title = "Describe";break;
				default: throw new RuntimeException("BUG!");
			}
			controlPanel.setBorder(BorderFactory.createTitledBorder(title));
			if( inside != null )
				controlPanel.add(BorderLayout.CENTER,inside);
			controlPanel.validate();
			SwingUtilities.invokeLater(this::repaint);
		}

		@Override
		public void controlChanged(final Object source) {
			int which = -1;
			if (source == comboDetect) {
				configDetDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.values()[comboDetect.getSelectedIndex()];
				which = 0;
			} else if (source == comboDescribe) {
				configDetDesc.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.values()[comboDescribe.getSelectedIndex()];
				which = 1;
			} else {
				throw new RuntimeException("BUG!");
			}
			updateActiveControls(which);
			listener.changedStereoQuadPnP();
		}

		@Override
		protected void handleControlsUpdated() {
			listener.changedStereoQuadPnP();
		}
	}

	public class ControlPanelMotion extends StandardAlgConfigPanel
	{
		private final EnumPNP[] VALUES_PNP = new EnumPNP[]{EnumPNP.P3P_GRUNERT,EnumPNP.P3P_FINSTERWALDER,EnumPNP.EPNP};

		private final JSpinner spinRansacIter;
		private final JSpinner spinRansacTol;
		private final JSpinner spinRefinePnP;
		private final JComboBox<String> comboPnpType;
		private final JSpinner spinBundleMaxIter;

		public ControlPanelMotion() {
			spinRansacIter = spinner(config.ransac.iterations,0,9999,1);
			spinRansacTol = spinner(config.ransac.inlierThreshold,0.0,50.0,0.2);
			spinRefinePnP = spinner(config.refineIterations,0,999,1);
			comboPnpType = combo(config.pnp.ordinal(),VALUES_PNP);
			spinBundleMaxIter = spinner(config.bundleConverge.maxIterations,0,999,1);

			var panelPnP = new StandardAlgConfigPanel();
			panelPnP.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"PnP"));
			panelPnP.addLabeled(spinRansacIter,"RANSAC Iter","Maximum RANSAC iterations");
			panelPnP.addLabeled(spinRansacTol,"Inlier Tol","RANSAC inlier tolerance");
			panelPnP.addLabeled(spinRefinePnP,"Refine Iter","Non-linear refinement iterations for PNP");
			panelPnP.addAlignCenter(comboPnpType);
			comboPnpType.setToolTipText("PNP solution to use in RANSAC");

			var panelBundle= new StandardAlgConfigPanel();
			panelBundle.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Bundle"));
			panelBundle.addLabeled(spinBundleMaxIter,"Iteration","Bundle Adjustment Iterations. 0 = disable");

			add(fillHorizontally(panelPnP));
			add(fillHorizontally(panelBundle));
		}

		@Override
		public void controlChanged(final Object source) {
			if( spinRansacIter == source ) {
				config.ransac.iterations = (Integer)spinRansacIter.getValue();
			} else if( spinRansacTol == source ) {
				config.ransac.inlierThreshold = (Double) spinRansacTol.getValue();
			} else if( spinRefinePnP == source ) {
				config.refineIterations = (Integer)spinRefinePnP.getValue();
			} else if( spinBundleMaxIter == source ) {
				config.bundleConverge.maxIterations = (Integer) spinBundleMaxIter.getValue();
			} else if( comboPnpType == source ) {
				config.pnp = VALUES_PNP[comboPnpType.getSelectedIndex()];
			}

			listener.changedStereoQuadPnP();
		}
	}


	public interface Listener {
		void changedStereoQuadPnP();
	}
}
