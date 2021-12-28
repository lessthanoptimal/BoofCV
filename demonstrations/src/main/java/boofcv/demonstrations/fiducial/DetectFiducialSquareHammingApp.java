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

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.square.DetectFiducialSquareHamming;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.demonstrations.shapes.*;
import boofcv.factory.fiducial.ConfigFiducialHammingDetector;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.controls.JCheckBoxValue;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Application which lets you detect {@link DetectFiducialSquareHamming}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectFiducialSquareHammingApp extends DemonstrationBase implements ShapeGuiListener {
	// TODO Visualize the squares it collects when processing an image

	Detector detector;
	VisualizePanel guiImage;

	Controls controls;

	BufferedImage original;
	BufferedImage work;

	final Object lockProcessing = new Object();

	public DetectFiducialSquareHammingApp( List<String> examples ) {
		super(examples, ImageType.single(GrayF32.class));
		setupGui();
	}

	protected void setupGui() {
		this.guiImage = new VisualizePanel();
		this.controls = new Controls();

		guiImage.autoScaleCenterOnSetImage = false; // keep scale when switching views

		guiImage.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				double scale = guiImage.getScale();
				System.out.printf("click %5.1f %5.1f\n", e.getX()/scale, e.getY()/scale);
			}
		});

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, guiImage);

		createDetector(true);

		guiImage.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (inputMethod == InputMethod.VIDEO) {
						streamPaused = !streamPaused;
					}
				}
			}
		});
	}

	protected void createDetector( boolean initializing ) {
		if (!initializing)
			BoofSwingUtil.checkGuiThread();

		synchronized (lockProcessing) {
			ConfigHammingMarker configMarker = controls.configMarker;
			ConfigFiducialHammingDetector configFid = controls.getDetectorConfig();

			final InputToBinary<GrayF32> binary = FactoryThresholdBinary.threshold(configFid.configThreshold, GrayF32.class);
			final DetectPolygonBinaryGrayRefine<GrayF32> squareDetector = FactoryShapeDetector.
					polygon(configFid.squareDetector, GrayF32.class);

			detector = new Detector(configMarker, configFid.minimumBlackBorderFraction, binary, squareDetector);
		}
	}

	@Override
	public void configUpdate() {
		createDetector(false);
		reprocessImageOnly();
	}

	@Override
	public void imageThresholdUpdated() {
		createDetector(false);
		reprocessImageOnly();
	}

	@Override protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);
		guiImage.setPreferredSize(new Dimension(width, height));
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {
		original = ConvertBufferedImage.checkCopy(buffered, original);
		work = ConvertBufferedImage.checkDeclare(buffered, work);

		final double timeInSeconds;
		synchronized (lockProcessing) {
			long before = System.nanoTime();
			detector.process((GrayF32)input);
			long after = System.nanoTime();
			timeInSeconds = (after - before)*1e-9;
		}

		SwingUtilities.invokeLater(() -> {
			controls.setProcessingTimeS(timeInSeconds);
			viewUpdated();
		});
	}

	/**
	 * Called when how the data is visualized has changed
	 */
	@Override
	public void viewUpdated() {
		BufferedImage active = null;
		if (controls.selectedView == 0) {
			active = original;
		} else if (controls.selectedView == 1) {
			synchronized (lockProcessing) {
				VisualizeBinaryData.renderBinary(detector.getBinary(), false, work);
			}
			active = work;
			work.setRGB(0, 0, work.getRGB(0, 0)); // hack so that Swing knows it's been modified
		} else {
			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, work.getWidth(), work.getHeight());
			active = work;
		}

		guiImage.setImage(active);
		guiImage.repaint();
	}

	class VisualizePanel extends ShapeVisualizePanel {
		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			BoofSwingUtil.antialiasing(g2);

			synchronized (lockProcessing) {
				if (controls.showContour.value) {
					BinaryContourFinder contour = detector.getSquareDetector().getDetector().getContourFinder();
					List<Contour> contours = BinaryImageOps.convertContours(contour);
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null, Color.CYAN, 1.0, scale, g2);
				}

				DogArray<FoundFiducial> detected = detector.getFound();

//				g2.setColor(new Color(0x50FF0000,true));
//				for (int i = 0; i < detected.size; i++) {
//					FoundFiducial fid = detected.get(i);
//					VisualizeShapes.drawQuad(fid.distortedPixels,g2,scale,true,Color.RED,Color.BLACK);
//				}
				if (controls.showSquares.value) {
					List<Polygon2D_F64> polygons = detector.getSquareDetector().getPolygons(null, null);

					g2.setColor(Color.GREEN);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.showOrientation.value) {
					g2.setStroke(new BasicStroke(2));
					for (int i = 0; i < detected.size; i++) {
						VisualizeShapes.drawArrowSubPixel(detected.get(i).distortedPixels, 3, scale, g2);
					}
				}

				if (controls.showLabels.value) {
					Point2D_F64 center = new Point2D_F64();
					g2.setStroke(new BasicStroke(2));
					for (int i = 0; i < detected.size; i++) {
						FoundFiducial f = detected.get(i);
						UtilPolygons2D_F64.center(f.distortedPixels, center);
						center.x *= scale;
						center.y *= scale;
						VisualizeFiducial.drawLabel(center, "" + f.id, g2);
					}
				}
			}
		}

		@Override
		public synchronized void setScale( double scale ) {
			controls.setZoom(scale);
			super.setScale(controls.zoom);
		}
	}

	public static class Detector extends DetectFiducialSquareHamming<GrayF32> {
		public DogArray<GrayU8> squares = new DogArray<>(() -> new GrayU8(1, 1));
		public DogArray<GrayF32> squaresGray = new DogArray<>(() -> new GrayF32(1, 1));

		protected Detector( ConfigHammingMarker config, double minimumBlackBorderFraction,
							InputToBinary<GrayF32> inputToBinary,
							DetectPolygonBinaryGrayRefine<GrayF32> quadDetector ) {
			super(config, minimumBlackBorderFraction, inputToBinary, quadDetector, GrayF32.class);
		}

		@Override
		protected boolean processSquare( GrayF32 square, Result result, double a, double b ) {
			squares.reset();
			squaresGray.reset();

			if (super.processSquare(square, result, a, b)) {
				squares.grow().setTo(getBinaryInner());
				squaresGray.grow().setTo(getGrayNoBorder());
				return true;
			}

			return false;
		}
	}

	public class Controls extends DetectBlackShapePanel {
		ConfigHammingMarker configMarker = ConfigHammingMarker.loadDictionary(HammingDictionary.ARUCO_MIP_25h7);
		ConfigFiducialHammingDetector configDetector = new ConfigFiducialHammingDetector();

		// selects which image to view
		JComboBox<String> imageView;

		JCheckBoxValue showSquares = checkboxWrap("Squares", false);
		JCheckBoxValue showOrientation = checkboxWrap("Orientation", true);
		JCheckBoxValue showContour = checkboxWrap("Contour", false);
		JCheckBoxValue showLabels = checkboxWrap("Labels", false);
		JCheckBoxValue printVerbose = checkboxWrap("Verbose", false).tt("Prints verbose messages to stdout");
		JComboBox<String> comboDictionary = combo(configMarker.dictionary.ordinal() - 1,
				(Object[])HammingDictionary.allPredefined());

		DetectBlackPolygonControlPanel polygonPanel;

		public Controls() {
			polygonPanel = new DetectBlackPolygonControlPanel(
					DetectFiducialSquareHammingApp.this, configDetector.squareDetector, configDetector.configThreshold);
			polygonPanel.removeControlNumberOfSides();

			imageView = combo(0, "Input", "Binary", "Black");
			selectZoom = spinner(1, MIN_ZOOM, MAX_ZOOM, 0.1);

			addLabeled(processingTimeLabel, "Time (ms)");
			addLabeled(imageSizeLabel, "Size");
			addLabeled(imageView, "View");
			addLabeled(selectZoom, "Zoom");
			addAlignLeft(showOrientation.check);
			addAlignLeft(showLabels.check);
			addAlignLeft(showSquares.check);
			addAlignLeft(showContour.check);
			addAlignLeft(printVerbose.check);
			addLabeled(comboDictionary, "Dictionary");
			add(polygonPanel);
			addVerticalGlue(this);
		}

		@Override public void controlChanged( final Object source ) {
			if (source == imageView) {
				selectedView = imageView.getSelectedIndex();
				viewUpdated();
			} else if (source == showSquares.check) {
				showSquares.updateValue();
				guiImage.repaint();
			} else if (source == showOrientation.check) {
				showOrientation.updateValue();
				guiImage.repaint();
			} else if (source == showContour.check) {
				showContour.updateValue();
				guiImage.repaint();
			} else if (source == showLabels.check) {
				showLabels.updateValue();
				guiImage.repaint();
			} else if (source == printVerbose.check) {
				printVerbose.updateValue();
				detector.setVerbose(printVerbose.value ? System.out : null, null);
			} else if (source == selectZoom) {
				double value = ((Number)selectZoom.getValue()).doubleValue();
				if (value != zoom) {
					zoom = value;
					guiImage.setScale(zoom);
				}
			} else if (source == comboDictionary) {
				int which = comboDictionary.getSelectedIndex() + 1;
				configMarker.setTo(ConfigHammingMarker.loadDictionary(HammingDictionary.values()[which]));
				configUpdate();
			} else {
				configUpdate();
			}
		}

		public ThresholdControlPanel getThreshold() {
			return polygonPanel.thresholdPanel;
		}

		public ConfigFiducialHammingDetector getDetectorConfig() {
			configDetector.squareDetector = polygonPanel.getConfigPolygon();
			configDetector.configThreshold = polygonPanel.getThresholdPanel().createConfig();

			return configDetector;
		}
	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("fiducial/square_hamming/aruco_25h7/image01.jpg"));
		examples.add(UtilIO.pathExample("fiducial/square_hamming/aruco_25h7/image02.jpg"));
		examples.add(UtilIO.pathExample("fiducial/square_hamming/aruco_25h7/image03.jpg"));
		examples.add(UtilIO.pathExample("fiducial/square_hamming/aruco_25h7/movie.mp4"));

		SwingUtilities.invokeLater(() -> {
			DetectFiducialSquareHammingApp app = new DetectFiducialSquareHammingApp(examples);
			app.openFile(new File(examples.get(0)));
			app.display("Fiducial Square Hamming Detector");
		});
	}
}
