/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.QrCodePreciseDetector;
import boofcv.alg.fiducial.calib.squares.SquareEdge;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.demonstrations.shapes.DetectBlackShapeAppBase;
import boofcv.demonstrations.shapes.ShapeGuiListener;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
public class DetectQrCodeApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase implements ShapeGuiListener
{
	QrCodePreciseDetector<T> detector;

	public DetectQrCodeApp(List<String> examples , Class<T> imageType) {
		super(examples, imageType);

		final VisualizePanel gui = new VisualizePanel();
		DetectQrCodeControlPanel controlPanel = new DetectQrCodeControlPanel(this);
		controlPanel.polygonPanel.removeControlNumberOfSides();
		setupGui(gui,controlPanel);

		gui.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				double scale = gui.getScale();
				System.out.printf("click %5.1f %5.1f\n",e.getX()/scale,e.getY()/scale);
			}
		});

		setPreferredSize(new Dimension(800,800));
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		super.handleInputChange(source, method, width, height);
		detector.resetRuntimeProfiling();
	}

	@Override
	protected void createDetector(boolean initializing) {
		if( !initializing)
			BoofSwingUtil.checkGuiThread();

		DetectQrCodeControlPanel controls = (DetectQrCodeControlPanel)DetectQrCodeApp.this.controls;

		synchronized (this) {
			ConfigQrCode config = controls.getConfigQr();
			config.threshold = controls.getThreshold().createConfig();

			detector = FactoryFiducial.qrcode(config,imageClass);
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
	public void processImage(int sourceID, long frameID, final BufferedImage buffered, ImageBase input) {
		System.out.flush();

		original = ConvertBufferedImage.checkCopy(buffered,original);
		work = ConvertBufferedImage.checkDeclare(buffered,work);

		final double timeInSeconds;
		synchronized (this) {
			long before = System.nanoTime();
			detector.process((T)input);
			long after = System.nanoTime();
			timeInSeconds = (after-before)*1e-9;
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				controls.setProcessingTime(timeInSeconds);
				viewUpdated();
			}
		});
	}

	int count = 0;
	@Override
	protected void detectorProcess(ImageGray input, GrayU8 binary) {
		throw new RuntimeException("This shouldn't be called");
	}

	class VisualizePanel extends ShapeVisualizePanel {

		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {

			DetectQrCodeControlPanel controls = (DetectQrCodeControlPanel) DetectQrCodeApp.this.controls;

			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized (DetectQrCodeApp.this) {

				if (controls.bShowContour) {

					LinearContourLabelChang2004 contour = detector.getSquareDetector().getDetector().getContourFinder();
					List<Contour> contours =
							BinaryImageOps.convertContours(contour.getPackedPoints(), contour.getContours());
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null, Color.CYAN, scale, g2);
				}

				List<QrCode> detected = detector.getDetections();

				g2.setColor(new Color(0x50FF0000, true));
				for (int i = 0; i < detected.size(); i++) {
					QrCode qr = detected.get(i);
					VisualizeShapes.fillPolygon(qr.bounds, scale, g2);
				}

				if (controls.bShowSquares) {
					List<Polygon2D_F64> polygons = detector.getSquareDetector().getPolygons(null, null);

					g2.setColor(Color.GREEN);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowAlignmentPattern) {
					List<QrCode> codes = detector.getDetections();

					g2.setColor(Color.BLUE);
					g2.setStroke(new BasicStroke(3));
					for (QrCode qr : codes) {
						double size = Math.sqrt(qr.ppCorner.areaSimple()) / 14.0;
						for (int i = 0; i < qr.alignment.size; i++) {
							QrCode.Alignment a = qr.alignment.get(i);
							VisualizeFeatures.drawCircle(g2, a.pixel.x * scale, a.pixel.y * scale, size * scale);
						}
					}
				}

				if (controls.bShowPositionPattern) {
					FastQueue<PositionPatternNode> nodes = detector.getDetectPositionPatterns().getPositionPatterns();

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

						l.setLine(scale * a.x, scale * a.y, scale * b.x, scale * b.y);
						g2.draw(l);
					}
				}
			}
		}
	}

	@Override
	public void viewUpdated() {
		BufferedImage active = null;
		if( controls.selectedView == 0 ) {
			active = original;
		} else if( controls.selectedView == 1 ) {
			VisualizeBinaryData.renderBinary(detector.getBinary(),false,work);
			active = work;
			work.setRGB(0, 0, work.getRGB(0, 0)); // hack so that Swing knows it's been modified
		} else {
			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0,0,work.getWidth(),work.getHeight());
			active = work;
		}

		guiImage.setBufferedImage(active);
		guiImage.setScale(controls.zoom);

		guiImage.repaint();
	}

	public static void main(String[] args) {

		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("fiducial/qrcode/image01.jpg"));
		examples.add(UtilIO.pathExample("fiducial/qrcode/image02.jpg"));
		examples.add(UtilIO.pathExample("fiducial/qrcode/image03.jpg"));

		DetectQrCodeApp app = new DetectQrCodeApp(examples,GrayF32.class);

		app.openFile(new File(examples.get(0)));
		app.display("QR-Code Detector");
	}



}
