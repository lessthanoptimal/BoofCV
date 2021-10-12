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

package boofcv.app;

import boofcv.alg.fiducial.square.FiducialSquareHammingGenerator;
import boofcv.app.fiducials.CreateSquareFiducialControlPanel;
import boofcv.app.fiducials.CreateSquareFiducialGui;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.io.image.ConvertBufferedImage;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;

/**
 * GUI for printing square binary fiducials
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareHammingGui extends CreateSquareFiducialGui {

	ControlPanel controls;
	CreateFiducialSquareHamming c = new CreateFiducialSquareHamming();

	public CreateFiducialSquareHammingGui() {
		super("hamming");
		c.markerWidth = 1;
		c.finishParsing();
		generator = new FiducialSquareHammingGenerator(c.config);
		generator.setRenderer(render);
		controls = new ControlPanel(this);
		super.controls = controls;
		setupGui(controls, "Fiducial Square Hamming");
	}

	@Override
	protected void saveFile( boolean sendToPrinter ) {
		if (controls.patterns.size == 0)
			return;
		c.sendToPrinter = sendToPrinter;
		c.unit = controls.documentUnits;
		c.paperSize = controls.paperSize;
		if (controls.format.equalsIgnoreCase("pdf")) {
			c.markerWidth = (float)controls.markerWidthUnits;
		} else {
			c.markerWidth = controls.markerWidthPixels;
		}
		c.spaceBetween = c.markerWidth/4;
		c.gridFill = controls.fillGrid;
		c.drawGrid = controls.drawGrid;
		c.hideInfo = controls.hideInfo;
		c.numbers = new Long[controls.patterns.size];
		for (int i = 0; i < controls.patterns.size; i++) {
			c.numbers[i] = (long)controls.patterns.get(i);
		}

		saveFile(sendToPrinter, c);
	}

	@Override protected void showHelp() {}

	@Override
	protected void renderPreview() {
		long pattern = controls.selectedPattern;
		if (pattern < 0) {
			imagePanel.setImageRepaint(null);
		} else if (pattern >= c.config.encoding.size()) {
			System.err.println("Pattern outside of allowed range");
			imagePanel.setImageRepaint(null);
		} else {
			FiducialSquareHammingGenerator generator = (FiducialSquareHammingGenerator)this.generator;
			generator.generate(controls.selectedPattern);
			ConvertBufferedImage.convertTo(render.getGray(), buffered, true);
			imagePanel.setImageRepaint(buffered);
		}
	}

	class ControlPanel extends CreateSquareFiducialControlPanel {

		int selectedPattern = -1;
		int encoding = c.config.dictionary.ordinal() - 1;

		JLabel labelMaxID = new JLabel();
		DefaultListModel<Long> listModel = new DefaultListModel<>();
		JList<Long> listPatterns = new JList<>(listModel);
		DogArray_I32 patterns = new DogArray_I32();
		JComboBox<String> comboEncoding = combo(encoding, (Object[])HammingDictionary.allPredefined());

		public ControlPanel( Listener listener ) {
			super(listener);

			labelMaxID.setText(c.config.encoding.size() + "");
			listPatterns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			listPatterns.setLayoutOrientation(JList.VERTICAL);
//			listPatterns.setVisibleRowCount(-1);
			listPatterns.addListSelectionListener(e -> {
				int s = listPatterns.getSelectedIndex();
				if (s >= 0) {
					selectedPattern = patterns.get(s);
				} else {
					selectedPattern = -1;
				}
				renderPreview();
			});

			add(new JScrollPane(listPatterns));
			addLabeled(labelMaxID, "Total Markers");
			addLabeled(comboEncoding, "Encoding");
			layoutComponents(false);
		}

		@Override public void controlChanged( final Object source ) {
			if (source == comboEncoding) {
				encoding = comboEncoding.getSelectedIndex();
				HammingDictionary dictionary = HammingDictionary.values()[encoding + 1];
				c.config.setTo(ConfigHammingMarker.loadDictionary(dictionary));
				labelMaxID.setText(c.config.encoding.size() + "");
				renderPreview();
			} else {
				super.controlChanged(source);
			}
		}

		@Override
		public void handleAddPattern() {
			String text = JOptionPane.showInputDialog("Enter ID", "0");
			try {
				int lvalue = Integer.parseInt(text);

				int maxValue = c.config.encoding.size() - 1;
				if (lvalue > maxValue)
					lvalue = maxValue;
				else if (lvalue < 0)
					lvalue = 0;

				listModel.add(listModel.size(), (long)lvalue);
				patterns.add(lvalue);
				listPatterns.setSelectedIndex(listModel.size() - 1);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Must be an integer!");
			}
		}

		@Override
		public void handleRemovePattern() {
			int selected = listPatterns.getSelectedIndex();
			if (selected >= 0) {
				listModel.removeElementAt(selected);
				patterns.remove(selected);
			}
		}
	}

	public static void main( String[] args ) {
		SwingUtilities.invokeLater(CreateFiducialSquareHammingGui::new);
	}
}
