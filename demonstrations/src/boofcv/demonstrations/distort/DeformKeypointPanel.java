/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.distort;

import boofcv.abst.distort.mls.ConfigDeformPointMLS;
import boofcv.alg.distort.mls.TypeDeformMLS;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Peter Abeles
 */
public class DeformKeypointPanel extends StandardAlgConfigPanel implements ChangeListener {
//	JSpinner selectAlgorithm;

	JCheckBox checkDualView;
	JCheckBox checkShowPoints;

	JSpinner selectModel;
	JSpinner selectGridRows;
	JSpinner selectGridCols;
	JSpinner selectAlpha;

	boolean dualView = false;
	boolean showPoints = true;

	ConfigDeformPointMLS configMLS = new ConfigDeformPointMLS();

	Listener listener;

	public DeformKeypointPanel( Listener listener ) {
		this.listener = listener;


		selectModel = spinner(configMLS.type, TypeDeformMLS.values());
		selectGridRows = spinner(configMLS.rows, 5, 600, 5);
		selectGridCols = spinner(configMLS.cols, 5, 600, 5);
		selectAlpha = spinner(configMLS.alpha, 0.5f, 20.f, 0.5f);

		addLabeled(selectModel, "Model", this);
		addLabeled(selectGridRows, "Grid Rows", this);
		addLabeled(selectGridCols, "Grid Cols", this);
		addLabeled(selectAlpha, "Alpha", this);
	}

	public boolean isDualView() {
		return dualView;
	}

	public boolean isShowPoints() {
		return showPoints;
	}

	public ConfigDeformPointMLS getConfigMLS() {
		return configMLS;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectModel ) {
			configMLS.type = (TypeDeformMLS)selectModel.getValue();
		} else if( e.getSource() == selectGridRows ) {
			configMLS.rows = ((SpinnerNumberModel)selectGridRows.getModel()).getNumber().intValue();
		} else if( e.getSource() == selectGridCols ) {
			configMLS.cols = ((SpinnerNumberModel)selectGridCols.getModel()).getNumber().intValue();
		} else if( e.getSource() == selectAlpha ) {
			configMLS.alpha = ((SpinnerNumberModel)selectAlpha.getModel()).getNumber().floatValue();
		} else {
			return;
		}
		listener.handleAlgorithmChange();
	}

	public interface Listener {
		void handleVisualizationChange();

		void handleAlgorithmChange();
	}
}

