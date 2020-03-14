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

import boofcv.factory.geo.EnumPNP;
import boofcv.factory.sfm.ConfigVisOdomDepthPnP;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * Controls for {@link ConfigVisOdomDepthPnP}
 *
 * @author Peter Abeles
 */
public class ControlPanelVisOdomDepthPnP extends StandardAlgConfigPanel {
	public final ConfigVisOdomDepthPnP config = new ConfigVisOdomDepthPnP();

	private final EnumPNP[] VALUES_PNP = new EnumPNP[]{EnumPNP.P3P_GRUNERT,EnumPNP.P3P_FINSTERWALDER,EnumPNP.EPNP};

	private final JSpinner spinRansacIter = spinner(config.ransacIterations,0,9999,1);
	private final JSpinner spinRansacTol = spinner(config.ransacInlierTol,0.0,50.0,0.2);
	private final JSpinner spinRefinePnP = spinner(config.pnpRefineIterations,0,999,1);
	private final JComboBox<String> comboPnpType = combo(config.pnp.ordinal(),VALUES_PNP);
	private final JSpinner spinMaxBundle = spinner(config.maxBundleIterations,0,999,1);
	private final JSpinner spinDropOutliers = spinner(config.dropOutlierTracks,0,999,1);
	private final JSpinner spinMaxKeyFrames = spinner(config.maxKeyFrames,0,999,1);

	private final Listener listener;

	public ControlPanelVisOdomDepthPnP(Listener listener) {
		setBorder(BorderFactory.createEmptyBorder());
		this.listener = listener;
		var panelPnP = new StandardAlgConfigPanel();
		panelPnP.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"PnP"));
		panelPnP.addLabeled(spinRansacIter,"RANSAC Iter","Maximum RANSAC iterations");
		panelPnP.addLabeled(spinRansacTol,"Inlier Tol","RANSAC inlier tolerance");
		panelPnP.addLabeled(spinRefinePnP,"Refine Iter","Non-linear refinement iterations for PNP");
		panelPnP.addAlignCenter(comboPnpType);
		comboPnpType.setToolTipText("PNP solution to use in RANSAC");

		var panelBundle= new StandardAlgConfigPanel();
		panelBundle.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Bundle"));
		panelBundle.addLabeled(spinMaxBundle,"Iteration","Bundle Adjustment Iterations. 0 = disable");

		var panelMaintenance = new StandardAlgConfigPanel();
		panelMaintenance.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(),"Maintenance"));
		panelMaintenance.addLabeled(spinMaxKeyFrames,"Key Frames", "Number of key frames");
		panelMaintenance.addLabeled(spinDropOutliers,"Drop Outliers","Discards features which are outliers for this many frames. 0 = disable");

		add(fillHorizontally(panelPnP));
		add(fillHorizontally(panelBundle));
		add(fillHorizontally(panelMaintenance));
	}

	@Override
	public void controlChanged(final Object source) {
		if( spinRansacIter == source ) {
			config.ransacIterations = (Integer)spinRansacIter.getValue();
		} else if( spinRansacTol == source ) {
			config.ransacInlierTol = (Double) spinRansacTol.getValue();
		} else if( spinRefinePnP == source ) {
			config.pnpRefineIterations = (Integer)spinRefinePnP.getValue();
		} else if( spinMaxBundle == source ) {
			config.maxBundleIterations = (Integer)spinMaxBundle.getValue();
		} else if( spinDropOutliers == source ) {
			config.dropOutlierTracks = (Integer)spinDropOutliers.getValue();
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
