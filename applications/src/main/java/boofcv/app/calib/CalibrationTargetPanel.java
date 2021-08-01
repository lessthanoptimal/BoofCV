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

package boofcv.app.calib;

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Used to specify the calibration target's parameters
 *
 * @author Peter Abeles
 */
public class CalibrationTargetPanel extends StandardAlgConfigPanel implements ActionListener {
	JComboBox<CalibrationPatterns> comboType;
	JPanel panelTarget = new JPanel();

	Listener listener;

	public CalibrationPatterns selected = CalibrationPatterns.CHESSBOARD;

	public ConfigGridDimen configChessboard = new ConfigGridDimen(7, 5, 1);
	public ConfigGridDimen configChessboardBits = new ConfigGridDimen(9, 7, 1);
	public ConfigGridDimen configSquare = new ConfigGridDimen(4, 3, 1, 1);
	public ConfigGridDimen configCircle = new ConfigGridDimen(15, 10, 1, 1.5);
	public ConfigGridDimen configCircleHex = new ConfigGridDimen(15, 15, 1, 1.5);

	public CalibrationTargetPanel( Listener listener ) {
		setBorder(BorderFactory.createEmptyBorder());

		this.listener = listener;
		comboType = new JComboBox<>(CalibrationPatterns.values());
		comboType.addActionListener(this);
		comboType.setMaximumSize(comboType.getPreferredSize());

		panelTarget.setLayout(new BorderLayout());
		panelTarget.setPreferredSize(new Dimension(250, 106));
		panelTarget.setMaximumSize(panelTarget.getPreferredSize());
		changeTargetPanel();

		addLabeled(comboType, "Target Type");
		add(Box.createRigidArea(new Dimension(10, 10)));
		addAlignCenter(panelTarget);
	}

	public void updateParameters() {
		ConfigGridDimen c = switch (selected) {
			case CHESSBOARD -> configChessboard;
			case CHESSBOARD_BITS -> configChessboardBits;
			case SQUARE_GRID -> configSquare;
			case CIRCLE_GRID -> configCircle;
			case CIRCLE_HEXAGONAL -> configCircleHex;
			default -> throw new RuntimeException("Unknown");
		};
		listener.calibrationParametersChanged(selected, c);
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == comboType) {
			selected = (CalibrationPatterns)comboType.getSelectedItem();
			changeTargetPanel();
			updateParameters();
		}
	}

	public void changeTargetPanel() {

		JPanel p = switch (selected) {
			case CHESSBOARD -> new ChessPanel();
			case CHESSBOARD_BITS -> new ChessPanel();
			case SQUARE_GRID -> new SquareGridPanel();
			case CIRCLE_GRID -> new CircleGridPanel();
			case CIRCLE_HEXAGONAL -> new CircleHexPanel();
			default -> throw new RuntimeException("Unknown");
		};

		panelTarget.removeAll();
		panelTarget.add(BorderLayout.CENTER, p);
		panelTarget.validate();
		panelTarget.repaint();
	}

	private class ChessPanel extends StandardAlgConfigPanel implements ChangeListener {

		JSpinner sRows, sCols, sWidth;

		public ChessPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sRows = spinner(configChessboard.numRows, 1, 1000, 1);
			sCols = spinner(configChessboard.numCols, 1, 1000, 1);
			sWidth = spinner(configChessboard.shapeSize, 0, 1000000.0, 1);

			addLabeled(sRows, "Rows");
			addLabeled(sCols, "Cols");
			addLabeled(sWidth, "Square Width");
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == sRows) {
				configChessboard.numRows = ((Number)sRows.getValue()).intValue();
			} else if (e.getSource() == sCols) {
				configChessboard.numCols = ((Number)sCols.getValue()).intValue();
			} else if (e.getSource() == sWidth) {
				configChessboard.shapeSize = ((Number)sWidth.getValue()).doubleValue();
			}
			updateParameters();
		}
	}

	private class SquareGridPanel extends StandardAlgConfigPanel implements ChangeListener {

		JSpinner sRows, sCols;
		JSpinner sWidth, sSpace;

		public SquareGridPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sRows = spinner(configSquare.numRows, 1, 1000, 1);
			sCols = spinner(configSquare.numCols, 1, 1000, 1);
			sWidth = spinner(configSquare.shapeSize, 0, 1000000.0, 1);
			sSpace = spinner(configSquare.shapeDistance, 0, 1000000.0, 1);

			addLabeled(sRows, "Rows");
			addLabeled(sCols, "Cols");
			addLabeled(sWidth, "Square Width");
			addLabeled(sSpace, "Space Width");
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == sRows) {
				configSquare.numRows = ((Number)sRows.getValue()).intValue();
			} else if (e.getSource() == sCols) {
				configSquare.numCols = ((Number)sCols.getValue()).intValue();
			} else if (e.getSource() == sWidth) {
				configSquare.shapeSize = ((Number)sWidth.getValue()).doubleValue();
			} else if (e.getSource() == sSpace) {
				configSquare.shapeDistance = ((Number)sSpace.getValue()).doubleValue();
			}
			updateParameters();
		}
	}

	private class CircleGridPanel extends StandardAlgConfigPanel implements ChangeListener {

		JSpinner sRows, sCols;
		JSpinner sDiam, sDist;

		public CircleGridPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sRows = spinner(configCircle.numRows, 1, 1000, 1);
			sCols = spinner(configCircle.numCols, 1, 1000, 1);
			sDiam = spinner(configCircle.shapeSize, 0, 1000000.0, 1);
			sDist = spinner(configCircle.shapeDistance, 0, 1000000.0, 1);

			addLabeled(sRows, "Rows");
			addLabeled(sCols, "Cols");
			addLabeled(sDiam, "Circle Diameter");
			addLabeled(sDist, "Center Distance");
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == sRows) {
				configCircle.numRows = ((Number)sRows.getValue()).intValue();
			} else if (e.getSource() == sCols) {
				configCircle.numCols = ((Number)sCols.getValue()).intValue();
			} else if (e.getSource() == sDiam) {
				configCircle.shapeSize = ((Number)sDiam.getValue()).doubleValue();
			} else if (e.getSource() == sDist) {
				configCircle.shapeDistance = ((Number)sDist.getValue()).doubleValue();
			}
			updateParameters();
		}
	}

	private class CircleHexPanel extends StandardAlgConfigPanel implements ChangeListener {

		JSpinner sRows, sCols;
		JSpinner sDiam, sDist;

		public CircleHexPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sRows = spinner(configCircleHex.numRows, 1, 1000, 1);
			sCols = spinner(configCircleHex.numCols, 1, 1000, 1);
			sDiam = spinner(configCircleHex.shapeSize, 0, 1000000.0, 1);
			sDist = spinner(configCircleHex.shapeDistance, 0, 1000000.0, 1);

			addLabeled(sRows, "Rows");
			addLabeled(sCols, "Cols");
			addLabeled(sDiam, "Circle Diameter");
			addLabeled(sDist, "Center Distance");
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == sRows) {
				configCircleHex.numRows = ((Number)sRows.getValue()).intValue();
			} else if (e.getSource() == sCols) {
				configCircleHex.numCols = ((Number)sCols.getValue()).intValue();
			} else if (e.getSource() == sDiam) {
				configCircleHex.shapeSize = ((Number)sDiam.getValue()).doubleValue();
			} else if (e.getSource() == sDist) {
				configCircleHex.shapeDistance = ((Number)sDist.getValue()).doubleValue();
			}
			updateParameters();
		}
	}

	public interface Listener {
		void calibrationParametersChanged( CalibrationPatterns type, ConfigGridDimen config );
	}
}
