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

package boofcv.app;

import boofcv.app.markers.CreateSquareFiducialControlPanel;
import boofcv.app.markers.CreateSquareFiducialGui;
import boofcv.io.image.ConvertBufferedImage;
import org.ddogleg.struct.GrowQueue_I64;

import javax.swing.*;

/**
 * GUI for printing square binary fiducials
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareBinaryGui extends CreateSquareFiducialGui {

	ControlPanel controls = new ControlPanel(this);

	public CreateFiducialSquareBinaryGui() {
		super("binary");
		setupGui(controls,"Fiducial Square Binary");
	}

	@Override
	protected void saveFile( boolean sendToPrinter ) {
		if( controls.patterns.size == 0 )
			return;
		CreateFiducialSquareBinary c = new CreateFiducialSquareBinary();
		c.sendToPrinter = sendToPrinter;
		c.unit = controls.documentUnits;
		c.paperSize = controls.paperSize;
		c.markerWidth = (float)controls.markerWidth;
		c.spaceBetween = c.markerWidth/4;
		c.gridWidth = controls.gridWidth;
		c.gridFill = controls.fillGrid;
		c.drawGrid = controls.drawGrid;
		c.hideInfo = controls.hideInfo;
		c.numbers = new Long[controls.patterns.size];
		for (int i = 0; i < controls.patterns.size; i++) {
			c.numbers[i] = controls.patterns.get(i);
		}

		saveFile(sendToPrinter, c);
	}

	@Override
	protected void showHelp() {

	}

	@Override
	protected void renderPreview() {
		long pattern = controls.selectedPattern;
		if( pattern <= 0 ) {
			imagePanel.setImageRepaint(null);
		} else {
			generator.setBlackBorder(controls.borderFraction);
			generator.generate(controls.selectedPattern,controls.gridWidth);
			ConvertBufferedImage.convertTo(render.getGray(),buffered,true);
			imagePanel.setImageRepaint(buffered);
		}
	}

	class ControlPanel extends CreateSquareFiducialControlPanel {

		DefaultListModel<Long> listModel = new DefaultListModel<>();
		JList<Long> listPatterns = new JList<>(listModel);
		GrowQueue_I64 patterns = new GrowQueue_I64();
		JSpinner spinnerGridWidth;

		long selectedPattern =-1;
		int gridWidth = 4;

		public ControlPanel(Listener listener) {
			super(listener);

			listPatterns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			listPatterns.setLayoutOrientation(JList.VERTICAL);
//			listPatterns.setVisibleRowCount(-1);
			listPatterns.addListSelectionListener(e -> {
				int s = listPatterns.getSelectedIndex();
				if( s >= 0 ) {
					selectedPattern = patterns.get(s);
				} else {
					selectedPattern = -1;
				}
				renderPreview();
			});

			spinnerGridWidth = spinner(gridWidth,2,8,1,
					e->{
						gridWidth=((Number)spinnerGridWidth.getValue()).intValue();
						if( listener != null )
							listener.controlsUpdates();
					});

			add( new JScrollPane(listPatterns));
			addLabeled(spinnerGridWidth,"Grid Width");
			layoutComponents();
		}

		@Override
		public void handleAddPattern() {
			String text = JOptionPane.showInputDialog("Enter ID","1234");
			try {
				long lvalue = Long.parseLong(text);

				long maxValue = (long)(Math.pow(2,gridWidth*gridWidth)-4);
				if( lvalue > maxValue )
					lvalue = maxValue;
				else if( lvalue < 0 )
					lvalue = 0;

				listModel.add(listModel.size(), lvalue);
				patterns.add(lvalue);
				listPatterns.setSelectedIndex(listModel.size()-1);
			} catch( NumberFormatException e ) {
				JOptionPane.showMessageDialog(this,"Must be an integer!");
			}
		}

		@Override
		public void handleRemovePattern() {
			int selected = listPatterns.getSelectedIndex();
			if( selected >= 0 ) {
				listModel.removeElementAt(selected);
				patterns.remove(selected);
			}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(CreateFiducialSquareBinaryGui::new);
	}
}
