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

import boofcv.alg.fiducial.calib.squares.SquareEdge;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeDetector;
import boofcv.alg.filter.binary.Contour;
import boofcv.demonstrations.shapes.DetectBlackShapeAppBase;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
public class DetectQrCodeApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase
{
	QrCodeDetector<T> detector;

	public DetectQrCodeApp(List<String> examples , Class<T> imageType) {
		super(examples, imageType);

		final VisualizePanel gui = new VisualizePanel();
		setupGui(gui,new DetectQrCodeControlPanel(this));

		gui.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				double scale = gui.getScale();
				System.out.printf("click %5.1f %5.1f\n",e.getX()/scale,e.getY()/scale);
			}
		});
	}

	@Override
	protected void createDetector(boolean initializing) {
		if( !initializing)
			BoofSwingUtil.checkGuiThread();

		DetectQrCodeControlPanel controls = (DetectQrCodeControlPanel)DetectQrCodeApp.this.controls;

		synchronized (this) {
			ConfigQrCode config = controls.getConfigPolygon();

			if( controls.bRefineGray ) {
				config.polygon.refineGray = controls.refineGray;
			} else {
				config.polygon.refineGray = null;
			}

			detector = FactoryFiducial.qrcode(config,imageClass);
		}
		imageThresholdUpdated();
	}


	public void configUpdate() {
		createDetector(false);
		// does process and render too
	}

	@Override
	public void imageThresholdUpdated() {
		DetectQrCodeControlPanel controls = (DetectQrCodeControlPanel)DetectQrCodeApp.this.controls;

		ConfigThreshold config = controls.getThreshold().createConfig();

		synchronized (this) {
			inputToBinary = FactoryThresholdBinary.threshold(config, imageClass);
		}
		reprocessImageOnly();
	}

	int count = 0;
	@Override
	protected void detectorProcess(ImageGray input, GrayU8 binary) {
//		System.out.println("processing image "+count++);
		detector.process((T) input, binary);
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {

			DetectQrCodeControlPanel controls = (DetectQrCodeControlPanel)DetectQrCodeApp.this.controls;

			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized ( DetectQrCodeApp.this ) {

				if (controls.bShowContour) {
					List<Contour> contours = detector.getSquareDetector().getAllContours();
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null,Color.CYAN, scale, g2);
				}

				if ( true ) {
					FastQueue<QrCode> detected = detector.getDetections();

					g2.setColor(new Color(0x50FF0000,true));
					for (int i = 0; i < detected.size; i++) {
						QrCode qr = detected.get(i);
						VisualizeShapes.fillPolygon(qr.bounds,scale,g2);
					}

//					g2.setColor(Color.BLUE);
//					for (int i = 0; i < detected.size; i++) {
//						QrCode qr = detected.get(i);
//						Point2D_F64 c = qr.ppCorner.get(0);
//						VisualizeFeatures.drawCircle(g2, c.x * scale, c.y * scale, 5 * scale);
//						c = qr.ppRight.get(0);
//						VisualizeFeatures.drawCircle(g2, c.x * scale, c.y * scale, 5 * scale);
//						c = qr.ppDown.get(0);
//						VisualizeFeatures.drawCircle(g2, c.x * scale, c.y * scale, 5 * scale);
//					}
//					g2.setColor(Color.GREEN);
//					for (int i = 0; i < detected.size; i++) {
//						QrCode qr = detected.get(i);
//						Point2D_F64 c = qr.ppCorner.get(1);
//						VisualizeFeatures.drawCircle(g2, c.x * scale, c.y * scale, 5 * scale);
//						c = qr.ppRight.get(1);
//						VisualizeFeatures.drawCircle(g2, c.x * scale, c.y * scale, 5 * scale);
//						c = qr.ppDown.get(1);
//						VisualizeFeatures.drawCircle(g2, c.x * scale, c.y * scale, 5 * scale);
//					}
				}

				if (controls.bShowSquares) {
					List<Polygon2D_F64> polygons = detector.getSquareDetector().getPolygons(null);

					g2.setColor(Color.GREEN);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowAlignmentPattern) {
					List<QrCode> codes = detector.getDetections().toList();

					g2.setColor(Color.BLUE);
					g2.setStroke(new BasicStroke(3));
					for (QrCode qr : codes) {
						double size = Math.sqrt(qr.ppCorner.areaSimple())/14.0;
						for (int i = 0; i < qr.alignment.size; i++) {
							QrCode.Alignment a = qr.alignment.get(i);
							VisualizeFeatures.drawCircle(g2, a.pixel.x*scale, a.pixel.y*scale,size*scale);
//							if( scale > 4 )
//								VisualizeFeatures.drawCircle(g2, a.pixel.x*scale, a.pixel.y*scale,0.2*scale);
						}
					}
				}

				if (controls.bShowPositionPattern ) {
					FastQueue<PositionPatternNode> nodes = detector.getDetectPositionPatterns().getPositionPatterns();

					g2.setColor(Color.ORANGE);
					g2.setStroke(new BasicStroke(3));
					List<SquareEdge> list = new ArrayList<>();
					for (int i = 0; i < nodes.size(); i++) {
						SquareNode n = nodes.get(i);
						VisualizeShapes.drawPolygon(n.square, true, scale, g2);
						for (int j = 0; j < 4; j++) {
							if( n.edges[j] != null ) {
								if( !list.contains(n.edges[j])) {
									list.add(n.edges[j]);
								}
							}
						}
					}

					g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor( new Color(255, 150, 100));
					g2.setStroke(new BasicStroke(3));
					Line2D.Double l = new Line2D.Double();
					for (int i = 0; i < list.size(); i++) {
						SquareEdge e = list.get(i);
						Point2D_F64 a = e.a.center;
						Point2D_F64 b = e.b.center;

						l.setLine(scale*a.x,scale*a.y,scale*b.x,scale*b.y);
						g2.draw(l);
					}
					
				}
			}
		}
	}

	public static void main(String[] args) {

		List<String> examples = new ArrayList<>();
		examples.add("shapes/polygons01.jpg");
		examples.add("shapes/polygons_border_01.jpg");
//		examples.add("shapes/shapes01.png"); This is a pathological case specifically design to screw up threshold algs
		examples.add("shapes/shapes02.png");
		examples.add("shapes/concave01.jpg");
		examples.add("shapes/line_text_test_image.png");
		examples.add("fiducial/binary/image0000.jpg");
		examples.add("calibration/stereo/Bumblebee2_Square/left10.jpg");
		examples.add("fiducial/square_grid/movie.mp4");

		DetectQrCodeApp app = new DetectQrCodeApp(examples,GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.display("QR-Code Detector");
	}



}
