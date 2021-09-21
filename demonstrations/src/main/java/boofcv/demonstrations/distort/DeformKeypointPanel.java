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

package boofcv.demonstrations.distort;

import boofcv.abst.distort.ConfigDeformPointMLS;
import boofcv.alg.distort.mls.TypeDeformMLS;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Controls for {@link DeformImageKeyPointsApp}
 *
 * @author Peter Abeles
 */
public class DeformKeypointPanel extends StandardAlgConfigPanel
		implements ChangeListener, ActionListener
{
//	JSpinner selectAlgorithm;

	JCheckBox checkShowOriginal;
	JSpinner selectZoom;
	JCheckBox checkShowPoints;
	JButton buttonClear;

	JComboBox selectModel;
	JSpinner selectGridRows;
	JSpinner selectGridCols;
	JSpinner selectAlpha;

	boolean showOriginal = false;
	boolean showPoints = true;
	protected double zoom = 1;

	ConfigDeformPointMLS configMLS = new ConfigDeformPointMLS();

	Listener listener;

	public DeformKeypointPanel( Listener listener ) {
		this.listener = listener;

		checkShowOriginal = new JCheckBox("Original");
		checkShowOriginal.setSelected(showOriginal);
		checkShowOriginal.addActionListener(this);

		selectZoom = new JSpinner(new SpinnerNumberModel(zoom,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		checkShowPoints = new JCheckBox("Show Points");
		checkShowPoints.setSelected(showPoints);
		checkShowPoints.addActionListener(this);

		buttonClear = new JButton("Clear");
		buttonClear.addActionListener(this);

		selectModel = new JComboBox(TypeDeformMLS.values());
		selectModel.setSelectedIndex(configMLS.type.ordinal());
		selectModel.addActionListener(this);
		selectModel.setMaximumSize(selectModel.getPreferredSize());
		selectGridRows = spinner(configMLS.rows, 5, 600, 5);
		selectGridCols = spinner(configMLS.cols, 5, 600, 5);
		selectAlpha = spinner(configMLS.alpha, 0.5f, 20.f, 0.5f);

		addAlignLeft(checkShowOriginal);
		addLabeled(selectZoom,"Zoom");
		addAlignLeft(checkShowPoints);
		addAlignCenter(buttonClear);
		addSeparator(200);
		addLabeled(selectModel, "Model");
		addLabeled(selectGridRows, "Grid Rows");
		addLabeled(selectGridCols, "Grid Cols");
		addLabeled(selectAlpha, "Alpha");
		addVerticalGlue();
	}

	public void setZoom( double _zoom ) {
		_zoom = Math.max(MIN_ZOOM,_zoom);
		_zoom = Math.min(MAX_ZOOM,_zoom);
		if( zoom == _zoom )
			return;
		this.zoom = _zoom;

		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				selectZoom.setValue(zoom);
			}
		});
	}

	public boolean isShowOriginal() {
		return showOriginal;
	}

	public boolean isShowPoints() {
		return showPoints;
	}

	public ConfigDeformPointMLS getConfigMLS() {
		return configMLS;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectGridRows ) {
			configMLS.rows = ((SpinnerNumberModel)selectGridRows.getModel()).getNumber().intValue();
		} else if( e.getSource() == selectGridCols ) {
			configMLS.cols = ((SpinnerNumberModel)selectGridCols.getModel()).getNumber().intValue();
		} else if( e.getSource() == selectAlpha ) {
			configMLS.alpha = ((SpinnerNumberModel) selectAlpha.getModel()).getNumber().floatValue();
		} else if( e.getSource() == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			listener.handleVisualizationChange();
			return;
		} else {
			return;
		}
		listener.handleAlgorithmChange();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == selectModel ) {
			configMLS.type = TypeDeformMLS.values()[selectModel.getSelectedIndex()];
			listener.handleAlgorithmChange();
		} else if( e.getSource() == checkShowOriginal) {
			showOriginal = checkShowOriginal.isSelected();
			listener.handleVisualizationChange();
		} else if( e.getSource() == checkShowPoints ) {
			showPoints = checkShowPoints.isSelected();
			listener.handleVisualizationChange();
		} else if( e.getSource() == buttonClear ) {
			listener.handleClearPoints();
		}
	}

	public interface Listener {
		void handleVisualizationChange();

		void handleAlgorithmChange();

		void handleClearPoints();
	}
}
