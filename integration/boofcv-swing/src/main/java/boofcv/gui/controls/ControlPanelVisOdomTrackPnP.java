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

package boofcv.gui.controls;

import boofcv.factory.geo.EnumPNP;
import boofcv.factory.sfm.ConfigVisOdomTrackPnP;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * Controls for {@link ConfigVisOdomTrackPnP}
 *
 * @author Peter Abeles
 */
public class ControlPanelVisOdomTrackPnP extends StandardAlgConfigPanel {
	public final ConfigVisOdomTrackPnP config;

	private final EnumPNP[] VALUES_PNP = new EnumPNP[]{EnumPNP.P3P_GRUNERT,EnumPNP.P3P_FINSTERWALDER,EnumPNP.EPNP};

	private final JSpinner spinRansacIter;
	private final JSpinner spinRansacTol;
	private final JSpinner spinRefinePnP;
	private final JComboBox<String> comboPnpType;
	private final JSpinner spinBundleMaxIter;
	private final JSpinner spinBundleFeatFrame;
	private final JSpinner spinBundleMinObs;

	private final JSpinner spinDropOutliers;
	private final JSpinner spinMaxKeyFrames;
	private final JSpinner spinKeyCoverage;

	private final Listener listener;

	public ControlPanelVisOdomTrackPnP(Listener listener, ConfigVisOdomTrackPnP config_) {
		setBorder(BorderFactory.createEmptyBorder());
		this.listener = listener;
		this.config = config_==null? new ConfigVisOdomTrackPnP() : config_;

		spinRansacIter = spinner(config.ransac.iterations,0,9999,1);
		spinRansacTol = spinner(config.ransac.inlierThreshold,0.0,50.0,0.2);
		spinRefinePnP = spinner(config.refineIterations,0,999,1);
		comboPnpType = combo(config.pnp.ordinal(), (Object[]) VALUES_PNP);
		spinBundleMaxIter = spinner(config.bundleConverge.maxIterations,0,999,1);
		spinBundleFeatFrame = spinner(config.bundleMaxFeaturesPerFrame,0,999,1);
		spinBundleMinObs = spinner(config.bundleMinObservations,2,50,1);

		spinDropOutliers = spinner(config.dropOutlierTracks,0,999,1);
		spinMaxKeyFrames = spinner(config.maxKeyFrames,2,999,1);
		spinKeyCoverage = spinner(config.keyframes.geoMinCoverage,0.0,1.0,0.05,"0.0E0",8);

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
		panelBundle.addLabeled(spinBundleFeatFrame,"Feat Frame","Maximum number of features per frame. 0 = disable");
		panelBundle.addLabeled(spinBundleMinObs,"Min Obs","Minimum observations for a track. 3 is minimum for max stability.");

		var panelMaintenance = new StandardAlgConfigPanel();
		panelMaintenance.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Maintenance"));
		panelMaintenance.addLabeled(spinMaxKeyFrames,"Key Frames", "Number of key frames");
		panelMaintenance.addLabeled(spinDropOutliers,"Drop Outliers","Discards features which are outliers for this many frames. 0 = disable");
		panelMaintenance.addLabeled(spinKeyCoverage,"Key Coverage","If tracks cover less than this fraction of the image a new keyframe is forced");

		add(fillHorizontally(panelPnP));
		add(fillHorizontally(panelBundle));
		add(fillHorizontally(panelMaintenance));
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
		} else if( spinBundleFeatFrame == source ) {
			config.bundleMaxFeaturesPerFrame = (Integer) spinBundleFeatFrame.getValue();
		} else if( spinBundleMinObs == source ) {
			config.bundleMinObservations = (Integer) spinBundleMinObs.getValue();
		} else if( spinDropOutliers == source ) {
			config.dropOutlierTracks = (Integer)spinDropOutliers.getValue();
		} else if( spinKeyCoverage == source ) {
			config.keyframes.geoMinCoverage = (Double)spinKeyCoverage.getValue();
		} else if( spinMaxKeyFrames == source ) {
			config.maxKeyFrames = (Integer)spinMaxKeyFrames.getValue();
		} else if( comboPnpType == source ) {
			config.pnp = VALUES_PNP[comboPnpType.getSelectedIndex()];
		}

		listener.changedVisOdomDepthPnP();
	}

	public interface Listener {
		void changedVisOdomDepthPnP();
	}
}
