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

package boofcv.demonstrations.fiducial;

import boofcv.abst.fiducial.calib.ConfigChessboardBits;
import boofcv.abst.fiducial.calib.ConfigChessboardBitsMarkers;
import boofcv.alg.fiducial.calib.chessbits.ChessboardBitPattern;
import boofcv.alg.fiducial.calib.chessbits.ChessboardReedSolomonDetector;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.calibration.DisplayPinholeCalibrationPanel;
import boofcv.gui.controls.JSpinnerNumber;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Shows detections and visualizes intermediate results for {@link ChessboardReedSolomonDetector}
 *
 * @author Peter Abeles
 */
public class DetectChessboardBitsApp extends DemonstrationBase {
	// TODO colorize anonymous differently
	// TODO mark decoded squares
	// TODO Show square ID numbers
	// TODO render marker ID
	// TODO show squares which failed to decode
	ConfigChessboardBits configDetector = new ConfigChessboardBits();
	ConfigChessboardBitsMarkers configMarker = ConfigChessboardBitsMarkers.singleShape(9, 7, 1, 1);

	// GUI Controls and visualization panels
	private final DisplayPanel imagePanel = new DisplayPanel();
	private final ControlPanel controlPanel;

	// Workspace for visualized image data
	private BufferedImage visualized;
	private BufferedImage original;

	//------------- Lock for when the detector is being used
	private final Object lockAlgorithm = new Object();
	ChessboardReedSolomonDetector<GrayF32> detector;
	//-------------

	//------------- Lock for what's being drawn in the GUI
	private final Object lockVisualize = new Object();
	DogArray<ChessboardBitPattern> foundPatterns = new DogArray<>(ChessboardBitPattern::new);
	//-------------


	public DetectChessboardBitsApp( List<PathLabel> examples ) {
		super(true, true, examples, ImageType.single(GrayF32.class));

		controlPanel = new ControlPanel();
		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, imagePanel);

		createAlgorithm();

