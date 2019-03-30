/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.detect.chess.DetectChessboardCorners;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersPyramid;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;

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
public class DetectChessboardCornersVisualizeApp
		extends DemonstrationBase
{
	// displays intensity image
	DisplayPanel imagePanel = new DisplayPanel();
	ControlPanel controlPanel;

	// intensity image is rendered here
	BufferedImage visualized;
	BufferedImage binary;
	BufferedImage original;


	GrayF32 logIntensity = new GrayF32(1,1);

	DetectChessboardCornersPyramid detector = new DetectChessboardCornersPyramid();

	// used to compute feature intensity
	final Object lockAlgorithm = new Object();

	final Object lockCorners = new Object();
	FastQueue<ChessboardCorner> foundCorners = new FastQueue<>(ChessboardCorner.class,true);

	public DetectChessboardCornersVisualizeApp(List<PathLabel> examples ) {
		super(true,true,examples,ImageType.single(GrayF32.class));

		controlPanel = new ControlPanel();
		add(BorderLayout.WEST,controlPanel);
		add(BorderLayout.CENTER,imagePanel);

		createAlgorithm();

		imagePanel.getImagePanel().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if( e.getKeyCode()==KeyEvent.VK_SPACE) {
					streamPaused = !streamPaused;
				}
			}
		});

		imagePanel.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				double curr =controlPanel.zoom;

				if( e.getWheelRotation() > 0 )
					curr *= 1.1;
				else
					curr /= 1.1;
				controlPanel.setZoom(curr);
			}
		});
	}

	private void createAlgorithm() {
		synchronized (lockAlgorithm) {
			ConfigThreshold threshold = controlPanel.thresholdPanel.createConfig();
			threshold.maxPixelValue = DetectChessboardCorners.GRAY_LEVELS;

			DetectChessboardCorners corners = new DetectChessboardCorners();
			corners.setKernelRadius(controlPanel.radius);
			corners.useMeanShift = controlPanel.debug;
			detector = new DetectChessboardCornersPyramid(corners);
			detector.setPyramidTopSize(controlPanel.pyramidTop);
			detector.getDetector().setThresholding(FactoryThresholdBinary.threshold(threshold,GrayF32.class));
		}
	}

	@Override
	protected void configureVideo(int which, SimpleImageSequence sequence) {
		sequence.setLoop(true);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		visualized = ConvertBufferedImage.checkDeclare(width,height, visualized,BufferedImage.TYPE_INT_RGB);

		SwingUtilities.invokeLater(()->{
			Dimension preferred = imagePanel.getPreferredSize();

			boolean sizeChange = preferred.width != width || preferred.height != height;
			if( sizeChange ) {
				imagePanel.setPreferredSize(new Dimension(width, height));

				double scale = BoofSwingUtil.selectZoomToShowAll(imagePanel, width, height);
//			controlPanel.zoom = scale; // prevent it from broadcasting an event
				controlPanel.setZoom(scale);
				imagePanel.setScaleAndCenter(scale, width / 2, height / 2);
				controlPanel.setImageSize(width, height);
			}
		});
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {

		original = ConvertBufferedImage.checkCopy(buffered, original);

		GrayF32 gray = (GrayF32) input;

		controlPanel.thresholdPanel.updateHistogram(gray);

		GrayF32 featureImg;
		double processingTime;
		synchronized (lockAlgorithm) {
			long time0 = System.nanoTime();
			detector.process(gray);
			long time1 = System.nanoTime();
			processingTime = (time1-time0)*1e-6; // milliseconds

			featureImg = detector.getDetector().getIntensity();

			if( controlPanel.logItensity ) {
				PixelMath.log(featureImg,logIntensity);
				VisualizeImageData.colorizeSign(logIntensity, visualized, ImageStatistics.maxAbs(logIntensity));
			} else {
				VisualizeImageData.colorizeSign(featureImg, visualized, ImageStatistics.maxAbs(featureImg));
			}

			binary=VisualizeBinaryData.renderBinary(detector.getDetector().getBinary(),false,binary);

			synchronized (lockCorners) {
				FastQueue<ChessboardCorner> orig = detector.getCorners();
				foundCorners.reset();
				for (int i = 0; i < orig.size; i++) {
					foundCorners.grow().set( orig.get(i) );
				}
			}
		}

		SwingUtilities.invokeLater(() -> {
			controlPanel.setProcessingTimeMS(processingTime);
			changeViewImage();
			imagePanel.repaint();
		});
	}

	private void changeViewImage() {
		if( controlPanel.view == 0 || controlPanel.view == 2 ) {
			imagePanel.setBufferedImageNoChange(visualized);
		} else if( controlPanel.view == 1 ) {
			imagePanel.setBufferedImageNoChange(original);
		} else if( controlPanel.view == 3 ) {
			imagePanel.setBufferedImageNoChange(binary);
		}
	}

	class DisplayPanel extends ShapeVisualizePanel {
		Ellipse2D.Double circle = new Ellipse2D.Double();
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			super.paintInPanel(tran, g2);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if(  controlPanel.view == 2 ) {
				// this requires some explaining
				// for some reason it was decided that the transform would apply a translation, but not a scale
				// so this scale will be concatted on top of the translation in the g2
				tran.setTransform(scale,0,0,scale,0,0);
				Composite beforeAC = g2.getComposite();
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
				g2.setComposite(ac);
				g2.drawImage(original,tran,null);
				g2.setComposite(beforeAC);
			}

			if( controlPanel.showCorners) {
				Line2D.Double line = new Line2D.Double();

				synchronized (lockCorners) {
					g2.setStroke(new BasicStroke(3));
					for (int i = 0; i < foundCorners.size; i++) {
						ChessboardCorner c = foundCorners.get(i);
						// shift the corner over 1/2 a pixel so that it appears inside the pixels' center
						// this visually looks better
						double x = c.x + 0.5;
						double y = c.y + 0.5;

						g2.setColor(Color.ORANGE);
						VisualizeFeatures.drawCircle(g2, x * scale, y * scale, 5, circle);

						double dx = 4*Math.cos(c.orientation);
						double dy = 4*Math.sin(c.orientation);

						g2.setColor(Color.CYAN);
						line.setLine((x-dx)*scale,(y-dy)*scale,(x+dx)*scale,(y+dy)*scale);
						g2.draw(line);
					}
				}
			}
		}
	}

	class ControlPanel extends DetectBlackShapePanel
			implements ActionListener , ChangeListener , ThresholdControlPanel.Listener
	{
		JComboBox<String> comboView;
		JCheckBox checkLogIntensity;
		JSpinner spinnerRadius;
		JSpinner spinnerTop;
		JCheckBox checkShowCorners;
		JCheckBox checkDebug;
		public ThresholdControlPanel thresholdPanel;

		int pyramidTop = 100;
		int radius = 1;
		boolean showCorners =true;
		boolean logItensity =false;
		int view = 1;
		boolean debug = true;

		public ControlPanel() {
			{
				ConfigThreshold config = ConfigThreshold.global(ThresholdType.GLOBAL_OTSU);
				config.down = false;
				thresholdPanel = new ThresholdControlPanel(this,config);
				thresholdPanel.addHistogramGraph();
			}

			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1.0);
			checkLogIntensity = checkbox("Log Intensity", logItensity);
			comboView = combo(view,"Intensity","Image","Both","Binary");
			spinnerRadius = spinner(radius, 1, 100, 1);
			spinnerTop = spinner(pyramidTop, 50, 10000, 50);
			checkShowCorners = checkbox("Show Corners", showCorners);
			checkDebug = checkbox("Debug",debug);

			addLabeled(processingTimeLabel, "Time (ms)");
			addLabeled(imageSizeLabel,"Image Size");
			addLabeled(comboView,"View");
			addLabeled(selectZoom,"Zoom");
			addAlignLeft(checkLogIntensity);
			addAlignLeft(checkShowCorners);
			addAlignLeft(checkDebug);
			addLabeled(spinnerRadius,"Corner Radius");
			addLabeled(spinnerTop,"Pyramid Top");
			addAlignCenter(thresholdPanel);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == comboView ) {
				view = comboView.getSelectedIndex();
				changeViewImage();
				imagePanel.repaint();
			} else if( e.getSource() == checkShowCorners) {
				showCorners = checkShowCorners.isSelected();
				imagePanel.repaint();
			} else if( e.getSource() == checkLogIntensity) {
				logItensity = checkLogIntensity.isSelected();
				reprocessImageOnly();
			} else if( e.getSource() == checkDebug) {
				debug = checkDebug.isSelected();
				createAlgorithm();
				reprocessImageOnly();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == selectZoom ) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				imagePanel.setScale(zoom);
			} else if( e.getSource() == spinnerRadius ) {
				radius = ((Number)spinnerRadius.getValue()).intValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerTop ) {
				pyramidTop = ((Number)spinnerTop.getValue()).intValue();
				createAlgorithm();
				reprocessImageOnly();
			}
		}

		@Override
		public void imageThresholdUpdated() {
			createAlgorithm();
			reprocessImageOnly();
		}
	}

	// TODO visualize contours
	// TODO show each layer in the pyramid
	// TODO control corner intensity threshold

	public static void main( String args[] ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Chessboard",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame06.jpg")));
		examples.add(new PathLabel("Square Grid",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Square/frame06.jpg")));
		examples.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("sunflowers",UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("beach",UtilIO.pathExample("scale/beach02.jpg")));
		examples.add(new PathLabel("Chessboard Movie",UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		SwingUtilities.invokeLater(()->{
			DetectChessboardCornersVisualizeApp app = new DetectChessboardCornersVisualizeApp(examples);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Feature Intensity");
		});
	}
}
