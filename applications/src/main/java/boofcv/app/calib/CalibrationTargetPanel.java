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
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.JCheckBoxValue;
import boofcv.gui.controls.JSpinnerNumber;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
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
	public ConfigECoCheckMarkers configECoCheck = ConfigECoCheckMarkers.singleShape(9, 7, 1, 1);
	public ConfigGridDimen configSquare = new ConfigGridDimen(4, 3, 1, 1);
	public ConfigGridDimen configCircle = new ConfigGridDimen(15, 10, 1, 1.5);
	public ConfigGridDimen configCircleHex = new ConfigGridDimen(15, 15, 1, 1.5);
	public ConfigHammingChessboard configHammingChess = ConfigHammingChessboard.create(HammingDictionary.ARUCO_MIP_25h7, 8, 6);
	public ConfigHammingGrid configHammingGrid = ConfigHammingGrid.create(HammingDictionary.ARUCO_MIP_25h7, 6, 4, 0.4);

	public CalibrationTargetPanel( Listener listener ) {
		setBorder(BorderFactory.createEmptyBorder());

		this.listener = listener;
		comboType = new JComboBox<>(CalibrationPatterns.values());
		comboType.addActionListener(this);
		comboType.setMaximumSize(comboType.getPreferredSize());

		panelTarget.setLayout(new BorderLayout());
		panelTarget.setPreferredSize(new Dimension(250, 180));
		panelTarget.setMaximumSize(panelTarget.getPreferredSize());
		changeTargetPanel();

		addLabeled(comboType, "Target Type");
		add(Box.createRigidArea(new Dimension(10, 10)));
		addAlignCenter(panelTarget);
	}

	public void updateParameters() {
		Object c = switch (selected) {
			case CHESSBOARD -> configChessboard;
			case ECOCHECK -> configECoCheck;
			case SQUARE_GRID -> configSquare;
			case CIRCLE_GRID -> configCircle;
			case CIRCLE_HEXAGONAL -> configCircleHex;
			case HAMMING_CHESSBOARD -> configHammingChess;
			case HAMMING_GRID -> configHammingGrid;
		};
		listener.calibrationParametersChanged(selected, c);
	}

	@Override
	public void controlChanged( Object source ) {
		if (source == comboType) {
			selected = (CalibrationPatterns)comboType.getSelectedItem();
			changeTargetPanel();
			updateParameters();
		}
	}

	public void changeTargetPanel() {
		JPanel p = switch (selected) {
			case CHESSBOARD -> new ChessPanel();
			case ECOCHECK -> new EcoCheckPanel();
			case SQUARE_GRID -> new SquareGridPanel();
			case CIRCLE_GRID -> new CircleGridPanel();
			case CIRCLE_HEXAGONAL -> new CircleHexPanel();
			case HAMMING_CHESSBOARD -> new HammingChessPanel();
			case HAMMING_GRID -> new HammingGridPanel();
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

			addLabeled(sRows, "Rows", "Number of square rows");
			addLabeled(sCols, "Cols", "Number of square columns");
			addLabeled(sWidth, "Square Width", "How wide each square is");
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

	private class EcoCheckPanel extends StandardAlgConfigPanel implements ChangeListener {
		JSpinner sRows, sCols, sWidth, sMarkers;
		JComboBox<String> comboErrorLevel;

		public EcoCheckPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			ConfigECoCheckMarkers.MarkerShape shape = configECoCheck.markerShapes.get(0);

			sRows = spinner(shape.numRows, 1, 1000, 1);
			sCols = spinner(shape.numCols, 1, 1000, 1);
			sWidth = spinner(shape.squareSize, 0, 1000000.0, 1);
			sMarkers = spinner(configECoCheck.firstTargetDuplicated, 1, 1000, 1);
			comboErrorLevel = combo(configECoCheck.errorCorrectionLevel, "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

			addLabeled(sRows, "Rows", "Number of square rows");
			addLabeled(sCols, "Cols", "Number of square columns");
			addLabeled(sWidth, "Square Width", "How wide each square is");
			addLabeled(sMarkers, "Count", "Number of unique markers");
			addLabeled(comboErrorLevel, "Error Level", "Amount of error correction. 0 = none. 10 = max.");
		}

		@Override
		public void controlChanged( Object source ) {
			ConfigECoCheckMarkers.MarkerShape shape = configECoCheck.markerShapes.get(0);

			if (source == sRows) {
				shape.numRows = ((Number)sRows.getValue()).intValue();
			} else if (source == sCols) {
				shape.numCols = ((Number)sCols.getValue()).intValue();
			} else if (source == sWidth) {
				shape.squareSize = ((Number)sWidth.getValue()).doubleValue();
			} else if (source == sMarkers) {
				configECoCheck.firstTargetDuplicated = ((Number)sMarkers.getValue()).intValue();
			} else if (source == comboErrorLevel) {
				configECoCheck.errorCorrectionLevel = comboErrorLevel.getSelectedIndex();
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

			addLabeled(sRows, "Rows", "Number of square rows");
			addLabeled(sCols, "Cols", "Number of square columns");
			addLabeled(sWidth, "Square Width", "How wide each square is");
			addLabeled(sSpace, "Space Width", "Space between squares");
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

	private class HammingChessPanel extends StandardAlgConfigPanel implements ChangeListener {
		JSpinnerNumber sRows = spinnerWrap(configHammingChess.numRows, 1, 1000, 1);
		JSpinnerNumber sCols = spinnerWrap(configHammingChess.numCols, 1, 1000, 1);
		JSpinnerNumber sWidth = spinnerWrap(configHammingChess.squareSize, 0, 1000000.0, 1);
		JSpinnerNumber sScale = spinnerWrap(configHammingChess.markerScale, 0, 1.0, 0.02);
		JComboBox<?> cDict = combo(configHammingChess.markers.dictionary.ordinal(), (Object[])HammingDictionary.allPredefined());
		JSpinnerNumber sOffset = spinnerWrap(configHammingChess.markerOffset, 0, 200, 1);
		JCheckBoxValue cEven = checkboxWrap("Even pattern", configHammingChess.chessboardEven);

		public HammingChessPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sScale.spinner.setPreferredSize(sRows.spinner.getPreferredSize());
			sScale.spinner.setMaximumSize(sRows.spinner.getMaximumSize());

			addLabeled(sRows.spinner, "Rows", "Number of square rows");
			addLabeled(sCols.spinner, "Cols", "Number of square columns");
			addAlignCenter(cEven.check,"Chessboard is an even or odd pattern");
			addLabeled(sWidth.spinner, "Square Width", "How wide each square is");
			addLabeled(sScale.spinner, "Marker Scale", "Relative size of markers");
			addLabeled(cDict, "Dictionary", "Encoding dictionary");
			addLabeled(sOffset.spinner, "Marker Offset", "Index of the first marker");
		}

		@Override public void controlChanged( final Object source ) {
			if (source == sRows.spinner) {
				sRows.updateValue();
				configHammingChess.numRows = sRows.value.intValue();
			} else if (source == sCols.spinner) {
				sCols.updateValue();
				configHammingChess.numCols = sCols.value.intValue();
			} else if (source == cEven.check) {
				cEven.updateValue();
				configHammingChess.chessboardEven = cEven.value;
			} else if (source == sWidth.spinner) {
				sWidth.updateValue();
				configHammingChess.squareSize = sWidth.value.doubleValue();
			} else if (source == sScale.spinner) {
				sScale.updateValue();
				configHammingChess.markerScale = sScale.value.doubleValue();
			} else if (source == cDict) {
				HammingDictionary dictionary = HammingDictionary.valueOf((String)cDict.getSelectedItem());
				configHammingChess.markers.setTo(ConfigHammingMarker.loadDictionary(dictionary));
			} else if (source == sOffset.spinner) {
				sOffset.updateValue();
				configHammingChess.markerOffset = sOffset.value.intValue();
			}
			updateParameters();
		}
	}

	private class HammingGridPanel extends StandardAlgConfigPanel implements ChangeListener {
		JSpinnerNumber sRows = spinnerWrap(configHammingGrid.numRows, 1, 1000, 1);
		JSpinnerNumber sCols = spinnerWrap(configHammingGrid.numCols, 1, 1000, 1);
		JSpinnerNumber sWidth = spinnerWrap(configHammingGrid.squareSize, 0, 1000000.0, 1);
		JSpinnerNumber sSpace = spinnerWrap(configHammingGrid.spaceToSquare, 0, 1.0, 0.02);
		JComboBox<?> cDict = combo(configHammingGrid.markers.dictionary.ordinal(), (Object[])HammingDictionary.allPredefined());
		JSpinnerNumber sOffset = spinnerWrap(configHammingGrid.markerOffset, 0, 200, 1);

		public HammingGridPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sSpace.spinner.setPreferredSize(sRows.spinner.getPreferredSize());
			sSpace.spinner.setMaximumSize(sRows.spinner.getMaximumSize());

			addLabeled(sRows.spinner, "Rows", "Number of square rows");
			addLabeled(sCols.spinner, "Cols", "Number of square columns");
			addLabeled(sWidth.spinner, "Square Width", "How wide each square is");
			addLabeled(sSpace.spinner, "Space", "Space between squares in units of squares");
			addLabeled(cDict, "Dictionary", "Encoding dictionary");
			addLabeled(sOffset.spinner, "Marker Offset", "Index of the first marker");
		}

		@Override public void controlChanged( final Object source ) {
			if (source == sRows.spinner) {
				sRows.updateValue();
				configHammingGrid.numRows = sRows.value.intValue();
			} else if (source == sCols.spinner) {
				sCols.updateValue();
				configHammingGrid.numCols = sCols.value.intValue();
			} else if (source == sWidth.spinner) {
				sWidth.updateValue();
				configHammingGrid.squareSize = sWidth.value.doubleValue();
			} else if (source == sSpace.spinner) {
				sSpace.updateValue();
				configHammingGrid.spaceToSquare = sSpace.value.doubleValue();
			} else if (source == cDict) {
				HammingDictionary dictionary = HammingDictionary.valueOf((String)cDict.getSelectedItem());
				configHammingGrid.markers.setTo(ConfigHammingMarker.loadDictionary(dictionary));
			} else if (source == sOffset.spinner) {
				sOffset.updateValue();
				configHammingGrid.markerOffset = sOffset.value.intValue();
			}
			updateParameters();
		}
	}

	public interface Listener {
		void calibrationParametersChanged( CalibrationPatterns type, Object config );
	}
}