		imagePanel.getImagePanel().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed( KeyEvent e ) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					streamPaused = !streamPaused;
				}
			}
		});
	}

	@Override
	protected void configureVideo( int which, SimpleImageSequence sequence ) {
		sequence.setLoop(true);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		visualized = ConvertBufferedImage.checkDeclare(width, height, visualized, BufferedImage.TYPE_INT_RGB);

		SwingUtilities.invokeLater(() -> {
			Dimension preferred = imagePanel.getPreferredSize();

			boolean sizeChange = preferred.width != width || preferred.height != height;
			if (sizeChange) {
				imagePanel.setPreferredSize(new Dimension(width, height));

				double scale = BoofSwingUtil.selectZoomToShowAll(imagePanel, width, height);
//			controlPanel.zoom = scale; // prevent it from broadcasting an event
				controlPanel.setZoom(scale);
				imagePanel.setScaleAndCenter(scale, width/2, height/2);
				controlPanel.setImageSize(width, height);
			}
		});
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {

		original = ConvertBufferedImage.checkCopy(buffered, original);

		GrayF32 gray = (GrayF32)input;

		double processingTime;
		synchronized (lockAlgorithm) {
			long time0 = System.nanoTime();
			detector.process(gray);
			long time1 = System.nanoTime();
			processingTime = (time1 - time0)*1e-6; // milliseconds

			FastAccess<ChessboardBitPattern> found = detector.getFound();

			synchronized (lockVisualize) {
				this.foundPatterns.resetResize(found.size);
				for (int i = 0; i < found.size; i++) {
					this.foundPatterns.get(i).setTo(found.get(i));

					// Sort so that they have some sane order when visualized
					Collections.sort(foundPatterns.get(i).corners.toList(), Comparator.comparingInt(a -> a.index));
				}
			}

			System.out.println("found.size="+found.size);
			for (int i = 0; i < found.size; i++) {
				ChessboardBitPattern c = found.get(i);
				if (!controlPanel.showAnonymous && c.marker < 0)
					continue;
				System.out.println("["+i+"]");
				System.out.println("  marker="+c.marker);
				System.out.println("  rows="+c.squareRows);
				System.out.println("  cols="+c.squareCols);
				System.out.println("  decoded="+c.decodedCells);

			}
		}

		SwingUtilities.invokeLater(() -> {
			controlPanel.setProcessingTimeMS(processingTime);
			imagePanel.setBufferedImageNoChange(original);
			imagePanel.repaint();
		});
	}

	private void createAlgorithm() {
		synchronized (lockAlgorithm) {
			configMarker.markerShapes.get(0).numRows = controlPanel.gridRows.value.intValue();
			configMarker.markerShapes.get(0).numCols = controlPanel.gridCols.value.intValue();
			configMarker.firstTargetDuplicated = controlPanel.numMarkers.value.intValue();
			configMarker.errorCorrectionLevel = controlPanel.errorLevel.value.intValue();

			detector = FactoryFiducial.chessboardBits(configDetector, configMarker, GrayF32.class);
		}
	}

	class DisplayPanel extends ShapeVisualizePanel {
		Ellipse2D.Double circle = new Ellipse2D.Double();
		BasicStroke stroke2 = new BasicStroke(2);
		BasicStroke stroke5 = new BasicStroke(5);

		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			super.paintInPanel(tran, g2);
			BoofSwingUtil.antialiasing(g2);

			if (controlPanel.translucent > 0) {
				// this requires some explaining
				// for some reason it was decided that the transform would apply a translation, but not a scale
				// so this scale will be concatted on top of the translation in the g2
				tran.setTransform(scale, 0, 0, scale, 0, 0);
				Composite beforeAC = g2.getComposite();
				float translucent = controlPanel.translucent/100.0f;
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, translucent);
				g2.setComposite(ac);
				g2.drawImage(visualized, tran, null);
				g2.setComposite(beforeAC);
			}

			synchronized (lockVisualize) {
				for (int i = 0; i < foundPatterns.size; i++) {
					ChessboardBitPattern c = foundPatterns.get(i);
					if (!controlPanel.showAnonymous && c.marker < 0)
						continue;
					DisplayPinholeCalibrationPanel.renderOrder(g2, scale, c.corners.toList());
				}
			}
			
		}

		@Override
		public synchronized void setScale( double scale ) {
			controlPanel.setZoom(scale);
			super.setScale(controlPanel.zoom);
		}
	}

	class ControlPanel extends DetectBlackShapePanel {
		JTextArea textArea = new JTextArea();

		JSpinnerNumber gridRows = spinnerWrap(configMarker.markerShapes.get(0).numRows, 3, 500, 1);
		JSpinnerNumber gridCols = spinnerWrap(configMarker.markerShapes.get(0).numCols, 4, 500, 1);
		JSpinnerNumber numMarkers = spinnerWrap(configMarker.firstTargetDuplicated, 1, 1000, 1);
		JSpinnerNumber errorLevel = spinnerWrap(configMarker.errorCorrectionLevel, 0, 10, 1);

		boolean showChessboards = true;
		boolean showNumbers = true;
		int translucent = 0;
		boolean showAnonymous = false;

		JSlider sliderTranslucent = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		JCheckBox checkShowTargets = checkbox("Chessboard", showChessboards);
		JCheckBox checkShowNumbers = checkbox("Numbers", showNumbers);
		JCheckBox checkShowAnonymous = checkbox("Anonymous", showAnonymous);

		ControlPanel() {
			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);

			selectZoom = spinner(1.0, MIN_ZOOM, MAX_ZOOM, 1.0);

			JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.addTab("Controls", controlPanel);
			tabbedPane.addTab("Info", textArea);

			addLabeled(processingTimeLabel, "Time (ms)");
			addLabeled(imageSizeLabel, "Image Size");
			addLabeled(sliderTranslucent, "X-Corners");
			addLabeled(selectZoom, "Zoom");
			add(createCheckPanel());
			add(createGridShapePanel());
			addLabeled(numMarkers.spinner, "Num. Markers");
			addLabeled(errorLevel.spinner, "Error Level");
			add(tabbedPane);
		}

		JPanel createCheckPanel() {
			JPanel panelChecks = new JPanel(new GridLayout(0, 2));

			panelChecks.add(checkShowTargets);
			panelChecks.add(checkShowNumbers);
			panelChecks.add(checkShowAnonymous);

			panelChecks.setPreferredSize(panelChecks.getMinimumSize());
			panelChecks.setMaximumSize(panelChecks.getMinimumSize());

			StandardAlgConfigPanel vertPanel = new StandardAlgConfigPanel();
			vertPanel.add(panelChecks);

			vertPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
					"Visualize", TitledBorder.CENTER, TitledBorder.TOP));

			return vertPanel;
		}

		JPanel createGridShapePanel() {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

			panel.add(new JLabel("Grid Shape"));
			panel.add(Box.createHorizontalGlue());
			panel.add(gridRows.spinner);
			panel.add(gridCols.spinner);

//			panel.setPreferredSize(panel.getMinimumSize());
//			panel.setMaximumSize(panel.getMinimumSize());

			return panel;
		}

		@Override public void controlChanged( Object source ) {
			if (source == checkShowTargets) {
				showChessboards = checkShowTargets.isSelected();
				imagePanel.repaint();
			} else if (source == checkShowNumbers) {
				showNumbers = checkShowNumbers.isSelected();
				imagePanel.repaint();
			} else if (source == checkShowAnonymous) {
				showAnonymous = checkShowAnonymous.isSelected();
				imagePanel.repaint();
			} else if (source == selectZoom) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				imagePanel.setScale(zoom);
			} else if (source == sliderTranslucent) {
				translucent = ((Number)sliderTranslucent.getValue()).intValue();
				imagePanel.repaint();
			} else {
				// Handle controls that modify the detector here
				if (source == gridRows.spinner) {
					gridRows.value = ((Number)gridRows.spinner.getValue()).intValue();
				} else if (source == gridCols.spinner) {
					gridCols.value = ((Number)gridCols.spinner.getValue()).intValue();
				} else if (source == numMarkers.spinner) {
					numMarkers.value = ((Number)numMarkers.spinner.getValue()).intValue();
				} else if (source == errorLevel.spinner) {
					errorLevel.value = ((Number)errorLevel.spinner.getValue()).intValue();
				}

				createAlgorithm();
				reprocessImageOnly();
			}
		}
	}

	public static void main( String[] args ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Chessboard 1", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame06.jpg")));
		examples.add(new PathLabel("Chessboard 2 ", UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left03.jpg")));
		examples.add(new PathLabel("Chessboard 3 ", UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left06.jpg")));

		SwingUtilities.invokeLater(() -> {
			var app = new DetectChessboardBitsApp(examples);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Chessboard Bits Detector");
		});
	}
}
