/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofVerbose;
import boofcv.abst.fiducial.AztecCodePreciseDetector;
import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.alg.fiducial.aztec.AztecPyramid;
import boofcv.alg.fiducial.calib.squares.SquareEdge;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.demonstrations.shapes.DetectBlackShapeAppBase;
import boofcv.demonstrations.shapes.ShapeGuiListener;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.fiducial.ConfigAztecCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects and displays Aztec Code markers
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectAztecCodeApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase<T> implements ShapeGuiListener, DetectAztecCodeMessagePanel.Listener {
	AztecCodePreciseDetector<T> detector;

	//--------- ONLY INVOKE IN THE GUI ------------
	DetectAztecCodeControlPanel controlPanel;
	VisualizePanel gui = new VisualizePanel();

	// Lock against detected
	final DogArray<AztecCode> detected = new DogArray<>(AztecCode::new);
	final DogArray<AztecCode> failures = new DogArray<>(AztecCode::new);

	public DetectAztecCodeApp( List<String> examples, Class<T> imageType ) {
		super(examples, imageType);

		controlPanel = new DetectAztecCodeControlPanel(this);
		controlPanel.polygonPanel.removeControlNumberOfSides();
		setupGui(gui, controlPanel);

		gui.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				gui.getImagePanel().grabFocus();
				double scale = gui.getScale();
				Point2D_F64 p = new Point2D_F64(e.getX()/scale, e.getY()/scale);
				System.out.printf("click %5.1f %5.1f\n", p.x, p.y);
				synchronized (detected) {
					for (int i = 0; i < detected.size; i++) {
						if (Intersection2D_F64.containsConvex(detected.get(i).bounds, p)) {
							selectedMarkerMouse(i, false);
							return;
						}
					}
					for (int i = 0; i < failures.size; i++) {
						if (Intersection2D_F64.containsConvex(failures.get(i).bounds, p)) {
							selectedMarkerMouse(i, true);
							return;
						}
					}
				}
			}
		});

		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.addKeyEventDispatcher(new KeyEventDispatcher() {
					boolean spaceDown = false;

					@Override
					public boolean dispatchKeyEvent( KeyEvent e ) {
						if (e.getKeyCode() != KeyEvent.VK_SPACE)
							return false;

						if (spaceDown) {
							if (e.getID() != KeyEvent.KEY_RELEASED)
								return false;
						} else {
							if (e.getID() != KeyEvent.KEY_PRESSED) {
								return false;
							}
						}
						spaceDown = !spaceDown;

						System.out.println("Space down = " + spaceDown);

						if (spaceDown) {
							switch (DetectAztecCodeApp.this.inputMethod) {
								case VIDEO:
								case WEBCAM:
									DetectAztecCodeApp.this.streamPaused = !DetectAztecCodeApp.this.streamPaused;
									break;
								default:
									break;
							}
						}
						return false;
					}
				});

		setPreferredSize(new Dimension(1200, 800));
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);
//		detector.resetRuntimeProfiling();
	}

	@Override
	protected void createDetector( boolean initializing ) {
		if (!initializing)
			BoofSwingUtil.checkGuiThread();

		DetectAztecCodeControlPanel controls = (DetectAztecCodeControlPanel)DetectAztecCodeApp.this.controls;

		synchronized (this) {
			ConfigAztecCode config = controls.getConfigQr();
			config.threshold = controls.getThreshold().createConfig();
			config.considerTransposed = controls.checkTransposed.value;

			detector = FactoryFiducial.aztec(config, imageClass);
			detector.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
//			detector.setProfilerState(true);
		}
	}

	@Override
	public void configUpdate() {
		createDetector(false);
		reprocessImageOnly();
		// does process and render too
	}

	@Override
	public void imageThresholdUpdated() {
		configUpdate();
	}

	/**
	 * Override this function so that it doesn't threshold the image twice
	 */
	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {
		System.out.flush();

		synchronized (bufferedImageLock) {
			original = ConvertBufferedImage.checkCopy(buffered, original);
			work = ConvertBufferedImage.checkDeclare(buffered, work);
		}

		if (saveRequested) {
			saveInputImage();
			saveRequested = false;
		}

		final double timeInSeconds;

		// TODO Copy all data that's visualized outside so that GUI doesn't lock
		synchronized (this) {
			long before = System.nanoTime();
			detector.process((T)input);
			long after = System.nanoTime();
			timeInSeconds = (after - before)*1e-9;
		}

		// create a local copy so that gui and processing thread's dont conflict
		synchronized (detected) {
			this.detected.reset();
			for (AztecCode d : detector.getDetections()) {
				this.detected.grow().setTo(d);
			}
			this.failures.reset();
			for (AztecCode d : detector.getFailures()) {
				this.failures.grow().setTo(d);
			}
//			System.out.println("Failed "+failures.size());
//			for( AztecCode qr : failures.toList() ) {
//				System.out.println("  cause "+qr.failureCause);
//			}
		}
		controlPanel.polygonPanel.thresholdPanel.updateHistogram((T)input);


		SwingUtilities.invokeLater(() -> {
			controls.setProcessingTimeS(timeInSeconds);
			viewUpdated();
			synchronized (detected) {
				controlPanel.messagePanel.updateList(detected.toList(), failures.toList());
			}
		});
	}

	@Override
	protected void detectorProcess( ImageGray input, GrayU8 binary ) {
		throw new RuntimeException("This shouldn't be called");
	}

	public void selectedMarkerMouse( int index, boolean failure ) {
		controlPanel.messagePanel.setSelectedMarker(index, failure);
	}

	@Override
	public void selectedMarkerInList( int index, boolean failure ) {
		double width = 0;
		final Point2D_F64 center = new Point2D_F64();

		synchronized (detected) {
			AztecCode qr;
			if (failure) {
				if (index >= failures.size)
					return;
				qr = failures.get(index);
			} else {
				if (index >= detected.size)
					return;
				qr = detected.get(index);
			}
			UtilPolygons2D_F64.vertexAverage(qr.bounds, center);


			for (int i = 0; i < 4; i++) {
				width = Math.max(width, qr.bounds.getSideLength(i));
			}
		}

		final double _width = width;

		BoofSwingUtil.invokeNowOrLater(() -> {
			double scale = 0.75*Math.min(gui.getWidth(), gui.getHeight())/_width;
			gui.setScaleAndCenter(scale, center.x, center.y);
			controlPanel.setZoom(scale);
		});
	}

	class VisualizePanel extends ShapeVisualizePanel {

		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {

			DetectAztecCodeControlPanel controls = (DetectAztecCodeControlPanel)DetectAztecCodeApp.this.controls;
			BoofSwingUtil.antialiasing(g2);

			synchronized (DetectAztecCodeApp.this) {

				if (controls.bShowContour) {
					// todo copy after process() to avoid thread issues. or lock
					BinaryContourFinder contour = detector.getDetectorPyramids().getSquareDetector().getDetector().getContourFinder();
					List<Contour> contours = BinaryImageOps.convertContours(contour);
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null, Color.CYAN, 1.0, scale, g2);
				}

				synchronized (detected) {
					if (controls.bShowMarkers) {
						for (int i = 0; i < detected.size(); i++) {
							g2.setColor(new Color(0x5011FF00, true));
							AztecCode qr = detected.get(i);
							VisualizeShapes.fillPolygon(qr.bounds, scale, g2);

							if (controls.bShowBits)
								renderBinaryValues(g2, qr);
						}
					}

					if (controls.bShowFailures) {
						for (int i = 0; i < failures.size(); i++) {
							AztecCode qr = failures.get(i);
							switch (qr.failure) {
								case MODE_ECC -> g2.setColor(new Color(0x80FF0000, true));
								case MESSAGE_ECC -> g2.setColor(new Color(0x80FF9000, true));
								default -> g2.setColor(new Color(0x80FF00C0, true));
							}
							VisualizeShapes.fillPolygon(qr.bounds, scale, g2);

							if (controls.bShowBits)
								renderBinaryValues(g2, qr);
						}
					}
				}

				if (controls.bShowBits) {
//					synchronized (detected) {
//						for (int i = 0; i < detected.size(); i++) {
//							AztecCode qr = detected.get(i);
//							renderBinaryValues(g2, qr);
//						}
//
//						for (int i = 0; i < failures.size(); i++) {
//							AztecCode qr = failures.get(i);
//							if (qr.failureCause.ordinal() < ALIGNMENT.ordinal())
//								continue;
//							renderBinaryValues(g2, qr);
//						}
//					}
				}

				if (controls.bShowSquares) {
					// todo copy after process() to avoid thread issues
					List<Polygon2D_F64> polygons = detector.getDetectorPyramids().getSquareDetector().getPolygons(null, null);

					g2.setColor(Color.GREEN);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowPyramids) {
					// todo copy after process() to avoid thread issues
					DogArray<AztecPyramid> nodes = detector.getDetectorPyramids().getFound();

					g2.setColor(Color.ORANGE);
					g2.setStroke(new BasicStroke(3));
					List<SquareEdge> list = new ArrayList<>();
					for (int i = 0; i < nodes.size(); i++) {
						Polygon2D_F64 square = nodes.get(i).get(0).square;
						VisualizeShapes.drawPolygon(square, true, scale, g2);
					}

					g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(new Color(255, 150, 100));
					g2.setStroke(new BasicStroke(3));
					Line2D.Double l = new Line2D.Double();
					for (int i = 0; i < list.size(); i++) {
						SquareEdge e = list.get(i);
						Point2D_F64 a = e.a.center;
						Point2D_F64 b = e.b.center;

						l.setLine(scale*a.x, scale*a.y, scale*b.x, scale*b.y);
						g2.draw(l);
					}
				}
			}
		}

		/**
		 * Draws the estimated value of each bit onto the image
		 */
		private void renderBinaryValues( Graphics2D g2, AztecCode qr ) {
//			locator.setHomographyInv(qr.Hinv);
//
//			List<Point2D_I32> points = AztecCode.LOCATION_BITS[qr.version];
//
//			PackedBits8 bits = new PackedBits8();
//			bits.data = qr.rawbits;
//			bits.size = qr.rawbits.length*8;
//
//			Point2D_F32 p = new Point2D_F32();
//			g2.setStroke(new BasicStroke(1));
//			for (int i = 0; i < bits.size; i++) {
//				Point2D_I32 c = points.get(i);
//				locator.gridToImage(c.y + 0.5f, c.x + 0.5f, p);
//				int value = qr.mask.apply(c.y, c.x, bits.get(i));
//
//				if (value == 1) {
//					renderCircleAt(g2, p, Color.BLACK, Color.LIGHT_GRAY);
//				} else if (value == 0) {
//					renderCircleAt(g2, p, Color.WHITE, Color.LIGHT_GRAY);
//				} else {
//					renderCircleAt(g2, p, Color.RED, Color.LIGHT_GRAY);
//				}
//			}

//			int N = qr.totalModules();
//			g2.setColor(Color.BLUE);
//			locator.gridToImage(0,N-7,p);
//			renderCircleAt(g2, p);
//			locator.gridToImage(7,N-7,p);
//			renderCircleAt(g2, p);
//			locator.gridToImage(N-7,0,p);
//			renderCircleAt(g2, p);
//			locator.gridToImage(N-7,7,p);
//			renderCircleAt(g2, p);
		}

		private void renderCircleAt( Graphics2D g2, Point2D_F32 p, Color center, Color border ) {
			int x = (int)(scale*p.x + 0.5);
			int y = (int)(scale*p.y + 0.5);
			g2.setColor(center);
			g2.fillOval(x - 3, y - 3, 7, 7);
			g2.setColor(border);
			g2.drawOval(x - 3, y - 3, 7, 7);
		}
	}

	@Override
	public void viewUpdated() {
		BufferedImage active;
		if (controls.selectedView == 0) {
			active = original;
		} else if (controls.selectedView == 1) {
			VisualizeBinaryData.renderBinary(detector.getBinary(), false, work);
			active = work;
			work.setRGB(0, 0, work.getRGB(0, 0)); // hack so that Swing knows it's been modified
		} else {
			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, work.getWidth(), work.getHeight());
			active = work;
		}

		guiImage.setImage(active);
		guiImage.setScale(controls.zoom);

		guiImage.repaint();
	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("fiducial/aztec/image01.jpg"));
		examples.add(UtilIO.pathExample("fiducial/aztec/image02.jpg"));
		examples.add(UtilIO.pathExample("fiducial/aztec/image03.jpg"));
		examples.add(UtilIO.pathExample("fiducial/aztec/image04.jpg"));
		examples.add(UtilIO.pathExample("fiducial/aztec/movie.mp4"));

		SwingUtilities.invokeLater(() -> {
			var app = new DetectAztecCodeApp<>(examples, GrayF32.class);
			app.openExample(examples.get(0));
			app.display("Aztec Code Detector");
		});
	}
}
