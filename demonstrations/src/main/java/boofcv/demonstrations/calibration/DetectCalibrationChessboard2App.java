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

package boofcv.demonstrations.calibration;

import boofcv.abst.fiducial.calib.CalibrationDetectorChessboard2;
import boofcv.abst.fiducial.calib.ConfigChessboard2;
import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.feature.detect.chess.DetectChessboardCorners;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.core.graph.FeatureGraph2D;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.calibration.DisplayPinholeCalibrationPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import javax.swing.border.TitledBorder;
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
 * Detects a chessboard pattern for calibration. Visualizes internal computations.
 *
 * @author Peter Abeles
 */
public class DetectCalibrationChessboard2App
		extends DemonstrationBase
{
	ConfigChessboard2 config = new ConfigChessboard2(5,7,1);

	// displays intensity image
	DisplayPanel imagePanel = new DisplayPanel();
	ControlPanel controlPanel;

	// intensity image is rendered here
	BufferedImage visualized;
	BufferedImage binary;
	BufferedImage original;


	GrayF32 logIntensity = new GrayF32(1,1);

	CalibrationDetectorChessboard2 detector;
	// used to compute feature intensity
	final Object lockAlgorithm = new Object();

	//-----------------
	final Object lockCorners = new Object();
	FastQueue<ChessboardCorner> foundCorners = new FastQueue<>(ChessboardCorner.class,true);
	FastQueue<PointIndex2D_F64> foundChessboard = new FastQueue<>(PointIndex2D_F64.class,true);
	FastQueue<FeatureGraph2D> foundClusters = new FastQueue<>(FeatureGraph2D.class,true);
	boolean success;
	//-----------------

	public DetectCalibrationChessboard2App(List<PathLabel> examples ) {
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

		imagePanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if( SwingUtilities.isLeftMouseButton(e)) {
					Point2D_F64 p = imagePanel.pixelToPoint(e.getX(), e.getY());
					System.out.printf("Clicked at %.2f , %.2f\n",p.x,p.y);
				}
			}
		});
	}

	private void createAlgorithm() {
		synchronized (lockAlgorithm) {
			ConfigThreshold threshold = controlPanel.thresholdPanel.createConfig();
			threshold.maxPixelValue = DetectChessboardCorners.GRAY_LEVELS;
			config.threshold = threshold;
			config.pyramidTopSize = controlPanel.pyramidTop;
			config.cornerRadius = controlPanel.radius;
			config.cornerThreshold = controlPanel.cornerThreshold;

			detector = new CalibrationDetectorChessboard2(config);
			detector.getDetector().getDetector().useMeanShift = controlPanel.meanShift;
			detector.getClusterFinder().setOrientationTol(controlPanel.orientationTol);
			detector.getClusterFinder().setDirectionTol(controlPanel.directionTol);
			detector.getClusterFinder().setDistanceTol(controlPanel.distanceTol);

			if( controlPanel.anyGrid ) {
				detector.getClusterToGrid().setCheckShape(null);
			}
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

		GrayF32 featureImg;
		double processingTime;
		synchronized (lockAlgorithm) {
			long time0 = System.nanoTime();
			boolean success = detector.process(gray);
			long time1 = System.nanoTime();
			processingTime = (time1-time0)*1e-6; // milliseconds

			featureImg = detector.getDetector().getDetector().getIntensity();

			controlPanel.thresholdPanel.updateHistogram(featureImg);

			if( controlPanel.logItensity ) {
				PixelMath.log(featureImg,logIntensity);
				VisualizeImageData.colorizeSign(logIntensity, visualized, ImageStatistics.maxAbs(logIntensity));
			} else {
				VisualizeImageData.colorizeSign(featureImg, visualized, ImageStatistics.maxAbs(featureImg));
			}

			System.out.println("success = "+success);

			binary=VisualizeBinaryData.renderBinary(detector.getDetector().getDetector().getBinary(),false,binary);

			synchronized (lockCorners) {
				this.success = success;
				FastQueue<ChessboardCorner> orig = detector.getDetector().getCorners();
				foundCorners.reset();
				for (int i = 0; i < orig.size; i++) {
					foundCorners.grow().set( orig.get(i) );
				}
				if( success ) {
					CalibrationObservation detected = detector.getDetectedPoints();
					foundChessboard.reset();
					for (int i = 0; i < detected.size(); i++) {
						foundChessboard.grow().set(detected.get(i));
					}
				}

				foundClusters.reset();
				FastQueue<ChessboardCornerGraph> clusters = detector.getClusterFinder().getOutputClusters();
				for (int i = 0; i < clusters.size; i++) {
					clusters.get(i).convert(foundClusters.grow());
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

			Line2D.Double line = new Line2D.Double();
			if( controlPanel.showCorners) {
				synchronized (lockCorners) {
					g2.setStroke(new BasicStroke(3));
					for (int i = 0; i < foundCorners.size; i++) {
						ChessboardCorner c = foundCorners.get(i);
						double x = c.x;
						double y = c.y;

						g2.setColor(Color.ORANGE);
						VisualizeFeatures.drawCircle(g2, x * scale, y * scale, 5, circle);

						double dx = 4*Math.cos(c.orientation);
						double dy = 4*Math.sin(c.orientation);

						g2.setColor(Color.CYAN);
						line.setLine((x-dx)*scale,(y-dy)*scale,(x+dx)*scale,(y+dy)*scale);
						g2.draw(line);
					}

					if( controlPanel.showNumbers ) {
						DisplayPinholeCalibrationPanel.drawIndexes(g2,18,(List)foundCorners.toList(),null,scale);
					}
				}
			}

			if( controlPanel.showClusters ) {
				synchronized (lockCorners) {
					int width = img.getWidth();
					int height = img.getHeight();


					for (int i = 0; i < foundClusters.size; i++) {
						FeatureGraph2D graph = foundClusters.get(i);

						// draw black outline
						g2.setStroke(new BasicStroke(5));
						g2.setColor(Color.black);
						renderGraph(g2, line, graph);

						// make each graph a different color depending on position
						FeatureGraph2D.Node n0 = graph.nodes.get(0);
						int color = (int)((n0.x/width)*255) << 16 | ((int)((n0.y/height)*200)+55) << 8 | 255;
						g2.setColor(new Color(color));
						g2.setStroke(new BasicStroke(3));
						renderGraph(g2, line, graph);
					}
				}
			}

			if( success && controlPanel.showChessboards ) {
				synchronized (lockCorners) {
					DisplayPinholeCalibrationPanel.renderOrder(g2, scale, (List)foundChessboard.toList());

					if( controlPanel.showNumbers ) {
						DisplayPinholeCalibrationPanel.drawNumbers(g2, foundChessboard.toList(), null, scale);
					}
				}
			}
		}

		private void renderGraph(Graphics2D g2, Line2D.Double line, FeatureGraph2D graph) {
			for (int j = 0; j < graph.edges.size; j++) {
				FeatureGraph2D.Edge e = graph.edges.get(j);
				Point2D_F64 a = graph.nodes.get(e.src);
				Point2D_F64 b = graph.nodes.get(e.dst);

				line.setLine(a.x * scale, a.y * scale, b.x * scale, b.y * scale);
				g2.draw(line);
			}
		}
	}

	class ControlPanel extends DetectBlackShapePanel
			implements ActionListener , ChangeListener , ThresholdControlPanel.Listener
	{
		JComboBox<String> comboView;
		JCheckBox checkLogIntensity;
		JSpinner spinnerRadius;
		JSpinner spinnerCornerThreshold;
		JSpinner spinnerTop;
		JSpinner spinnerDistanceTol;
		JSpinner spinnerDirectionTol;
		JSpinner spinnerOrientationTol;
		JCheckBox checkShowTargets;
		JCheckBox checkShowNumbers;
		JCheckBox checkShowClusters;
		JCheckBox checkShowCorners;
		JCheckBox checkMeanShift;
		JCheckBox checkAnyGrid;
		public ThresholdControlPanel thresholdPanel;

		int pyramidTop = 100;
		int radius = 1;
		double cornerThreshold;
		double distanceTol;
		double directionTol;
		double orientationTol;
		boolean showChessboards = true;
		boolean showNumbers = true;
		boolean showClusters = false;
		boolean showCorners = false;
		boolean logItensity =false;
		int view = 1;
		boolean meanShift = true;
		boolean anyGrid = false;

		public ControlPanel() {
			{
				ConfigChessboard2 config = new ConfigChessboard2(1,1,1);
				thresholdPanel = new ThresholdControlPanel(this,config.threshold);
				thresholdPanel.addHistogramGraph();
				// use the actual image histogram. Peaks are rare and the approximate one will be very inaccurate
				thresholdPanel.histogramPanel.setApproximateHistogram(false);

				cornerThreshold = config.cornerThreshold;
			}

			ChessboardCornerClusterFinder finder = new ChessboardCornerClusterFinder();
			distanceTol = finder.getDistanceTol();
			directionTol = finder.getDirectionTol();
			orientationTol = finder.getOrientationTol();

			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1.0);
			checkLogIntensity = checkbox("Log Intensity", logItensity);
			comboView = combo(view,"Intensity","Image","Both","Binary");
			spinnerCornerThreshold = spinner(cornerThreshold, 0, 100, 0.2);
			spinnerRadius = spinner(radius, 1, 100, 1);
			spinnerTop = spinner(pyramidTop, 50, 10000, 50);
			spinnerOrientationTol = spinner(orientationTol,0,3.0,0.05,1,3);
			spinnerDirectionTol = spinner(directionTol,0,3.0,0.05,1,3);
			spinnerDistanceTol = spinner(distanceTol,0,1.0,0.05,1,3);
			checkShowTargets = checkbox("Chessboard", showChessboards);
			checkShowNumbers = checkbox("Numbers", showNumbers);
			checkShowClusters = checkbox("Clusters", showClusters);
			checkShowCorners = checkbox("Corners", showCorners);
			checkMeanShift = checkbox("Mean Shift", meanShift);
			checkAnyGrid = checkbox("Any Grid",anyGrid);

			addLabeled(processingTimeLabel, "Time (ms)");
			addLabeled(imageSizeLabel,"Image Size");
			addLabeled(comboView,"View");
			addLabeled(selectZoom,"Zoom");
			add(createCheckPanel());
			addAlignLeft(checkMeanShift);
			addAlignLeft(checkAnyGrid);
			addLabeled(spinnerRadius,"Corner Radius");
			addLabeled(spinnerCornerThreshold,"Corner Threshold");
			addLabeled(spinnerTop,"Pyramid Top");
			addLabeled(spinnerOrientationTol,"Orientation Tol");
			addLabeled(spinnerDirectionTol,"Direction Tol");
			addLabeled(spinnerDistanceTol,"Distance Tol");
			addAlignCenter(thresholdPanel);
		}

		public JPanel createCheckPanel() {
			JPanel panel = new JPanel(new GridLayout(0,2));

			panel.add(checkShowTargets);
			panel.add(checkShowNumbers);
			panel.add(checkShowClusters);
			panel.add(checkShowCorners);
			panel.add(checkLogIntensity);

			panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
					"Visualize",TitledBorder.CENTER, TitledBorder.TOP));

			panel.setPreferredSize(panel.getMinimumSize());
			panel.setMaximumSize(panel.getMinimumSize());

			return panel;
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
			} else if( e.getSource() == checkShowTargets) {
				showChessboards = checkShowTargets.isSelected();
				imagePanel.repaint();
			} else if( e.getSource() == checkShowNumbers) {
				showNumbers = checkShowNumbers.isSelected();
				imagePanel.repaint();
			} else if( e.getSource() == checkShowClusters) {
				showClusters = checkShowClusters.isSelected();
				imagePanel.repaint();
			} else if( e.getSource() == checkMeanShift) {
				meanShift = checkMeanShift.isSelected();
				createAlgorithm();
				reprocessImageOnly();
			} else if( e.getSource() == checkAnyGrid) {
				anyGrid = checkAnyGrid.isSelected();
				createAlgorithm();
				reprocessImageOnly();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == selectZoom ) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				imagePanel.setScale(zoom);
			} else if( e.getSource() == spinnerOrientationTol ) {
				orientationTol = ((Number)spinnerOrientationTol.getValue()).doubleValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerDirectionTol ) {
				directionTol = ((Number)spinnerDirectionTol.getValue()).doubleValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerDistanceTol ) {
				distanceTol = ((Number)spinnerDistanceTol.getValue()).doubleValue();
				createAlgorithm();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerCornerThreshold ) {
				cornerThreshold = ((Number)spinnerCornerThreshold.getValue()).doubleValue();
				createAlgorithm();
				reprocessImageOnly();
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

	// TODO render Option to render numbers
	// TODO render multiple chessboards, not just ones of a specific size

	// TODO option to tweak clustering magic numbers

	public static void main( String args[] ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Chessboard",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame06.jpg")));
		examples.add(new PathLabel("Fisheye",UtilIO.pathExample("calibration/fisheye/chessboard/1.jpg")));
		examples.add(new PathLabel("QR Movie",UtilIO.pathExample("fiducial/qrcode/movie.mp4")));
		examples.add(new PathLabel("Chessboard Movie",UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));
		examples.add(new PathLabel("TEMP 1","/home/pja/projects/ValidationBoof/data/fiducials/chessboard/standard/focus/image01.png"));
		examples.add(new PathLabel("TEMP 2","/home/pja/projects/ValidationBoof/data/fiducials/chessboard/standard/rotation/image01.jpg"));

		SwingUtilities.invokeLater(()->{
			DetectCalibrationChessboard2App app = new DetectCalibrationChessboard2App(examples);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Chessboard Corner Detector");
		});
	}
}
