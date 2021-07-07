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

import boofcv.abst.feature.describe.ConfigSiftDescribe;
import boofcv.gui.StandardAlgConfigPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Control panel for {@link ConfigSiftDescribe}
 *
 * @author Peter Abeles
 */
public class ControlPanelDescribeSift extends StandardAlgConfigPanel {
	public final ConfigSiftDescribe config;

	private final JSpinner spinnerWidthSubregion;
	private final JSpinner spinnerWidthGrid;
	private final JSpinner spinnerHistogram;
	private final JSpinner spinnerSigmaToPixels;
	private final JSpinner spinnerWeightFraction;
	private final JSpinner spinnerMaxElementFraction;

	Listener listener;

	public ControlPanelDescribeSift( @Nullable ConfigSiftDescribe config_, Listener listener ) {
		config = config_ == null ? new ConfigSiftDescribe() : config_;
		this.listener = listener;

		spinnerWidthSubregion = spinner(config.widthSubregion, 1, 50, 1);
		spinnerWidthGrid = spinner(config.widthGrid, 1, 50, 1);
		spinnerHistogram = spinner(config.numHistogramBins, 1, 50, 1);
		spinnerSigmaToPixels = spinner(config.sigmaToPixels, 0.1, 100.0, 0.5);
		spinnerWeightFraction = spinner(config.weightingSigmaFraction, 0.0001, 1.0, 0.05, "0.0E0", 10);
		spinnerMaxElementFraction = spinner(config.maxDescriptorElementValue, 0.00001, 1.0, 0.01, "0.0E0", 10);

		addLabeled(spinnerWidthSubregion, "Subregion", "Width of sub-region in samples");
		addLabeled(spinnerWidthGrid, "Grid", "Width of grid in subregions");
		addLabeled(spinnerHistogram, "Histogram", "Number of histogram bins");
		addLabeled(spinnerSigmaToPixels, "Sigma Pixels", "Conversion of sigma to pixels. Used to scale the descriptor region");
		addLabeled(spinnerWeightFraction, "Sigma Frac.", "Sigma for Gaussian weighting function is set to this value * region width");
		addLabeled(spinnerMaxElementFraction, "Max Frac.", "Maximum fraction a single element can have in descriptor. Helps with non-affine changes in lighting. See paper");
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == spinnerWidthSubregion) {
			config.widthSubregion = ((Number)spinnerWidthSubregion.getValue()).intValue();
		} else if (source == spinnerWidthGrid) {
			config.widthGrid = ((Number)spinnerWidthGrid.getValue()).intValue();
		} else if (source == spinnerHistogram) {
			config.numHistogramBins = ((Number)spinnerHistogram.getValue()).intValue();
		} else if (source == spinnerSigmaToPixels) {
			config.sigmaToPixels = ((Number)spinnerSigmaToPixels.getValue()).doubleValue();
		} else if (source == spinnerWeightFraction) {
			config.weightingSigmaFraction = ((Number)spinnerWeightFraction.getValue()).doubleValue();
		} else if (source == spinnerMaxElementFraction) {
			config.maxDescriptorElementValue = ((Number)spinnerMaxElementFraction.getValue()).doubleValue();
		}
		listener.handleChangeDescribeSift();
	}

	@FunctionalInterface
	public interface Listener {
		void handleChangeDescribeSift();
	}
}
