/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.fiducial.calib.ConfigCircleHexagonalGrid;
import boofcv.abst.fiducial.calib.ConfigCircleRegularGrid;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
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
public class CalibrationTargetPanel extends StandardAlgConfigPanel implements ActionListener
{
	JComboBox<TargetType> comboType;
	JPanel panelTarget = new JPanel();

	Listener listener;

	public TargetType selected = TargetType.CHESSBOARD;

	public ConfigChessboard configChessboard = new ConfigChessboard(7,5,1);
	public ConfigSquareGrid configSquare = new ConfigSquareGrid(4,3,1,1);
	public ConfigCircleRegularGrid configCircle = new ConfigCircleRegularGrid(15,10,1,1.5);
	public ConfigCircleHexagonalGrid configCircleHex = new ConfigCircleHexagonalGrid(15,15,1,1.5);

	public CalibrationTargetPanel( Listener listener ) {
		setBorder(BorderFactory.createEmptyBorder());

		this.listener = listener;
		comboType = new JComboBox<>(TargetType.values());
		comboType.addActionListener(this);
		comboType.setMaximumSize(comboType.getPreferredSize());

		panelTarget.setLayout(new BorderLayout());
		panelTarget.setPreferredSize(new Dimension(250,106));
		panelTarget.setMaximumSize(panelTarget.getPreferredSize());
		changeTargetPanel();

		addLabeled(comboType,"Target Type");
		add(Box.createRigidArea(new Dimension(10,10)));
		addAlignCenter(panelTarget);
	}

	public void updateParameters() {
		Object c;
		switch( selected ) {
			case CHESSBOARD:c=configChessboard;break;
			case SQUARE_GRID:c=configSquare;break;
			case CIRCLE_GRID:c=configCircle;break;
			case CIRCLE_HEX:c=configCircleHex;break;
			default: throw new RuntimeException("Unknown");
		}
		listener.calibrationParametersChanged(selected,c);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == comboType ) {
			selected = (TargetType)comboType.getSelectedItem();
			changeTargetPanel();
			updateParameters();
		}
	}

	public void changeTargetPanel() {

		JPanel p;
		switch(selected) {
			case CHESSBOARD:p=new ChessPanel();break;
			case SQUARE_GRID:p=new SquareGridPanel();break;
			case CIRCLE_GRID:p=new CircleGridPanel();break;
			case CIRCLE_HEX:p=new CircleHexPanel();break;
			default:throw new RuntimeException("Unknown");
		}

		panelTarget.removeAll();
		panelTarget.add(BorderLayout.CENTER,p);
		panelTarget.validate();
		panelTarget.repaint();
	}

	private class ChessPanel extends StandardAlgConfigPanel implements ChangeListener {

		JSpinner sRows,sCols,sWidth;

		public ChessPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sRows = spinner(configChessboard.numRows,1,1000,1);
			sCols = spinner(configChessboard.numCols,1,1000,1);
			sWidth = spinner(configChessboard.squareWidth,0,1000000.0,1);

			addLabeled(sRows,"Rows");
			addLabeled(sCols,"Cols");
			addLabeled(sWidth,"Square Width");
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == sRows ) {
				configChessboard.numRows = ((Number)sRows.getValue()).intValue();
			} else if( e.getSource() == sCols ) {
				configChessboard.numCols = ((Number)sCols.getValue()).intValue();
			} else if( e.getSource() == sWidth ) {
				configChessboard.squareWidth = ((Number)sWidth.getValue()).doubleValue();
			}
			updateParameters();
		}
	}

	private class SquareGridPanel extends StandardAlgConfigPanel implements ChangeListener {

		JSpinner sRows,sCols;
		JSpinner sWidth,sSpace;

		public SquareGridPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sRows = spinner(configSquare.numRows,1,1000,1);
			sCols = spinner(configSquare.numCols,1,1000,1);
			sWidth = spinner(configSquare.squareWidth,0,1000000.0,1);
			sSpace = spinner(configSquare.spaceWidth,0,1000000.0,1);

			addLabeled(sRows,"Rows");
			addLabeled(sCols,"Cols");
			addLabeled(sWidth,"Square Width");
			addLabeled(sSpace,"Space Width");
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == sRows ) {
				configSquare.numRows = ((Number)sRows.getValue()).intValue();
			} else if( e.getSource() == sCols ) {
				configSquare.numCols = ((Number)sCols.getValue()).intValue();
			} else if( e.getSource() == sWidth ) {
				configSquare.squareWidth = ((Number)sWidth.getValue()).doubleValue();
			} else if( e.getSource() == sSpace ) {
				configSquare.spaceWidth = ((Number)sSpace.getValue()).doubleValue();
			}
			updateParameters();
		}
	}

	private class CircleGridPanel extends StandardAlgConfigPanel implements ChangeListener  {

		JSpinner sRows,sCols;
		JSpinner sDiam, sDist;

		public CircleGridPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sRows = spinner(configCircle.numRows,1,1000,1);
			sCols = spinner(configCircle.numCols,1,1000,1);
			sDiam = spinner(configCircle.circleDiameter,0,1000000.0,1);
			sDist = spinner(configCircle.centerDistance,0,1000000.0,1);

			addLabeled(sRows,"Rows");
			addLabeled(sCols,"Cols");
			addLabeled(sDiam,"Circle Diameter");
			addLabeled(sDist,"Center Distance");
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == sRows ) {
				configCircle.numRows = ((Number)sRows.getValue()).intValue();
			} else if( e.getSource() == sCols ) {
				configCircle.numCols = ((Number)sCols.getValue()).intValue();
			} else if( e.getSource() == sDiam) {
				configCircle.circleDiameter = ((Number) sDiam.getValue()).doubleValue();
			} else if( e.getSource() == sDist) {
				configCircle.centerDistance = ((Number) sDist.getValue()).doubleValue();
			}
			updateParameters();
		}
	}

	private class CircleHexPanel extends StandardAlgConfigPanel implements ChangeListener {

		JSpinner sRows,sCols;
		JSpinner sDiam, sDist;

		public CircleHexPanel() {
			setBorder(BorderFactory.createEmptyBorder());

			sRows = spinner(configCircleHex.numRows,1,1000,1);
			sCols = spinner(configCircleHex.numCols,1,1000,1);
			sDiam = spinner(configCircleHex.circleDiameter,0,1000000.0,1);
			sDist = spinner(configCircleHex.centerDistance,0,1000000.0,1);

			addLabeled(sRows,"Rows");
			addLabeled(sCols,"Cols");
			addLabeled(sDiam,"Circle Diameter");
			addLabeled(sDist,"Center Distance");
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == sRows ) {
				configCircleHex.numRows = ((Number)sRows.getValue()).intValue();
			} else if( e.getSource() == sCols ) {
				configCircleHex.numCols = ((Number)sCols.getValue()).intValue();
			} else if( e.getSource() == sDiam) {
				configCircleHex.circleDiameter = ((Number) sDiam.getValue()).doubleValue();
			} else if( e.getSource() == sDist) {
				configCircleHex.centerDistance = ((Number) sDist.getValue()).doubleValue();
			}
			updateParameters();
		}
	}

	public enum TargetType {
		CHESSBOARD,
		SQUARE_GRID,
		CIRCLE_HEX,
		CIRCLE_GRID
	}

	public interface Listener {
		void calibrationParametersChanged( TargetType type , Object config );
	}

}
