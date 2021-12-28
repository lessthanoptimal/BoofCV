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

import boofcv.alg.fiducial.square.FiducialSquareGenerator;
import boofcv.app.fiducials.CreateSquareFiducialControlPanel;
import boofcv.app.fiducials.CreateSquareFiducialGui;
import boofcv.gui.BoofSwingUtil;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

/**
 * GUI for creating fiducials with a square black border
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareImageGui extends CreateSquareFiducialGui {
	ControlPanel controls;

	public CreateFiducialSquareImageGui() {
		super("square_image");
		generator = new FiducialSquareGenerator(render);
		controls = new ControlPanel(this);
		setupGui(controls, "Square Image Fiducial");
	}

	@Override
	protected void saveFile( boolean sendToPrinter ) {
		if (controls.patterns.size() == 0)
			return;
		CreateFiducialSquareImage c = new CreateFiducialSquareImage();
		c.sendToPrinter = sendToPrinter;
		c.unit = controls.documentUnits;
		c.paperSize = controls.paperSize;
		c.blackBorderFractionalWidth = (float)controls.borderFraction;
		if (controls.format.equalsIgnoreCase("pdf")) {
			c.markerWidth = (float)controls.markerWidthUnits;
		} else {
			c.markerWidth = controls.markerWidthPixels;
		}
		c.spaceBetween = c.markerWidth/4;
		c.gridFill = controls.fillGrid;
		c.hideInfo = controls.hideInfo;
		c.imagePaths.addAll(controls.patterns);

		saveFile(sendToPrinter, c);
	}

	@Override
	protected void showHelp() {

	}

	@Override
	protected void renderPreview() {
		String path = controls.selectedPattern;
		if (path == null) {
			imagePanel.setImageRepaint(null);
		} else {
			FiducialSquareGenerator generator = (FiducialSquareGenerator)this.generator;
			BufferedImage buffered = UtilImageIO.loadImageNotNull(path);
			GrayU8 gray = ConvertBufferedImage.convertFrom(buffered, (GrayU8)null);
			generator.setBlackBorder(controls.borderFraction);
			generator.generate(gray);
			buffered = ConvertBufferedImage.convertTo(render.getGray(), null, true);
			imagePanel.setImageRepaint(buffered);
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	class ControlPanel extends CreateSquareFiducialControlPanel {

		DefaultListModel<String> listModel = new DefaultListModel<>();
		JList<String> listPatterns = new JList<>(listModel);
		java.util.List<String> patterns = new ArrayList<>();

		@Nullable String selectedPattern = null;

		public ControlPanel( Listener listener ) {
			super(listener);

			listPatterns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			listPatterns.setLayoutOrientation(JList.VERTICAL);
//			listPatterns.setVisibleRowCount(-1);
			listPatterns.addListSelectionListener(e -> {
				int s = listPatterns.getSelectedIndex();
				if (s >= 0) {
					selectedPattern = patterns.get(s);
				} else {
					selectedPattern = null;
				}
				renderPreview();
			});

			add(new JScrollPane(listPatterns));
			layoutComponents(true);
		}

		@Override
		public void handleAddPattern() {
			File path = BoofSwingUtil.openFileChooser(this, BoofSwingUtil.FileTypes.IMAGES);
			if (path == null)
				return;
			listModel.add(listModel.size(), path.getName());
			patterns.add(path.getAbsolutePath());
			listPatterns.setSelectedIndex(listModel.size() - 1);
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
		SwingUtilities.invokeLater(CreateFiducialSquareImageGui::new);
	}
}
