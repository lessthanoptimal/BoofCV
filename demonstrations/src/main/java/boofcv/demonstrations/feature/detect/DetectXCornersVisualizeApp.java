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

package boofcv.demonstrations.feature.detect;

import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersXPyramid;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Displays the intensity of detected features inside an image
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectXCornersVisualizeApp extends DemonstrationBase {
	// displays intensity image
	DisplayPanel imagePanel = new DisplayPanel();
	ControlPanel controlPanel;

	// intensity image is rendered here
	BufferedImage visualized;
	BufferedImage original;

	GrayF32 logIntensity = new GrayF32(1, 1);

	DetectChessboardCornersXPyramid<GrayF32> detector = new DetectChessboardCornersXPyramid<>(ImageType.SB_F32);

	// used to compute feature intensity
	final Object lockAlgorithm = new Object();

	final Object lockCorners = new Object();
	DogArray<ChessboardCorner> foundCorners = new DogArray<>(ChessboardCorner::new);

	public DetectXCornersVisualizeApp( List<PathLabel> examples ) {
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

		imagePanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					Point2D_F64 p = imagePanel.pixelToPoint(e.getX(), e.getY());
					float value = detector.getDetector().getIntensityRaw().get((int)p.x, (int)p.y);
					System.out.printf("Clicked at ( %.2f , %.2f ) x-corner = %e \n", p.x, p.y, value);
				}
			}
		});
	}

	private void createAlgorithm() {
		synchronized (lockAlgorithm) {
			detector.setPyramidTopSize(controlPanel.pyramidTop);
			detector.getDetector().useMeanShift = controlPanel.meanShift;
			detector.getDetector().edgeIntensityRatioThreshold = controlPanel.threshEdgeIntensity;
			detector.getDetector().edgeAspectRatioThreshold = controlPanel.threshEdgeAspect;
			detector.getDetector().nonmaxThresholdRatio = controlPanel.threshXCorner;
			detector.getDetector().refinedXCornerThreshold = controlPanel.thresholdIntensity;
			detector.getDetector().setNonmaxRadius(controlPanel.nonmaxRadius);
		}
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
		GrayF32 gray = (GrayF32)input;

		if (controlPanel.scaleDown > 1) {
			int scale = controlPanel.scaleDown;
			GrayF32 scaled = new GrayF32(input.width/scale, input.height/scale);
			AverageDownSampleOps.down(gray, scaled);
			gray = scaled;

			visualized = ConvertBufferedImage.checkDeclare(scaled.width, scaled.height, visualized, BufferedImage.TYPE_INT_RGB);

			original = ConvertBufferedImage.checkDeclare(scaled.width, scaled.height, original, buffered.getType());
			Graphics2D g2 = original.createGraphics();
			g2.setTransform(AffineTransform.getScaleInstance(1.0/scale, 1.0/scale));
			g2.drawImage(buffered, 0, 0, null);
		} else {
			visualized = ConvertBufferedImage.checkDeclare(gray.width, gray.height, visualized, BufferedImage.TYPE_INT_RGB);
			original = ConvertBufferedImage.checkDeclare(gray.width, gray.height, original, buffered.getType());
			original = ConvertBufferedImage.checkCopy(buffered, original);
		}

		GrayF32 featureImg;
		double processingTime;
		synchronized (lockAlgorithm) {
			long time0 = System.nanoTime();
			detector.process(gray);
			long time1 = System.nanoTime();
			processingTime = (time1 - time0)*1e-6; // milliseconds

			featureImg = detector.getDetector().getIntensityRaw();

			if (controlPanel.logItensity) {
				PixelMath.logSign(featureImg, 1.0f, logIntensity);
				VisualizeImageData.colorizeSign(logIntensity, visualized, ImageStatistics.maxAbs(logIntensity));
			} else {
				VisualizeImageData.colorizeSign(featureImg, visualized, ImageStatistics.maxAbs(featureImg));
			}

			synchronized (lockCorners) {
				DogArray<ChessboardCorner> orig = detector.getCorners();
				foundCorners.reset();
				for (int i = 0; i < orig.size(); i++) {
					foundCorners.grow().setTo(orig.get(i));
				}
			}
		}

		SwingUtilities.invokeLater(() -> {
			imagePanel.setBufferedImageNoChange(original);
			controlPanel.setProcessingTimeMS(processingTime);
			imagePanel.repaint();
		});
	}

	class DisplayPanel extends ShapeVisualizePanel {
		Ellipse2D.Double circle = new Ellipse2D.Double();

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

			if (controlPanel.showCorners) {
				Line2D.Double line = new Line2D.Double();

				synchronized (lockCorners) {
					g2.setStroke(new BasicStroke(3));
					for (int i = 0; i < foundCorners.size; i++) {
						ChessboardCorner c = foundCorners.get(i);
						double x = c.x;
						double y = c.y;

						g2.setColor(Color.ORANGE);
						VisualizeFeatures.drawCircle(g2, x*scale, y*scale, 5, circle);

						double dx = 4*Math.cos(c.orientation);
						double dy = 4*Math.sin(c.orientation);

						g2.setColor(Color.CYAN);
						line.setLine((x - dx)*scale, (y - dy)*scale, (x + dx)*scale, (y + dy)*scale);
						g2.draw(line);
					}
				}
			}
		}

		@Override
		public synchronized void setScale( double scale ) {
			controlPanel.setZoom(scale);
			super.setScale(controlPanel.zoom);
		}
	}

	class ControlPanel extends DetectBlackShapePanel
			implements ActionListener, ChangeListener, ThresholdControlPanel.Listener {
		JCheckBox checkLogIntensity;
		JSpinner spinnerScaleDown;
		JSpinner spinnerTop;
		JSpinner spinnerEdgeIntensity;
		JSpinner spinnerEdgeAspect;
		JSpinner spinnerXCorner;
		JSpinner spinnerIntensity;
		JSpinner spinnerNonMaxRadius;
		JCheckBox checkShowCorners;
		JCheckBox checkDebug;
		JSlider sliderTranslucent = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);

		int pyramidTop = 100;
		boolean showCorners = true;
		boolean logItensity = false;
		int view = 0;
		boolean meanShift = true;
		int translucent = 0;
		int scaleDown = 1;
		double threshEdgeIntensity;
		double threshEdgeAspect;
		float threshXCorner;
		double thresholdIntensity;
		int nonmaxRadius;

		public ControlPanel() {
			{
				threshEdgeIntensity = detector.getDetector().getEdgeIntensityRatioThreshold();
				threshEdgeAspect = detector.getDetector().edgeAspectRatioThreshold;
				threshXCorner = detector.getDetector().getNonmaxThresholdRatio();
				nonmaxRadius = detector.getDetector().getNonmaxRadius();
				thresholdIntensity = detector.getDetector().refinedXCornerThreshold;
			}

			selectZoom = spinner(1.0, MIN_ZOOM, MAX_ZOOM, 1.0);
			checkLogIntensity = checkbox("Log Intensity", logItensity);
			spinnerEdgeIntensity = spinner(threshEdgeIntensity, 0.0, 1.0, 0.01, 1, 4);
			spinnerEdgeAspect = spinner(threshEdgeAspect, 0.0, 1.0, 0.01, 1, 4);
			spinnerXCorner = spinner(threshXCorner, 0.0, 1.0, 0.01, 1, 4);
			spinnerIntensity = spinner(thresholdIntensity, 0.0, 1000.0, 5, 3, 1);
			spinnerNonMaxRadius = spinner(nonmaxRadius, 1, 20, 1);
			spinnerScaleDown = spinner(scaleDown, 1, 128, 1);
			spinnerTop = spinner(pyramidTop, 50, 10000, 50);
			checkShowCorners = checkbox("Show Corners", showCorners);
			checkDebug = checkbox("Mean Shift", meanShift);

			sliderTranslucent.setMaximumSize(new Dimension(120, 26));
			sliderTranslucent.setPreferredSize(sliderTranslucent.getMaximumSize());
			sliderTranslucent.addChangeListener(this);

			addLabeled(processingTimeLabel, "Time (ms)");
			addLabeled(imageSizeLabel, "Image Size");
			addLabeled(spinnerScaleDown, "Scale Down");
			addLabeled(sliderTranslucent, "X-Corners");
			addLabeled(selectZoom, "Zoom");
			addAlignLeft(checkLogIntensity);
			addAlignLeft(checkShowCorners);
			addAlignLeft(checkDebug);
			addLabeled(spinnerEdgeIntensity, "Edge Intensity");
			addLabeled(spinnerEdgeAspect, "Edge Aspect");
			addLabeled(spinnerXCorner, "X-Corner");
			addLabeled(spinnerIntensity, "Intensity");
			addLabeled(spinnerNonMaxRadius, "NonMax Radius");
			addLabeled(spinnerTop, "Pyramid Top");
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (e.getSource() == checkShowCorners) {
				showCorners = checkShowCorners.isSelected();
				imagePanel.repaint();
			} else if (e.getSource() == checkLogIntensity) {
				logItensity = checkLogIntensity.isSelected();
				reprocessImageOnly();
			} else if (e.getSource() == checkDebug) {
				meanShift = checkDebug.isSelected();
				createAlgorithm();
				reprocessImageOnly();
			}
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == selectZoom) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				imagePanel.setScale(zoom);
			} else if (e.getSource() == spinnerEdgeIntensity) {
				threshEdgeIntensity = ((Number)spinnerEdgeIntensity.getValue()).doubleValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerEdgeAspect) {
				threshEdgeAspect = ((Number)spinnerEdgeAspect.getValue()).doubleValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerXCorner) {
				threshXCorner = ((Number)spinnerXCorner.getValue()).floatValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerIntensity) {
				thresholdIntensity = ((Number)spinnerIntensity.getValue()).floatValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerNonMaxRadius) {
				nonmaxRadius = ((Number)spinnerNonMaxRadius.getValue()).intValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerTop) {
				pyramidTop = ((Number)spinnerTop.getValue()).intValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerScaleDown) {
				scaleDown = ((Number)spinnerScaleDown.getValue()).intValue();
				reprocessImageOnly();
			} else if (e.getSource() == sliderTranslucent) {
				translucent = ((Number)sliderTranslucent.getValue()).intValue();
				imagePanel.repaint();
			}
		}

		@Override
		public void imageThresholdUpdated() {
			createAlgorithm();
			reprocessImageOnly();
		}
	}

	public static void main( String[] args ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Chessboard", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame06.jpg")));
		examples.add(new PathLabel("Square Grid", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Square/frame06.jpg")));
		examples.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("beach", UtilIO.pathExample("scale/beach02.jpg")));
		examples.add(new PathLabel("Chessboard Movie", UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		SwingUtilities.invokeLater(() -> {
			DetectXCornersVisualizeApp app = new DetectXCornersVisualizeApp(examples);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("X-Corner Detector");
		});
	}
}
