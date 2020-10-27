/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.describe;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Panel for adjusting how HOG is visualized and configured
 *
 * @author Peter Abeles
 */
public class ControlHogCellPanel extends StandardAlgConfigPanel
		implements ChangeListener, ItemListener {
	JCheckBox showInput;
	JCheckBox showGrid;
	JCheckBox showLog;
	JCheckBox showLocal;

	JSpinner selectWidth;
	JSpinner selectHistogram;

	boolean doShowGrid = false;
	boolean doShowLog = true;
	boolean doShowLocal = false;

	int cellWidth = 20;
	int histogram = 9;

	VisualizeImageHogCellApp owner;

	public ControlHogCellPanel( VisualizeImageHogCellApp owner ) {

		this.owner = owner;

		showInput = new JCheckBox("Show Input");
		showInput.setSelected(false);
		showInput.addItemListener(this);
		showInput.setMaximumSize(showInput.getPreferredSize());

		showGrid = new JCheckBox("Show Grid");
		showGrid.setSelected(doShowGrid);
		showGrid.addItemListener(this);
		showGrid.setMaximumSize(showGrid.getPreferredSize());

		showLog = new JCheckBox("Log Intensity");
		showLog.setSelected(doShowLog);
		showLog.addItemListener(this);
		showLog.setMaximumSize(showLog.getPreferredSize());

		showLocal = new JCheckBox("Local Scaling");
		showLocal.setSelected(doShowLocal);
		showLocal.addItemListener(this);
		showLocal.setMaximumSize(showLocal.getPreferredSize());

		selectWidth = new JSpinner(new SpinnerNumberModel(cellWidth, 5, 50, 1));
		selectWidth.addChangeListener(this);
		selectWidth.setMaximumSize(selectWidth.getPreferredSize());

		selectHistogram = new JSpinner(new SpinnerNumberModel(histogram, 4, 32, 1));
		selectHistogram.addChangeListener(this);
		selectHistogram.setMaximumSize(selectHistogram.getPreferredSize());

		addAlignLeft(showInput);
		addAlignLeft(showGrid);
		addAlignLeft(showLog);
		addAlignLeft(showLocal);

		addLabeled(selectWidth, "Size:");
		addLabeled(selectHistogram, "Histogram:");
	}

	@Override
	public void stateChanged( ChangeEvent e ) {
		if (selectWidth == e.getSource()) {
			cellWidth = ((Number)selectWidth.getValue()).intValue();
			owner.setCellWidth(cellWidth);
		} else if (selectHistogram == e.getSource()) {
			histogram = ((Number)selectHistogram.getValue()).intValue();
			owner.setOrientationBins(histogram);
		}
	}

	@Override
	public void itemStateChanged( ItemEvent e ) {
		if (showGrid == e.getSource()) {
			doShowGrid = showGrid.isSelected();
			owner.setShowGrid(doShowGrid);
		} else if (showLog == e.getSource()) {
			doShowLog = showLog.isSelected();
			owner.setShowLog(doShowLog);
		} else if (showLocal == e.getSource()) {
			doShowLocal = showLocal.isSelected();
			owner.setShowLocal(doShowLocal);
		} else if (showInput == e.getSource()) {
			owner.setShowInput(showInput.isSelected());
		}
	}
}
