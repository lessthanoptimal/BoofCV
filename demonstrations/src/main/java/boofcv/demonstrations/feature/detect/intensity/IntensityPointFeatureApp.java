/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.detect.intensity;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.filter.derivative.GradientThree;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I16;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the intensity of detected features inside an image
 *
 * @author Peter Abeles
 */
public class IntensityPointFeatureApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends DemonstrationBase
{
	// displays intensity image
	DisplayPanel imagePanel = new DisplayPanel();
	ControlPanel controlPanel;

	// intensity image is rendered here
	BufferedImage visualized;
	BufferedImage original;

	Class<D> derivType;
	// type of image the input image is
	Class<T> imageType;
	// computes image derivative
	AnyImageDerivative<T,D> deriv;

	// used to compute feature intensity
	GeneralFeatureDetector<T,D> detector;
	final Object lockAlgorithm = new Object();

	final Object lockCorners = new Object();
	QueueCorner minimums = new QueueCorner();
	QueueCorner maximums = new QueueCorner();

	String[] names = new String[]{"Harris","Shi Tomasi","FAST","KitRos","Median","Laplacian","Hessian Det"};

	public IntensityPointFeatureApp( List<String> examples , Class<T> imageType ) {
		super(true,true,examples,ImageType.single(imageType));
		this.imageType = imageType;

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(imageType);

		derivType = GImageDerivativeOps.getDerivativeType(imageType);
		deriv = new AnyImageDerivative<>(GradientThree.getKernelX(isInteger), imageType, derivType);

		controlPanel = new ControlPanel();
		controlPanel.comboAlgorithm.setSelectedIndex(0);
		add(BorderLayout.WEST,controlPanel);
		add(BorderLayout.CENTER,imagePanel);

		imagePanel.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {

				double curr =imagePanel.getScale();

				if( e.getWheelRotation() > 0 )
					curr *= 1.1;
				else
					curr /= 1.1;
				imagePanel.setScale(curr);
			}
		});
	}

	@Override
	protected void configureVideo(int which, SimpleImageSequence sequence) {
		sequence.setLoop(true);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		visualized = ConvertBufferedImage.checkDeclare(width,height, visualized,BufferedImage.TYPE_INT_RGB);
		imagePanel.setPreferredSize(new Dimension(width,height));

		double scale = BoofSwingUtil.selectZoomToShowAll(imagePanel,width,height);
		imagePanel.setScaleAndCenter(scale,width/2,height/2);
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {

		original = ConvertBufferedImage.checkCopy(buffered, original);

		T gray = (T) input;
		deriv.setInput(gray);

		D derivX = deriv.getDerivative(true);
		D derivY = deriv.getDerivative(false);
		D derivXX = deriv.getDerivative(true, true);
		D derivYY = deriv.getDerivative(false, false);
		D derivXY = deriv.getDerivative(true, false);

		GrayF32 featureImg;
		synchronized (lockAlgorithm) {
			detector.process(gray, derivX, derivY, derivXX, derivYY, derivXY);
			featureImg = detector.getIntensity();
			VisualizeImageData.colorizeSign(featureImg, visualized, ImageStatistics.maxAbs(featureImg));

			synchronized (lockCorners) {
				minimums.reset();
				minimums.addAll(detector.getMinimums());

				maximums.reset();
				maximums.addAll(detector.getMaximums());
			}
		}


		SwingUtilities.invokeLater(() -> {
			changeViewImage();
			imagePanel.repaint();
		});
	}

	private void changeViewImage() {
		if( controlPanel.view == 0 || controlPanel.view == 2 ) {
			imagePanel.setBufferedImageNoChange(visualized);
		} else if( controlPanel.view == 1 ) {
			imagePanel.setBufferedImageNoChange(original);
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

			if( controlPanel.showPeaks ) {
				synchronized (lockCorners) {
					g2.setStroke(new BasicStroke(3));
					g2.setColor(Color.ORANGE);
					for (int i = 0; i < minimums.size; i++) {
						Point2D_I16 c = minimums.get(i);
						VisualizeFeatures.drawCircle(g2, (c.x + 0.5) * scale, (c.y + 0.5) * scale, 5, circle);
					}
					g2.setColor(Color.BLUE);
					for (int i = 0; i < maximums.size; i++) {
						Point2D_I16 c = maximums.get(i);
						VisualizeFeatures.drawCircle(g2, (c.x + 0.5) * scale, (c.y + 0.5) * scale, 5, circle);
					}
				}
			}
		}
	}

	private void handleAlgorithmChanged() {
		ConfigGeneralDetector config = new ConfigGeneralDetector();
		config.radius = controlPanel.radius;
		config.maxFeatures = 200;

		synchronized (lockAlgorithm) {
			switch (controlPanel.selected) {
				case "Laplacian":
					config.detectMinimums = true;
					detector = FactoryDetectPoint.createHessian(HessianBlobIntensity.Type.TRACE,config,derivType);
					break;
				case "Hessian Det":
					detector = FactoryDetectPoint.createHessian(HessianBlobIntensity.Type.DETERMINANT,config,derivType);
					break;

				case "Harris":
					detector = FactoryDetectPoint.createHarris(config,controlPanel.weighted,derivType);
					break;

				case "Shi Tomasi":
					detector = FactoryDetectPoint.createShiTomasi(config,controlPanel.weighted,derivType);
					break;
				case "FAST":
					detector = FactoryDetectPoint.createFast(null,config,imageType);
					break;
				case "KitRos":
					detector = FactoryDetectPoint.createKitRos(config,derivType);
					break;
				case "Median":
					detector = FactoryDetectPoint.createMedian(config,imageType);
					break;

				default:
					throw new RuntimeException("Unknown exception");
			}
		}

		reprocessImageOnly();
	}

	class ControlPanel extends StandardAlgConfigPanel implements ActionListener , ChangeListener {
		JComboBox<String> comboView;
		JComboBox<String> comboAlgorithm;
		JSpinner spinnerRadius;
		JCheckBox checkWeighted;
		JCheckBox checkShowLocalMax;

		String selected = null;
		int radius = 2;
		boolean weighted=false;
		boolean showPeaks =false;
		int view = 0;

		public ControlPanel() {
			comboAlgorithm = new JComboBox<>();

			comboView = combo(view,"Intensity","Image","Both");
			spinnerRadius = spinner(radius, 1, 100, 1);
			checkWeighted = checkbox("weighted", weighted);
			checkShowLocalMax = checkbox("Show Maximums", showPeaks);

			for (String name : names) {
				comboAlgorithm.addItem(name);
			}
			comboAlgorithm.addActionListener(this);
			comboView.setPreferredSize(comboAlgorithm.getPreferredSize());
			comboView.setMaximumSize(comboAlgorithm.getPreferredSize());
			comboAlgorithm.setMaximumSize(comboAlgorithm.getPreferredSize());

			addLabeled(comboView,"View");
			addLabeled(comboAlgorithm,"Detector");
			addAlignLeft(checkShowLocalMax);
			addAlignLeft(checkWeighted);
			addLabeled(spinnerRadius,"Radius");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == comboAlgorithm ) {
				selected = (String)comboAlgorithm.getSelectedItem();
				updateEnabledControls();
				handleAlgorithmChanged();
			} else if( e.getSource() == checkWeighted ) {
				weighted = checkWeighted.isSelected();
				handleAlgorithmChanged();
			} else if( e.getSource() == comboView ) {
				view = comboView.getSelectedIndex();
				changeViewImage();
				imagePanel.repaint();
			} else if( e.getSource() == checkShowLocalMax ) {
				showPeaks = checkShowLocalMax.isSelected();
				imagePanel.repaint();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerRadius ) {
				radius = ((Number)spinnerRadius.getValue()).intValue();
				handleAlgorithmChanged();
			}
		}

		private void updateEnabledControls() {
			boolean activeRadius = false;
			boolean activeWeighted = false;

			switch( selected ) {
				case "Harris":
				case "Shi Tomasi":
					activeRadius = true;
					activeWeighted = true;
					break;

				case "Median":
					activeRadius = true;
					break;
			}

			spinnerRadius.setEnabled(activeRadius);
			checkWeighted.setEnabled(activeWeighted);
		}
	}

	public static void main( String args[] ) {
		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Chessboard",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame06.jpg")));
		examples.add(new PathLabel("Square Grid",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Square/frame06.jpg")));
		examples.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("sunflowers",UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("beach",UtilIO.pathExample("scale/beach02.jpg")));
		examples.add(new PathLabel("Chessboard Movie",UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		IntensityPointFeatureApp<GrayU8, GrayS16> app = new IntensityPointFeatureApp(examples,GrayU8.class);

		app.openExample(examples.get(0));
		app.waitUntilInputSizeIsKnown();
		app.display("Feature Intensity");
	}
}
