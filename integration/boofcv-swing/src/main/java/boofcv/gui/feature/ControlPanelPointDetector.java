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

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageGray;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Control for detecting corners and dots/blobs.
 *
 * @author Peter Abeles
 */
public class ControlPanelPointDetector extends StandardAlgConfigPanel {
	public ConfigPointDetector config;

	private @Getter final JComboBox<String> comboType;

	private @Getter final JSpinner spinnerScaleRadius;
	private @Getter final ControlPanelGeneralCorner controlGeneralCorner;
	private @Getter final JPanel panelSpecific = new JPanel(new BorderLayout());

	private @Getter final DefaultControls controlsDefault;
	private @Getter final ControlPanelFastCorner controlsFastCorner;

	Listener listener;

	public ControlPanelPointDetector( ConfigPointDetector config, Listener listener ) {
		this.config = config;
		this.listener = listener;

		spinnerScaleRadius = spinner(config.scaleRadius, 1.0, 500.0, 1.0);
		comboType = combo(config.type.ordinal(), (Object[])PointDetectorTypes.FIRST_ONLY);
		controlGeneralCorner = new ControlPanelGeneralCorner(config.general, listener::handleChangePointDetector);

		controlsDefault = new DefaultControls();
		controlsFastCorner = new ControlPanelFastCorner(config.fast, listener::handleChangePointDetector);
		controlsFastCorner.setBorder(BorderFactory.createEmptyBorder());

		controlGeneralCorner.setBorder(BorderFactory.createEmptyBorder());

		handleTypeChange();

		addLabeled(comboType, "Type", "Type of corner or blob detector");
		addLabeled(spinnerScaleRadius, "Scale-Radius", "How large it tells a scale invariant descriptor to be. Radius in pixels");
		add(controlGeneralCorner);
		add(panelSpecific);
	}

	public <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T, D> create( Class<T> imageType ) {
		return FactoryDetectPoint.create(config, imageType, null);
	}

	void handleTypeChange() {
		panelSpecific.removeAll();
		if (config.type == PointDetectorTypes.FAST) {
			panelSpecific.add(controlsFastCorner);
		} else {
			panelSpecific.add(controlsDefault);
			controlsDefault.setWeighted();
			controlsDefault.setKernelSize();
		}
		panelSpecific.validate();
		panelSpecific.repaint();
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == comboType) {
			config.type = PointDetectorTypes.FIRST_ONLY[comboType.getSelectedIndex()];
			handleTypeChange();
		} else if (source == spinnerScaleRadius) {
			config.scaleRadius = ((Number)spinnerScaleRadius.getValue()).doubleValue();
		} else {
			throw new RuntimeException("Unknown source: " +
					BoofMiscOps.toString(source, o -> o.getClass().getSimpleName()));
		}
		listener.handleChangePointDetector();
	}

	private class DefaultControls extends StandardAlgConfigPanel {
		private final JSpinner spinnerKernel;
		private final JCheckBox checkWeighted;

		public DefaultControls() {
			setBorder(BorderFactory.createEmptyBorder());
			spinnerKernel = spinner(1, 1, 1000, 1);
			checkWeighted = checkbox("Weighted", false, "Gaussian weighted or block");

			setKernelSize();
			setWeighted();

			addLabeled(spinnerKernel, "Kernel", "Radius of convolutional kernel when computing feature intensity");
			addAlignCenter(checkWeighted);
		}

		private void setKernelSize() {
			int radius = switch (config.type) {
				case SHI_TOMASI -> config.shiTomasi.radius;
				case HARRIS -> config.harris.radius;
				default -> -1;
			};
			spinnerKernel.removeChangeListener(this);
			if (radius == -1) {
				spinnerKernel.setEnabled(false);
			} else {
				spinnerKernel.setEnabled(true);
				spinnerKernel.setValue(radius);
			}
			spinnerKernel.addChangeListener(this);
		}

		private void setWeighted() {
			checkWeighted.removeActionListener(this);
			switch (config.type) {
				case SHI_TOMASI -> {
					checkWeighted.setEnabled(true);
					checkWeighted.setSelected(config.shiTomasi.weighted);
				}
				case HARRIS -> {
					checkWeighted.setEnabled(true);
					checkWeighted.setSelected(config.harris.weighted);
				}
				default -> checkWeighted.setEnabled(false);
			}
			checkWeighted.addActionListener(this);
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerKernel) {
				switch (config.type) {
					case SHI_TOMASI -> config.shiTomasi.radius = ((Number)spinnerKernel.getValue()).intValue();
					case HARRIS -> config.harris.radius = ((Number)spinnerKernel.getValue()).intValue();
					default -> {
					}
				}
			} else if (source == checkWeighted) {
				switch (config.type) {
					case SHI_TOMASI -> config.shiTomasi.weighted = checkWeighted.isSelected();
					case HARRIS -> config.harris.weighted = checkWeighted.isSelected();
					default -> {
					}
				}
			} else {
				throw new RuntimeException("Unknown source: " +
						BoofMiscOps.toString(source, o -> o.getClass().getSimpleName()));
			}
			listener.handleChangePointDetector();
		}
	}

	@FunctionalInterface
	public interface Listener {
		void handleChangePointDetector();
	}
}
