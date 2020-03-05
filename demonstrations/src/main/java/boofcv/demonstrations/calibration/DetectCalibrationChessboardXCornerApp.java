/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.CalibrationDetectorChessboardX;
import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder.LineInfo;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder.Vertex;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridInfo;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.core.graph.FeatureGraph2D;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
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
import georegression.metric.UtilAngle;
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
public class DetectCalibrationChessboardXCornerApp
		extends DemonstrationBase
{
	private ConfigChessboardX configDetector = new ConfigChessboardX();
	private ConfigGridDimen configGridDimen = new ConfigGridDimen(5,7,1);

	// GUI Controls and visualization panels
	private DisplayPanel imagePanel = new DisplayPanel();
	private ControlPanel controlPanel;

	// Workspace for visualized image data
	private BufferedImage visualized;
	private BufferedImage original;

	// The chessboard corner detector
	private CalibrationDetectorChessboardX detector;

	//--------------------
	// used to compute feature intensity
	private final Object lockAlgorithm = new Object();
	// workspace to store computed log intensity image used for visualizing feature intensity
	private GrayF32 logIntensity = new GrayF32(1,1);

	//-----------------
	private final Object lockCorners = new Object();
	private FastQueue<ChessboardCorner> foundCorners = new FastQueue<>(ChessboardCorner::new);
	private FastQueue<PointIndex2D_F64> foundChessboard = new FastQueue<>(PointIndex2D_F64::new);
	private FastQueue<CalibrationObservation> foundGrids = new FastQueue<>(CalibrationObservation::new);
	private FastQueue<FeatureGraph2D> foundClusters = new FastQueue<>(FeatureGraph2D::new);
	private boolean success;
	//-----------------

	DetectCalibrationChessboardXCornerApp(List<PathLabel> examples ) {
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

		imagePanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Point2D_F64 p = imagePanel.pixelToPoint(e.getX(), e.getY());
				if( SwingUtilities.isLeftMouseButton(e)) {
					System.out.printf("Clicked at %.2f , %.2f\n",p.x,p.y);
				} else if( SwingUtilities.isRightMouseButton(e) ) {
					String text = "";
					// show info for a corner if the user clicked near it
					if( controlPanel.showCorners ) {
						synchronized (lockCorners) {
							ChessboardCorner best = null;
							int bextId = -1;
							// Must be closer if zoomed in. The on screen pixels represent a smaller distance
							double bestDistance = 10/imagePanel.getScale();
							for (int i = 0; i < foundCorners.size; i++) {
								ChessboardCorner c = foundCorners.get(i);
								// make sure it's a visible corner
								if( c.level2 < controlPanel.detMinPyrLevel)
									continue;
								double d = c.distance(p);
								if( d < bestDistance ) {
									bestDistance = d;
									best = c;
									bextId = i;
								}
							}

							if( best != null ) {
								text = String.format("Corner #%d\n",bextId);
								text += String.format("  pixel (%7.1f , %7.1f )\n",best.x,best.y);
								text += String.format("  levels %d to %d\n",best.level1,best.level2);
								text += String.format("  levelMax %d\n",best.levelMax);
								text += String.format("  intensity %f\n",best.intensity);
								text += String.format("  orientation %.2f (deg)\n", UtilAngle.degree(best.orientation));
							} else {
								text += String.format("  pixel (%7.1f , %7.1f )\n",p.x,p.y);
							}
						}
					} else {
						text += String.format("  pixel (%7.1f , %7.1f )\n",p.x,p.y);
					}
					controlPanel.setInfoText(text);
				}
			}
		});
	}

	private void createAlgorithm() {
		synchronized (lockAlgorithm) {
			configDetector.detPyramidTopSize = controlPanel.detPyramidTop;
			configDetector.detNonMaxRadius = controlPanel.detRadius;
			configDetector.detNonMaxThresholdRatio = controlPanel.detNonMaxThreshold /100.0;
			configDetector.detRefinedXCornerThreshold = controlPanel.detRefinedXThresh;
			configDetector.connEdgeThreshold = controlPanel.connEdgeThreshold;
			configDetector.connOrientationTol = controlPanel.connOrientationTol;
			configDetector.connDirectionTol = controlPanel.connDirectionTol;
			configDetector.connAmbiguousTol = controlPanel.connAmbiguousTol;
			if( controlPanel.connMaxDistance == 0 )
				configDetector.connMaxNeighborDistance = Double.MAX_VALUE;
			else
				configDetector.connMaxNeighborDistance = controlPanel.connMaxDistance;

			configGridDimen.numCols = controlPanel.gridCols;
			configGridDimen.numRows = controlPanel.gridRows;

			// check to see if it should be turned off. 0 is allowed, but we will set it to less than zero
			if( configDetector.connEdgeThreshold <= 0 )
				configDetector.connEdgeThreshold = -1;

			detector = new CalibrationDetectorChessboardX(configDetector,configGridDimen);
			detector.getDetector().getDetector().useMeanShift = controlPanel.meanShift;

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

			featureImg = detector.getDetector().getDetector().getIntensityRaw();
//			featureImg = detector.getDetector().getDetector().getIntensity2x2();

			if( controlPanel.logItensity ) {
				PixelMath.logSign(featureImg,0.2f,logIntensity);
				VisualizeImageData.colorizeSign(logIntensity, visualized, ImageStatistics.maxAbs(logIntensity));
			} else {
				VisualizeImageData.colorizeSign(featureImg, visualized, ImageStatistics.maxAbs(featureImg));
			}

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

				{
					FastQueue<GridInfo> found = detector.getFoundChessboard();
					foundGrids.reset();
					for (int i = 0; i < found.size; i++) {
						GridInfo grid = found.get(i);
						CalibrationObservation c = foundGrids.grow();
						c.points.clear();

						for (int j = 0; j < grid.nodes.size(); j++) {
							c.points.add( new PointIndex2D_F64(grid.nodes.get(j),j) );
						}
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
			imagePanel.setBufferedImageNoChange(original);
			imagePanel.repaint();
		});
	}

	class DisplayPanel extends ShapeVisualizePanel {
		Ellipse2D.Double circle = new Ellipse2D.Double();
		BasicStroke stroke2 = new BasicStroke(2);
		BasicStroke stroke5 = new BasicStroke(5);

		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			super.paintInPanel(tran, g2);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if( controlPanel.translucent > 0 ) {
				// this requires some explaining
				// for some reason it was decided that the transform would apply a translation, but not a scale
				// so this scale will be concatted on top of the translation in the g2
				tran.setTransform(scale,0,0,scale,0,0);
				Composite beforeAC = g2.getComposite();
				float translucent = controlPanel.translucent/100.0f;
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, translucent);
				g2.setComposite(ac);
				g2.drawImage(visualized,tran,null);
				g2.setComposite(beforeAC);
			}

			Line2D.Double line = new Line2D.Double();
			if( controlPanel.showCorners) {
				synchronized (lockCorners) {
					for (int i = 0; i < foundCorners.size; i++) {
						ChessboardCorner c = foundCorners.get(i);
						if( c.level2 < controlPanel.detMinPyrLevel)
							continue;

						double x = c.x;
						double y = c.y;

						g2.setStroke(stroke5);
						g2.setColor(Color.BLACK);
						VisualizeFeatures.drawCircle(g2, x * scale, y * scale, 5, circle);
						g2.setStroke(stroke2);
						g2.setColor(Color.ORANGE);
						VisualizeFeatures.drawCircle(g2, x * scale, y * scale, 5, circle);

						double dx = 6*Math.cos(c.orientation);
						double dy = 6*Math.sin(c.orientation);

						g2.setStroke(stroke2);
						g2.setColor(Color.CYAN);
						line.setLine((x-dx)*scale,(y-dy)*scale,(x+dx)*scale,(y+dy)*scale);
						g2.draw(line);
					}

					if( controlPanel.showNumbers ) {
						DisplayPinholeCalibrationPanel.drawIndexes(g2,18,foundCorners.toList(),null,
								controlPanel.detMinPyrLevel,scale);
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

			if( controlPanel.showPerpendicular ) {
				// Locking the algorithm will kill performance.
				synchronized (lockAlgorithm) {
					Font regular = new Font("Serif", Font.PLAIN, 12);
					g2.setFont(regular);
					BasicStroke thin = new BasicStroke(2);
					BasicStroke thick = new BasicStroke(4);
//					g2.setStroke(new BasicStroke(1));
//					List<Vertex> vertexes = detector.getClusterFinder().getVertexes().toList();
					List<LineInfo> lines = detector.getClusterFinder().getLines().toList();

					for (int i = 0; i < lines.size(); i++) {
						LineInfo lineInfo = lines.get(i);
						if( lineInfo.isDisconnected() || lineInfo.parallel )
							continue;

						Vertex va = lineInfo.endA.dst;
						Vertex vb = lineInfo.endB.dst;

						ChessboardCorner ca = foundCorners.get(va.index);
						ChessboardCorner cb = foundCorners.get(vb.index);

						double intensity = lineInfo.intensity == -Double.MAX_VALUE ? Double.NaN : lineInfo.intensity;
						line.setLine(ca.x * scale, ca.y * scale, cb.x * scale, cb.y * scale);

						g2.setStroke(thick);
						g2.setColor(Color.BLACK);
						g2.draw(line);

						g2.setStroke(thin);
						g2.setColor(Color.ORANGE);

						g2.draw(line);

						float x = (float) ((ca.x + cb.x) / 2.0);
						float y = (float) ((ca.y + cb.y) / 2.0);

						g2.setColor(Color.RED);
						g2.drawString(String.format("%.1f", intensity), x * (float) scale, y * (float) scale);
					}
				}
			}

			if( controlPanel.anyGrid ) {
				if( controlPanel.showChessboards ) {
					synchronized (lockCorners) {
						for (int i = 0; i < foundGrids.size; i++) {
							CalibrationObservation c = foundGrids.get(i);
							DisplayPinholeCalibrationPanel.renderOrder(g2, scale, (List) c.points);

							if (controlPanel.showNumbers) {
								DisplayPinholeCalibrationPanel.drawNumbers(g2, c.points, null, scale);
							}
						}
					}
				}
			} else {
				if (success && controlPanel.showChessboards) {
					synchronized (lockCorners) {
						DisplayPinholeCalibrationPanel.renderOrder(g2, scale, (List) foundChessboard.toList());

						if (controlPanel.showNumbers) {
							DisplayPinholeCalibrationPanel.drawNumbers(g2, foundChessboard.toList(), null, scale);
						}
					}
				}
			}
		}

		@Override
		public synchronized void setScale(double scale) {
			controlPanel.setZoom(scale);
			super.setScale(controlPanel.zoom);
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
		JTextArea textArea = new JTextArea();

		int gridRows = configGridDimen.numRows;
		int gridCols = configGridDimen.numCols;
		int detPyramidTop = configDetector.detPyramidTopSize;
		int detRadius = configDetector.detNonMaxRadius;
		int detNonMaxThreshold = (int)(configDetector.detNonMaxThresholdRatio *100);
		double detRefinedXThresh = configDetector.detRefinedXCornerThreshold;
		int connMaxDistance = 0;
		double connEdgeThreshold = configDetector.connEdgeThreshold;
		double connAmbiguousTol = configDetector.connAmbiguousTol;
		double connDirectionTol = configDetector.connDirectionTol;
		double connOrientationTol = configDetector.connOrientationTol;

		boolean showChessboards = true;
		boolean showNumbers = true;
		boolean showClusters = false;
		boolean showPerpendicular = false;
		boolean showCorners = false;
		boolean logItensity =false;
		boolean meanShift = true;
		boolean anyGrid = false;
		int detMinPyrLevel = 0;
		int translucent = 0;

		JSlider sliderTranslucent = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		JCheckBox checkLogIntensity = checkbox("Log Intensity", logItensity);
		JSpinner spinnerDRadius = spinner(detRadius, 1, 100, 1);
		JSpinner spinnerDNonMaxThreshold = spinner(detNonMaxThreshold, 0, 100, 5);
		JSpinner spinnerDRefinedXThreshold = spinner(detRefinedXThresh, 0.0, 20.0, 0.005,2,3);
		JSpinner spinnerDTop = spinner(detPyramidTop, 0, 10000, 50);
		JSpinner spinnerDMinPyrLevel = spinner(detMinPyrLevel,0,20,1);
		JSpinner spinnerCEdgeThreshold = spinner(connEdgeThreshold, 0, 1.0, 0.1,1,3);
		JSpinner spinnerCAmbiguous = spinner(connAmbiguousTol,0,1.0,0.05,1,3);
		JSpinner spinnerCDirectionTol = spinner(connDirectionTol,0,1.0,0.05,1,3);
		JSpinner spinnerCOrientationTol = spinner(connOrientationTol,0,3.0,0.05,1,3);
		JSpinner spinnerCMaxDistance = spinner(connMaxDistance,0,50000,1);

		// select the calibration grid's dimensions
		JSpinner spinnerGridRows = spinner(gridRows,3,500,1);
		JSpinner spinnerGridCols = spinner(gridCols,3,500,1);

		JCheckBox checkShowTargets = checkbox("Chessboard", showChessboards);
		JCheckBox checkShowNumbers = checkbox("Numbers", showNumbers);
		JCheckBox checkShowClusters = checkbox("Clusters", showClusters);
		JCheckBox checkShowPerpendicular = checkbox("Perp.", showPerpendicular);
		JCheckBox checkShowCorners = checkbox("Corners", showCorners);
		JCheckBox checkMeanShift = checkbox("Mean Shift", meanShift);
		JCheckBox checkAnyGrid = checkbox("Any Grid",anyGrid);


		ControlPanel() {
			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);

			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1.0);
			checkLogIntensity = checkbox("Log Intensity", logItensity);

			sliderTranslucent.setMaximumSize(new Dimension(120,26));
			sliderTranslucent.setPreferredSize(sliderTranslucent.getMaximumSize());
			sliderTranslucent.addChangeListener(this);

			StandardAlgConfigPanel controlPanel = new StandardAlgConfigPanel();
			controlPanel.addCenterLabel("Detect",controlPanel);
			controlPanel.addAlignLeft(checkMeanShift);
			controlPanel.addAlignLeft(checkAnyGrid);
			controlPanel.addLabeled(spinnerDRadius,"Corner Radius");
			controlPanel.addLabeled(spinnerDRefinedXThreshold,"Refined X Threshold");
			controlPanel.addLabeled(spinnerDNonMaxThreshold,"Non-Max Threshold");
			controlPanel.addLabeled(spinnerDTop,"Pyramid Top");
			controlPanel.addCenterLabel("Connect",controlPanel);
			controlPanel.addLabeled(spinnerCEdgeThreshold,"Edge Threshold");
			controlPanel.addLabeled(spinnerCMaxDistance,"Max Dist.");
			controlPanel.addLabeled(spinnerCOrientationTol,"Orientation Tol");
			controlPanel.addLabeled(spinnerCDirectionTol,"Direction Tol");
			controlPanel.addLabeled(spinnerCAmbiguous,"Ambiguous Tol");

			JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.addTab("Controls",controlPanel);
			tabbedPane.addTab("Info",textArea);

			addLabeled(processingTimeLabel, "Time (ms)");
			addLabeled(imageSizeLabel,"Image Size");
			addLabeled(sliderTranslucent,"X-Corners");
			addLabeled(selectZoom,"Zoom");
			add(createCheckPanel());
			add(createGridShapePanel());
			add(tabbedPane);
		}

		void setInfoText( String text ) {
			SwingUtilities.invokeLater(()->textArea.setText(text));
		}

		JPanel createCheckPanel() {
			JPanel panelChecks = new JPanel(new GridLayout(0,2));

			panelChecks.add(checkShowTargets);
			panelChecks.add(checkShowNumbers);
			panelChecks.add(checkShowClusters);
			panelChecks.add(checkShowPerpendicular);
			panelChecks.add(checkShowCorners);
			panelChecks.add(checkLogIntensity);

			panelChecks.setPreferredSize(panelChecks.getMinimumSize());
			panelChecks.setMaximumSize(panelChecks.getMinimumSize());

			StandardAlgConfigPanel vertPanel = new StandardAlgConfigPanel();
			vertPanel.add(panelChecks);
			vertPanel.addLabeled(spinnerDMinPyrLevel,"Min Level");

			vertPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
					"Visualize",TitledBorder.CENTER, TitledBorder.TOP));

			return vertPanel;
		}

		JPanel createGridShapePanel() {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

			panel.add(new JLabel("Grid Shape"));
			panel.add( Box.createHorizontalGlue());
			panel.add(spinnerGridRows);
			panel.add(spinnerGridCols);

//			panel.setPreferredSize(panel.getMinimumSize());
//			panel.setMaximumSize(panel.getMinimumSize());

			return panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == checkShowCorners) {
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
			} else if( e.getSource() == checkShowPerpendicular) {
				showPerpendicular = checkShowPerpendicular.isSelected();
				imagePanel.repaint();
			} else if( e.getSource() == checkMeanShift) {
				meanShift = checkMeanShift.isSelected();
				createAlgorithm();
				reprocessImageOnly();
			} else if( e.getSource() == checkAnyGrid) {
				anyGrid = checkAnyGrid.isSelected();
				spinnerGridCols.setEnabled(!anyGrid);
				spinnerGridRows.setEnabled(!anyGrid);
				createAlgorithm();
				reprocessImageOnly();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == selectZoom ) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				imagePanel.setScale(zoom);
				return;
			}
			if( e.getSource() == spinnerCOrientationTol) {
				connOrientationTol = ((Number) spinnerCOrientationTol.getValue()).doubleValue();
			} else if( e.getSource() == spinnerCDirectionTol) {
				connDirectionTol = ((Number) spinnerCDirectionTol.getValue()).doubleValue();
			} else if( e.getSource() == spinnerCAmbiguous) {
				connAmbiguousTol = ((Number) spinnerCAmbiguous.getValue()).doubleValue();
			} else if( e.getSource() == spinnerDRefinedXThreshold) {
				detRefinedXThresh = ((Number) spinnerDRefinedXThreshold.getValue()).doubleValue();
			} else if( e.getSource() == spinnerDNonMaxThreshold) {
				detNonMaxThreshold = ((Number) spinnerDNonMaxThreshold.getValue()).intValue();
			} else if( e.getSource() == spinnerCEdgeThreshold) {
				connEdgeThreshold = ((Number) spinnerCEdgeThreshold.getValue()).doubleValue();
			} else if( e.getSource() == spinnerDRadius) {
				detRadius = ((Number) spinnerDRadius.getValue()).intValue();
			} else if( e.getSource() == spinnerDTop) {
				detPyramidTop = ((Number) spinnerDTop.getValue()).intValue();
			} else if( e.getSource() == spinnerGridRows ) {
				gridRows = ((Number)spinnerGridRows.getValue()).intValue();
			} else if( e.getSource() == spinnerGridCols ) {
				gridCols = ((Number)spinnerGridCols.getValue()).intValue();
			} else if( e.getSource() == spinnerCMaxDistance) {
				connMaxDistance = ((Number) spinnerCMaxDistance.getValue()).intValue();
			} else if( e.getSource() == spinnerDMinPyrLevel) {
				detMinPyrLevel = ((Number) spinnerDMinPyrLevel.getValue()).intValue();
				imagePanel.repaint();
				return;
			} else if( e.getSource() == sliderTranslucent ) {
				translucent = ((Number)sliderTranslucent.getValue()).intValue();
				imagePanel.repaint();
				return;
			}
			createAlgorithm();
			reprocessImageOnly();
		}

		@Override
		public void imageThresholdUpdated() {
			createAlgorithm();
			reprocessImageOnly();
		}
	}

	public static void main( String args[] ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Chessboard 1",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame06.jpg")));
		examples.add(new PathLabel("Chessboard 2 ",UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left03.jpg")));
		examples.add(new PathLabel("Chessboard 3 ",UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left06.jpg")));
		examples.add(new PathLabel("Fisheye 1",UtilIO.pathExample("calibration/fisheye/chessboard/1.jpg")));
		examples.add(new PathLabel("Fisheye 2",UtilIO.pathExample("calibration/fisheye/chessboard/20.jpg")));
		examples.add(new PathLabel("Fisheye 3",UtilIO.pathExample("calibration/fisheye/chessboard/21.jpg")));
		examples.add(new PathLabel("Chessboard Movie",UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		SwingUtilities.invokeLater(()->{
			DetectCalibrationChessboardXCornerApp app = new DetectCalibrationChessboardXCornerApp(examples);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Chessboard (X-Corner) Detector");
		});
	}
}
