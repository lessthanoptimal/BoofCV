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

import boofcv.alg.fiducial.dots.RandomDotMarkerGenerator;
import boofcv.alg.fiducial.dots.RandomDotMarkerGeneratorImage;
import boofcv.generate.Unit;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.fiducial.RandomDotDefinition;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Displays a GUI for generating random dot markers then printing them or saving them to disk
 *
 * @author Peter Abeles
 */
public class CreateFiducialRandomDotGui extends JPanel {

	// The command line equivalent of this class. Several of its functions are used
	CreateFiducialRandomDot owner;
	// control panel on left side
	ControlPanel controls;
	// Displays a preview
	ImagePanel imagePanel = new ImagePanel();
	// Window it's shown in
	JFrame frame;

	// Used to define the markers when showing preview
	RandomDotDefinition def = new RandomDotDefinition();

	public CreateFiducialRandomDotGui() {
		this(new CreateFiducialRandomDot());
	}

	public CreateFiducialRandomDotGui( CreateFiducialRandomDot owner ) {
		setLayout(new BorderLayout());
		this.owner = owner;

		// If not specified by the command line fill in these values with something reasonable
		if (owner.markerWidth <= 0)
			owner.markerWidth = 10.0f;
		if (owner.unit == null)
			owner.unit = Unit.CENTIMETER;
		if (owner.paperSize == null)
			owner.paperSize = PaperSize.LETTER;
		if (owner.spaceBetween == 0)
			owner.spaceBetween = owner.markerWidth/4;

		controls = new ControlPanel();

		// Configure preview image. Only scale down automatically and center the image always
		imagePanel.setScaling(ScaleOptions.DOWN);
		imagePanel.setCentering(true);
		imagePanel.setBackground(Color.GRAY);

		updateMarkers();

		add(controls, BorderLayout.WEST);
		add(imagePanel, BorderLayout.CENTER);

		setPreferredSize(new Dimension(700, 500));
		frame = ShowImages.setupWindow(this, "Create Random Dot Markers", true);
		createMenuBar();
		frame.setVisible(true);
	}

	void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		JMenuItem menuSave = BoofSwingUtil.createMenuItem("Save", KeyEvent.VK_S, KeyEvent.VK_S,
				() -> saveFile(false));
		JMenuItem menuPrint = BoofSwingUtil.createMenuItem("Print...", KeyEvent.VK_P, KeyEvent.VK_P,
				() -> saveFile(true));
		JMenuItem menuQuit = BoofSwingUtil.createMenuItem("Quit", KeyEvent.VK_Q, KeyEvent.VK_Q,
				() -> System.exit(0));

		menuFile.addSeparator();
		menuFile.add(menuSave);
		menuFile.add(menuPrint);
		menuFile.add(menuQuit);
		menuBar.add(menuFile);

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		JMenuItem menuCut = new JMenuItem(new DefaultEditorKit.CutAction());
		menuCut.setText("Cut");
		BoofSwingUtil.setMenuItemKeys(menuCut, KeyEvent.VK_T, KeyEvent.VK_X);
		JMenuItem menuCopy = new JMenuItem(new DefaultEditorKit.CopyAction());
		menuCopy.setText("Copy");
		BoofSwingUtil.setMenuItemKeys(menuCopy, KeyEvent.VK_C, KeyEvent.VK_C);
		JMenuItem menuPaste = new JMenuItem(new DefaultEditorKit.PasteAction());
		menuPaste.setText("Paste");
		BoofSwingUtil.setMenuItemKeys(menuPaste, KeyEvent.VK_P, KeyEvent.VK_V);

		editMenu.add(menuCut);
		editMenu.add(menuCopy);
		editMenu.add(menuPaste);
		menuBar.add(editMenu);

