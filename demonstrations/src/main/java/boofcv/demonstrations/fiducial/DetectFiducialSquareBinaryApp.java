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
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.demonstrations.shapes.ShapeGuiListener;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
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

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectFiducialSquareBinaryApp extends DemonstrationBase implements ShapeGuiListener {
	Detector detector;
	VisualizePanel guiImage;

	DetectFiducialSquareBinaryPanel controls;

	BufferedImage original;
	BufferedImage work;

	final Object lockProcessing = new Object();

	public DetectFiducialSquareBinaryApp( List<String> examples ) {
		super(examples, ImageType.single(GrayF32.class));
		setupGui();
		setPreferredSize(new Dimension(800, 800));
	}

	protected void setupGui() {
		this.guiImage = new VisualizePanel();
		this.controls = new DetectFiducialSquareBinaryPanel(this);

		guiImage.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				double scale = guiImage.getScale();
				System.out.printf("click %5.1f %5.1f\n", e.getX()/scale, e.getY()/scale);
			}
		});

		guiImage.setPreferredSize(new Dimension(800, 800));

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

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);
	}

	protected void createDetector( boolean initializing ) {
		if (!initializing)
			BoofSwingUtil.checkGuiThread();

		synchronized (lockProcessing) {
			ConfigThreshold configThresh = controls.polygonPanel.getThresholdPanel().createConfig();
			ConfigFiducialBinary configFid = controls.getConfig();

			final InputToBinary<GrayF32> binary = FactoryThresholdBinary.threshold(configThresh, GrayF32.class);
			final DetectPolygonBinaryGrayRefine<GrayF32> squareDetector = FactoryShapeDetector.
					polygon(configFid.squareDetector, GrayF32.class);

			detector = new Detector(configFid, binary, squareDetector);
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

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {
		System.out.flush();

		original = ConvertBufferedImage.checkCopy(buffered, original);
		work = ConvertBufferedImage.checkDeclare(buffered, work);

		final double timeInSeconds;
		synchronized (lockProcessing) {
			long before = System.nanoTime();
			detector.process((GrayF32)input);
			long after = System.nanoTime();
			timeInSeconds = (after - before)*1e-9;
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				controls.setProcessingTimeS(timeInSeconds);
				viewUpdated();
			}
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
		guiImage.setScale(controls.zoom);

		guiImage.repaint();
	}

	class VisualizePanel extends ShapeVisualizePanel {
		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			BoofSwingUtil.antialiasing(g2);

			synchronized (lockProcessing) {

				if (controls.bShowContour) {
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
				if (controls.bShowSquares) {
					List<Polygon2D_F64> polygons = detector.getSquareDetector().getPolygons(null, null);

					g2.setColor(Color.GREEN);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowOrienation) {
					g2.setStroke(new BasicStroke(2));
					for (int i = 0; i < detected.size; i++) {
						VisualizeShapes.drawArrowSubPixel(detected.get(i).distortedPixels, 3, scale, g2);
					}
				}

				if (controls.bShowlabels) {
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

	public static class Detector extends DetectFiducialSquareBinary<GrayF32> {

		public List<GrayU8> squares = new ArrayList<>();
		public List<GrayF32> squaresGray = new ArrayList<>();

		protected Detector( ConfigFiducialBinary config, InputToBinary<GrayF32> inputToBinary,
							DetectPolygonBinaryGrayRefine<GrayF32> quadDetector ) {
			super(config.gridWidth, config.borderWidthFraction, config.minimumBlackBorderFraction, inputToBinary,
					quadDetector, GrayF32.class);
		}

		@Override
		protected boolean processSquare( GrayF32 square, Result result, double a, double b ) {
			squares.clear();
			squaresGray.clear();

			if (super.processSquare(square, result, a, b)) {
				squares.add(super.getBinaryInner().clone());
				squaresGray.add(super.getGrayNoBorder().clone());
				return true;
			}

			return false;
		}
	}

	public static void main( String[] args ) {

		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("fiducial/binary/image0000.jpg"));
		examples.add(UtilIO.pathExample("fiducial/binary/image0001.jpg"));
		examples.add(UtilIO.pathExample("fiducial/binary/image0002.jpg"));
		examples.add(UtilIO.pathExample("fiducial/binary/movie.mjpeg"));

		SwingUtilities.invokeLater(() -> {
			DetectFiducialSquareBinaryApp app = new DetectFiducialSquareBinaryApp(examples);
			app.openFile(new File(examples.get(0)));
			app.display("Fiducial Square Binary Detector");
		});
	}
}
