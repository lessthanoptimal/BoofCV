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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
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
public class DetectChessboardVisualizeApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends DemonstrationBase
{
	// displays intensity image
	DisplayPanel imagePanel = new DisplayPanel();
	ControlPanel controlPanel;

	// intensity image is rendered here
	BufferedImage visualized;
	BufferedImage binary;
	BufferedImage original;

	Class<D> derivType;
	// type of image the input image is
	Class<T> imageType;
	// computes image derivative

	ImageGradient<T,D> gradient;
	D derivX,derivY;

	// used to compute feature intensity
	DetectChessboardCorners<T,D> detector;
	final Object lockAlgorithm = new Object();

	final Object lockCorners = new Object();
	FastQueue<Point2D_F64> foundCorners = new FastQueue<>(Point2D_F64.class,true);

	public DetectChessboardVisualizeApp(List<String> examples , Class<T> imageType ) {
		super(true,true,examples,ImageType.single(imageType));
		this.imageType = imageType;

		derivType = GImageDerivativeOps.getDerivativeType(imageType);

		gradient = FactoryDerivative.sobel(imageType,derivType);
		derivX = GeneralizedImageOps.createSingleBand(derivType,1,1);
		derivY = GeneralizedImageOps.createSingleBand(derivType,1,1);

		detector = new DetectChessboardCorners<>(imageType,derivType);

		controlPanel = new ControlPanel();
		add(BorderLayout.WEST,controlPanel);
		add(BorderLayout.CENTER,imagePanel);

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

	@Override
	protected void configureVideo(int which, SimpleImageSequence sequence) {
		sequence.setLoop(true);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		visualized = ConvertBufferedImage.checkDeclare(width,height, visualized,BufferedImage.TYPE_INT_RGB);

		SwingUtilities.invokeLater(()->{
			imagePanel.setPreferredSize(new Dimension(width,height));

			double scale = BoofSwingUtil.selectZoomToShowAll(imagePanel,width,height);
//			controlPanel.zoom = scale; // prevent it from broadcasting an event
			controlPanel.setZoom(scale);
			imagePanel.setScaleAndCenter(scale,width/2,height/2);
			controlPanel.setImageSize(width,height);
		});
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {

		original = ConvertBufferedImage.checkCopy(buffered, original);

		T gray = (T) input;
		gradient.process(gray,derivX,derivY);

		GrayF32 featureImg;
		synchronized (lockAlgorithm) {
			detector.process(gray,derivX,derivY);
			featureImg = detector.getIntensity();
			VisualizeImageData.colorizeSign(featureImg, visualized, ImageStatistics.maxAbs(featureImg));

			binary=VisualizeBinaryData.renderBinary(detector.getBinary(),false,binary);

			synchronized (lockCorners) {
				FastQueue<Point2D_F64> orig = detector.getCorners();
				foundCorners.reset();
				for (int i = 0; i < orig.size; i++) {
					foundCorners.grow().set( orig.get(i) );
				}
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
				synchronized (lockCorners) {
					g2.setStroke(new BasicStroke(3));
					g2.setColor(Color.ORANGE);
					for (int i = 0; i < foundCorners.size; i++) {
						Point2D_F64 c = foundCorners.get(i);
						VisualizeFeatures.drawCircle(g2, (c.x + 0.5) * scale, (c.y + 0.5) * scale, 5, circle);
					}
				}
			}
		}
	}

	class ControlPanel extends DetectBlackShapePanel implements ActionListener , ChangeListener {
		JComboBox<String> comboView;
		JSpinner spinnerRadius;
		JCheckBox checkShowCorners;
		JSpinner spinnerMaxFeatures;

		int radius = 2;
		boolean showCorners =true;
		int view = 1;
		int maxFeatures = 200;

		public ControlPanel() {

			selectZoom = spinner(1.0,MIN_ZOOM,MAX_ZOOM,1.0);
			comboView = combo(view,"Intensity","Image","Both","Binary");
			spinnerRadius = spinner(radius, 1, 100, 1);
			checkShowCorners = checkbox("Show Corners", showCorners);
			spinnerMaxFeatures = spinner(maxFeatures,1,100_000,10);

			addLabeled(imageSizeLabel,"Image Size");
			addLabeled(comboView,"View");
			addLabeled(selectZoom,"Zoom");
			addAlignLeft(checkShowCorners);
			addLabeled(spinnerRadius,"Radius");
			addLabeled(spinnerMaxFeatures,"Max Features");
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
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == selectZoom ) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				imagePanel.setScale(zoom);
			}
		}
	}

	// consider finding local maximums? how to remove all the false positives
	//    blobs are expected to be a specific size too


	// TODO why are there large blocks in binary image?

	// TODO add binary controls
	// TODO visualize contours

	public static void main( String args[] ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Square Grid",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Square/frame06.jpg")));
		examples.add(new PathLabel("Chessboard",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame06.jpg")));
		examples.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("sunflowers",UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("beach",UtilIO.pathExample("scale/beach02.jpg")));
		examples.add(new PathLabel("Chessboard Movie",UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		SwingUtilities.invokeLater(()->{
			DetectChessboardVisualizeApp<GrayU8, GrayS16> app = new DetectChessboardVisualizeApp(examples,GrayU8.class);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Feature Intensity");
		});
	}
}