		frame.setJMenuBar(menuBar);
	}

	private void saveFile( boolean sendToPrinter ) {
		// grab the focus and force what the user is editing to be saved

		File f;

		// see where the document is to be sent
		if (sendToPrinter) {
			if (owner.fileType.compareToIgnoreCase("pdf") != 0) {
				JOptionPane.showMessageDialog(this, "Must select PDF document type to print");
				return;
			}
			f = new File(""); // dummy to make the code below happy and less complex
		} else {
			f = FileSystemView.getFileSystemView().getHomeDirectory();
			f = new File(f, "dotmarker." + owner.fileType);

			f = BoofSwingUtil.fileChooser(null, this, false, f.getPath(), ( s ) -> {
				File parent = new File(s).getParentFile();
				String base = FilenameUtils.getBaseName(s);
				File ff = new File(parent, base + "." + owner.fileType);
				System.out.println(s);
				System.out.println(ff.getPath());
				return ff.getPath();
			});
			if (f == null) {
				return;
			}

			if (f.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Can't save to a directory!");
				return;
			}
		}

		// Make sure the file has the correct extension
		String outputFile = f.getAbsolutePath();
		String ext = FilenameUtils.getExtension(outputFile);
		if (ext.compareToIgnoreCase(owner.fileType) != 0) {
			outputFile = FilenameUtils.removeExtension(outputFile);
			outputFile += "." + owner.fileType;
		}

		owner.dumpLocations = true;
		owner.fileName = outputFile;
		owner.sendToPrinter = sendToPrinter;
		owner.markerHeight = (float)(owner.markerWidth*controls.markerRatio);

		try {
			owner.run();
		} catch (IOException | RuntimeException e) {
			System.out.println("Exception!!!");
			BoofSwingUtil.warningDialog(this, e);
		}
	}

	/**
	 * Update the markers because their configuration changed
	 */
	public void updateMarkers() {
		// Update the dots definitions
		def.randomSeed = owner.randomSeed;
		def.maxDotsPerMarker = owner.maxDotsPerMarker;
		def.dotDiameter = owner.dotDiameter;
		def.markerWidth = owner.markerWidth;
		def.markerHeight = owner.markerWidth*controls.markerRatio;
		def.units = owner.unit.getAbbreviation();
		owner.markers.clear();

		// Decide how close two dots can be
		double spacingDiameter = controls.spaceDiameter <= 0 ? def.dotDiameter : controls.spaceDiameter;

		// Create all the dots
		Random rand = new Random(def.randomSeed);
		for (int i = 0; i < controls.totalMarkers; i++) {
			List<Point2D_F64> marker = RandomDotMarkerGenerator.createRandomMarker(rand,
					def.maxDotsPerMarker, def.markerWidth, def.markerHeight, spacingDiameter);
			owner.markers.add(marker);
		}

		updateRendering();
	}

	/**
	 * Render the markers again and show the results
	 */
	public void updateRendering() {
		// If PDF always use a 600x600 image, otherwise show the actual rendered image which will be saved
		int width = owner.fileType.equals("pdf") ? 600 : (int)owner.markerWidth;

		// dot diameter rendered in the image
		double dd = owner.dotDiameter*(width/owner.markerWidth);
		int height = (int)(width*controls.markerRatio);

		// Generate the preview image
		var generator = new RandomDotMarkerGeneratorImage();
		generator.setRadius(dd/2.0);
		generator.configure(width, height, 20);
		generator.render(owner.markers.get(controls.viewMarkerIndex), def.markerWidth, def.markerHeight);

		GrayU8 gray = generator.getImage();
		BufferedImage out = new BufferedImage(gray.width, gray.height, BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(gray, out);
		imagePanel.setImageRepaint(out);
	}

	public boolean isPixels() {
		return !owner.fileType.equals("pdf");
	}

	/**
	 * Control panel which lets the user configure most of the options
	 */
	protected class ControlPanel extends StandardAlgConfigPanel {

		int totalMarkers = 1;       // number of markers it will generate
		double spaceDiameter = -1;  // space between the markers
		int viewMarkerIndex = 0;    // which marker is being displayed in the preview
		double markerRatio = 1.0;   // height / width ratio of the marker

		// Store two different sets of units for image vs pdf since if you interchange the two you get very bad results
		double widthPdf = owner.markerWidth;
		int widthPixels = 600;
		double diameterPdf = owner.dotDiameter;
		int diameterPixels = 30;

		JComboBox<String> comboOutputFormat = combo(0, "pdf", "png", "bmp", "jpg", "ppm", "pgm");
		JComboBox<String> comboPaper = combo(PaperSize.values().indexOf(owner.paperSize), PaperSize.values().toArray());
		JComboBox<String> comboUnits = combo(owner.unit.ordinal(), (Object[])Unit.values());
		JFormattedTextField fieldMarkerWidth = BoofSwingUtil.createTextField(owner.markerWidth, 0.0, Double.NaN);
		JSpinner spinnerRatio = spinner(markerRatio, 0.0001, 1000.0, 0.05);
		JFormattedTextField fieldRandomSeed = BoofSwingUtil.createHexTextField(owner.randomSeed);
		JCheckBox checkFillGrid = checkbox("Fill Grid", owner.gridFill, "Fill in all space in the document with markers");
		JCheckBox checkDrawGrid = checkbox("Draw Grid", owner.drawGrid, "Draw the grid in the document");
		JCheckBox checkDrawBorder = checkbox("Draw Border", owner.drawLineBorder, "Draw marker borders in the document");
		JCheckBox checkHideInfo = checkbox("Hide Info", owner.hideInfo, "Hide text info for each marker");
		JSpinner spinnerMaxDots = spinner(owner.maxDotsPerMarker, 1, 9999, 1);
		JSpinner spinnerTotalMarkers = spinner(totalMarkers, 1, 100000, 1);
		JSpinner spinnerDotDiameter = spinner(owner.dotDiameter, 0.0, 1000.0, 1.0);
		JSpinner spinnerSpace = spinner(spaceDiameter, -1.0, 1000.0, 1.0);
		JSpinner spinnerView = spinner(viewMarkerIndex, 0, 100000, 1);

		public ControlPanel() {
			owner.fileType = "pdf";

			fieldMarkerWidth.setPreferredSize(new Dimension(60, 24));
			fieldMarkerWidth.setMaximumSize(fieldMarkerWidth.getPreferredSize());
			fieldRandomSeed.setPreferredSize(new Dimension(100, 24));
			fieldRandomSeed.setMaximumSize(fieldRandomSeed.getPreferredSize());
			fieldMarkerWidth.addActionListener(this);
			fieldRandomSeed.addActionListener(this);

			addLabeled(spinnerView, "View Marker ID", "Specify which of the markers to view in the preview");
			addLabeled(spinnerTotalMarkers, "Total Markers", "The number of markers it will generate");
			addLabeled(fieldMarkerWidth, "Marker Width",
					"Width and Height of the markers. If image then this is pixels. Use command line for rectangular markers.");
			addLabeled(spinnerRatio, "Height/Width", "Ratio of the markers height/width");
			addLabeled(comboUnits, "Units", "Units that the width is specified in");
			addLabeled(spinnerMaxDots, "Max Dots", "Max number of possible dots in a marker");
			addLabeled(spinnerDotDiameter, "Dot Diameter", "How wide a marker is");
			addLabeled(spinnerSpace, "Dot Space", "If > 0 then this species the min spacing between dots. " +
					"If less than the diameter then two dots can touch. That's bad.");
			addLabeled(fieldRandomSeed, "Random Seed", "Random seed used to generate markers");

			addLabeled(comboOutputFormat, "Output Format", "Format for output file");
			addLabeled(comboPaper, "Paper Size", "Size of paper for PDF or printing");
			add(BoofSwingUtil.gridPanel(2, 2, 2, 2, checkFillGrid, checkDrawGrid, checkDrawBorder, checkHideInfo));

			// set the max size now so that it will have a width large enough for the max possible value
			((SpinnerNumberModel)spinnerView.getModel()).setMaximum(totalMarkers - 1);
		}

		/**
		 * Where all the user interactions are handled
		 */
		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerMaxDots) {
				owner.maxDotsPerMarker = ((Number)spinnerMaxDots.getValue()).intValue();
				updateMarkers();
			} else if (source == fieldMarkerWidth) {
				if (isPixels()) {
					widthPixels = ((Number)fieldMarkerWidth.getValue()).intValue();
					owner.markerWidth = widthPixels;
				} else {
					widthPdf = ((Number)fieldMarkerWidth.getValue()).doubleValue();
					owner.markerWidth = (float)widthPdf;
				}
				updateMarkers();
			} else if (source == spinnerRatio) {
				markerRatio = ((Number)spinnerRatio.getValue()).doubleValue();
				updateMarkers();
			} else if (source == spinnerDotDiameter) {
				if (isPixels()) {
					diameterPixels = ((Number)spinnerDotDiameter.getValue()).intValue();
					owner.dotDiameter = diameterPixels;
				} else {
					diameterPdf = ((Number)spinnerDotDiameter.getValue()).doubleValue();
					owner.dotDiameter = (float)diameterPdf;
				}
				updateMarkers();
			} else if (source == fieldRandomSeed) {
				owner.randomSeed = ((Number)fieldRandomSeed.getValue()).longValue();
				updateMarkers();
			} else if (source == spinnerSpace) {
				spaceDiameter = ((Number)spinnerSpace.getValue()).doubleValue();
				updateMarkers();
			} else if (source == spinnerTotalMarkers) {
				totalMarkers = ((Number)spinnerTotalMarkers.getValue()).intValue();
				// change the max allowed value in the marker being viewed
				SpinnerNumberModel m = (SpinnerNumberModel)spinnerView.getModel();
				m.setMaximum(totalMarkers - 1);
				updateMarkers();
			} else if (source == spinnerView) {
				int idx = ((Number)spinnerView.getValue()).intValue();
				if (viewMarkerIndex == idx)
					return;
				this.viewMarkerIndex = idx;
				updateRendering();
			} else if (source == comboOutputFormat) {
				owner.fileType = (String)comboOutputFormat.getSelectedItem();
				// toggle controls depending on type of output format
				boolean enable = comboOutputFormat.getSelectedIndex() == 0;
				comboPaper.setEnabled(enable);
				checkHideInfo.setEnabled(enable);
				checkFillGrid.setEnabled(enable);
				checkDrawBorder.setEnabled(enable);
				checkDrawGrid.setEnabled(enable);
				comboUnits.setEnabled(enable);
				if (enable) {
					owner.markerWidth = (float)widthPdf;
					owner.dotDiameter = (float)diameterPdf;
				} else {
					owner.markerWidth = widthPixels;
					owner.dotDiameter = diameterPixels;
				}
				fieldMarkerWidth.setValue(owner.markerWidth);
				spinnerDotDiameter.setValue(owner.dotDiameter);
				updateRendering();
			} else if (source == comboPaper) {
				owner.paperSize = PaperSize.values().get(comboPaper.getSelectedIndex());
			} else if (source == checkFillGrid) {
				owner.gridFill = checkFillGrid.isSelected();
			} else if (source == checkDrawGrid) {
				owner.drawGrid = checkDrawGrid.isSelected();
			} else if (source == checkDrawBorder) {
				owner.drawLineBorder = checkDrawBorder.isSelected();
			} else if (source == checkHideInfo) {
				owner.hideInfo = checkHideInfo.isSelected();
			}
		}
	}

	public static void main( String[] args ) {
		var owner = new CreateFiducialRandomDot();
		SwingUtilities.invokeLater(() -> new CreateFiducialRandomDotGui(owner));
	}
}
