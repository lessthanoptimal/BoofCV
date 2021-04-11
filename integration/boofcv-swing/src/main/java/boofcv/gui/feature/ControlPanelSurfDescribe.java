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

package boofcv.gui.feature;

import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

/**
 * Control panel for {@link ConfigSurfDescribe}
 *
 * @author Peter Abeles
 */
public abstract class ControlPanelSurfDescribe extends StandardAlgConfigPanel {
	public final ConfigSurfDescribe config;
	// should it use the color or gray variant
	public boolean color = false;

	private final JSpinner spinnerLargeGrid;
	private final JSpinner spinnerSubRegion;
	private final JSpinner spinnerWidthKernel; // kernel instead of sample since I think people will understand better
	private final JCheckBox checkHaar;
	private final JCheckBox checkColor = checkbox("Color", color,
			"False use the standard gray scale variant if true then the color variant is used");

	final Listener listener;

	protected ControlPanelSurfDescribe( ConfigSurfDescribe config, Listener listener ) {
		this.config = config;
		this.listener = listener;

		spinnerLargeGrid = spinner(config.widthLargeGrid, 1, 100, 1);
		spinnerSubRegion = spinner(config.widthSubRegion, 1, 100, 1);
		spinnerWidthKernel = spinner(config.widthSample, 1, 100, 1);
		checkHaar = checkbox("Use Haar", config.useHaar, "Use Haar instead of gradient. Haar is used in paper");

		addLabeled(spinnerLargeGrid, "Large Grid", "Width of large grid in sub-regions");
		addLabeled(spinnerSubRegion, "Sub Regions", "Points in a sub-region");
		addLabeled(spinnerWidthKernel, "Kernel Width", "Width of sample point");
		addAlignLeft(checkHaar);
		addAlignLeft(checkColor);
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == spinnerLargeGrid) {
			config.widthLargeGrid = ((Number)spinnerLargeGrid.getValue()).intValue();
		} else if (source == spinnerSubRegion) {
			config.widthSubRegion = ((Number)spinnerSubRegion.getValue()).intValue();
		} else if (source == spinnerWidthKernel) {
			config.widthSample = ((Number)spinnerWidthKernel.getValue()).intValue();
		} else if (source == checkHaar) {
			config.useHaar = checkHaar.isSelected();
		} else if (source == checkColor) {
			color = checkColor.isSelected();
		} else {
			throw new RuntimeException("Unknown source");
		}
		listener.handleChangeDescribeSurf();
	}

	public static class Speed extends ControlPanelSurfDescribe {
		public ConfigSurfDescribe.Fast config;

		private final JSpinner spinnerWeightSigma;

		public Speed( ConfigSurfDescribe.Fast config_, Listener listener ) {
			super(config_ == null ? new ConfigSurfDescribe.Fast() : config_, listener);
			this.config = (ConfigSurfDescribe.Fast)super.config;

			spinnerWeightSigma = spinner(config.weightSigma, 0.5, 100.0, 0.5);

			addLabeled(spinnerWeightSigma, "Weight Sigma");
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerWeightSigma) {
				config.weightSigma = ((Number)spinnerWeightSigma.getValue()).doubleValue();
			} else {
				super.controlChanged(source);
				return;
			}
			listener.handleChangeDescribeSurf();
		}
	}

	public static class Stability extends ControlPanelSurfDescribe {
		public ConfigSurfDescribe.Stability config;

		private final JSpinner spinnerOverlap;
		private final JSpinner spinnerLargeGrid;
		private final JSpinner spinnerSubRegion;

		public Stability( ConfigSurfDescribe.Stability config_, Listener listener ) {
			super(config_ == null ? new ConfigSurfDescribe.Stability() : config_, listener);
			this.config = (ConfigSurfDescribe.Stability)super.config;

			spinnerOverlap = spinner(config.overLap, 1, 20, 1);
			spinnerLargeGrid = spinner(config.sigmaLargeGrid, 1, 20, 1);
			spinnerSubRegion = spinner(config.sigmaSubRegion, 1, 20, 1);

			addLabeled(spinnerOverlap, "Overlap", "Number of sample points sub-regions overlap");
			addLabeled(spinnerLargeGrid, "Sigma-Grid", "Sigma used to weight points in sub-region grid");
			addLabeled(spinnerSubRegion, "Sigma-Sub", "Sigma used to weight points in large grid");
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerOverlap) {
				config.overLap = ((Number)spinnerOverlap.getValue()).intValue();
			} else if (source == spinnerLargeGrid) {
				config.sigmaLargeGrid = ((Number)spinnerLargeGrid.getValue()).doubleValue();
			} else if (source == spinnerSubRegion) {
				config.sigmaSubRegion = ((Number)spinnerSubRegion.getValue()).doubleValue();
			} else {
				super.controlChanged(source);
				return;
			}
			listener.handleChangeDescribeSurf();
		}
	}

	@FunctionalInterface
	public interface Listener {
		void handleChangeDescribeSurf();
	}
}
