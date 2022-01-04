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
import boofcv.abst.fiducial.MicroQrCodePreciseDetector;
import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.fiducial.calib.squares.SquareEdge;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.demonstrations.shapes.DetectBlackShapeAppBase;
import boofcv.demonstrations.shapes.ShapeGuiListener;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.fiducial.ConfigMicroQrCode;
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

import static boofcv.alg.fiducial.qrcode.QrCode.Failure.ALIGNMENT;

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectMicroQrApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase<T> implements ShapeGuiListener, DetectMicroQrMessagePanel.Listener {
	MicroQrCodePreciseDetector<T> detector;

	//--------- ONLY INVOKE IN THE GUI ------------
	DetectMicroQrControlPanel controlPanel;
	VisualizePanel gui = new VisualizePanel();

	// Lock against detected
	final DogArray<MicroQrCode> detected = new DogArray<>(MicroQrCode::new);
	final DogArray<MicroQrCode> failures = new DogArray<>(MicroQrCode::new);

	public DetectMicroQrApp( List<String> examples, Class<T> imageType ) {
		super(examples, imageType);

		controlPanel = new DetectMicroQrControlPanel(this);
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
							switch (DetectMicroQrApp.this.inputMethod) {
								case VIDEO:
								case WEBCAM:
									DetectMicroQrApp.this.streamPaused = !DetectMicroQrApp.this.streamPaused;
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
		detector.resetRuntimeProfiling();
	}

	@Override
	protected void createDetector( boolean initializing ) {
		if (!initializing)
			BoofSwingUtil.checkGuiThread();

		DetectMicroQrControlPanel controls = (DetectMicroQrControlPanel)DetectMicroQrApp.this.controls;

		synchronized (this) {
			ConfigMicroQrCode config = controls.getConfigQr();
			config.threshold = controls.getThreshold().createConfig();
			config.considerTransposed = controls.checkTransposed.value;

			detector = FactoryFiducial.microqr(config, imageClass);
			detector.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RUNTIME));
//			detector.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
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
//			detector.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
			long before = System.nanoTime();
			detector.process((T)input);
			long after = System.nanoTime();
			timeInSeconds = (after - before)*1e-9;
		}

		// create a local copy so that gui and processing thread's dont conflict
		synchronized (detected) {
			this.detected.reset();
			for (MicroQrCode d : detector.getDetections()) {
				this.detected.grow().setTo(d);
			}

			this.failures.reset();
			for (MicroQrCode d : detector.getFailures()) {
				if (d.failureCause.ordinal() >= QrCode.Failure.READING_BITS.ordinal())
					this.failures.grow().setTo(d);
			}
//			System.out.println("Failed "+failures.size());
//			for( QrCode qr : failures.toList() ) {
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
			MicroQrCode qr;
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

			var controls = (DetectMicroQrControlPanel)DetectMicroQrApp.this.controls;
			BoofSwingUtil.antialiasing(g2);

			synchronized (DetectMicroQrApp.this) {

				if (controls.bShowContour) {
					// todo copy after process() to avoid thread issues. or lock
					BinaryContourFinder contour = detector.getSquareDetector().getDetector().getContourFinder();
					List<Contour> contours = BinaryImageOps.convertContours(contour);
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null, Color.CYAN, 1.0, scale, g2);
				}

				synchronized (detected) {
					if (controls.bShowMarkers) {
						for (int i = 0; i < detected.size(); i++) {
							g2.setColor(new Color(0x5011FF00, true));
							MicroQrCode qr = detected.get(i);
							VisualizeShapes.fillPolygon(qr.bounds, scale, g2);

//							if (controls.bShowBits)
//								renderBinaryValues(g2, qr);
						}
					}

					if (controls.bShowFailures) {
						for (int i = 0; i < failures.size(); i++) {
							MicroQrCode qr = failures.get(i);
							if (qr.failureCause.ordinal() < ALIGNMENT.ordinal())
								continue;
							switch (qr.failureCause) {
								case ERROR_CORRECTION -> g2.setColor(new Color(0x80FF0000, true));
								case DECODING_MESSAGE -> g2.setColor(new Color(0x80FF9000, true));
								default -> g2.setColor(new Color(0x80FF00C0, true));
							}
							VisualizeShapes.fillPolygon(qr.bounds, scale, g2);

//							if (controls.bShowBits)
//								renderBinaryValues(g2, qr);
						}
					}
				}

//				if (controls.bShowBits) {
//					synchronized (detected) {
//						for (int i = 0; i < detected.size(); i++) {
//							MicroQrCode qr = detected.get(i);
//							renderBinaryValues(g2, qr);
//						}
//
//						for (int i = 0; i < failures.size(); i++) {
//							QrCode qr = failures.get(i);
//							if (qr.failureCause.ordinal() < ALIGNMENT.ordinal())
//								continue;
//							renderBinaryValues(g2, qr);
//						}
//					}
//				}

				if (controls.bShowSquares) {
					// todo copy after process() to avoid thread issues
					List<Polygon2D_F64> polygons = detector.getSquareDetector().getPolygons(null, null);

					g2.setColor(Color.GREEN);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowPositionPattern) {
					// todo copy after process() to avoid thread issues
					DogArray<PositionPatternNode> nodes = detector.getDetectPositionPatterns().getPositionPatterns();

					g2.setColor(Color.ORANGE);
					g2.setStroke(new BasicStroke(3));
					List<SquareEdge> list = new ArrayList<>();
					for (int i = 0; i < nodes.size(); i++) {
						SquareNode n = nodes.get(i);
						VisualizeShapes.drawPolygon(n.square, true, scale, g2);
						for (int j = 0; j < 4; j++) {
							if (n.edges[j] != null) {
								if (!list.contains(n.edges[j])) {
									list.add(n.edges[j]);
								}
							}
						}
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
		examples.add(UtilIO.pathExample("fiducial/microqr/image01.jpg"));
		examples.add(UtilIO.pathExample("fiducial/microqr/image02.jpg"));
		examples.add(UtilIO.pathExample("fiducial/microqr/image03.jpg"));
		examples.add(UtilIO.pathExample("fiducial/microqr/movie.mp4"));

		SwingUtilities.invokeLater(() -> {
			var app = new DetectMicroQrApp<>(examples, GrayF32.class);
			app.openExample(examples.get(0));
			app.display("Micro QR-Code Detector");
		});
	}
}
