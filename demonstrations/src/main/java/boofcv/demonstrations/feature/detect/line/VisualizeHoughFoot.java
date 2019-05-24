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

package boofcv.demonstrations.feature.detect.line;


import boofcv.abst.feature.detect.line.DetectEdgeLinesToLines;
import boofcv.abst.feature.detect.line.DetectLineHoughFoot;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanelZoom;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the Hough foot of norm transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public class VisualizeHoughFoot<I extends ImageGray<I>, D extends ImageGray<D>>
		extends DemonstrationBase
{

	Class<I> imageType;
	Class<D> derivType;

	I blur;

	Visualization imagePanel = new Visualization();
	ControlPanel controlPanel;

	//--------------------------
	final Object lockAlg = new Object();
	DetectLineHoughFoot<D> alg; // the algorithm being investigated
	DetectEdgeLinesToLines<I,D> lineDetector; // use a high level line detector since it already has boilerplate code in it
	//--------------------------

	ConfigHoughFoot config = new ConfigHoughFoot(6, 6, 5, 15, 10);
	int blurRadius = 2;
	int view = 0;
	boolean logIntensity = false;

	GrayF32 transformLog = new GrayF32(1,1);

	BufferedImage renderedTran;
	BufferedImage renderedBinary;

	public VisualizeHoughFoot(List<PathLabel> examples, Class<I> imageType) {
		super(true,true,examples, ImageType.single(imageType));
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		controlPanel = new ControlPanel();
		controlPanel.setListener((zoom)-> imagePanel.setScale(zoom));
		imagePanel.addMouseWheelListener(controlPanel);
		imagePanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Point2D_F64 p = imagePanel.pixelToPoint(e.getX(), e.getY());
				controlPanel.setCursor(p.x,p.y);

				if( SwingUtilities.isLeftMouseButton(e)) {
					imagePanel.centerView(p.x,p.y);

					if( view == 0 ) {
						int s = imagePanel.findLine(p.x, p.y, 5.0 / imagePanel.getScale());
						if (s != -1) {
							imagePanel.setSelected(s);
							imagePanel.repaint();
						}
					}
				}
			}
		});

		createAlg();
		blur = GeneralizedImageOps.createSingleBand(imageType,1,1);

		add(BorderLayout.WEST,controlPanel);
		add(BorderLayout.CENTER,imagePanel);
	}

	private void createAlg() {
		synchronized (lockAlg) {
			alg = FactoryDetectLineAlgs.houghFoot(config, derivType);
			lineDetector = new DetectEdgeLinesToLines<>(alg, imageType, derivType);
		}
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		blur.reshape(width,height);

		synchronized (lockAlg) {
			alg.setInputSize(width, height);
		}
		renderedTran = ConvertBufferedImage.checkDeclare(width,height,renderedTran,BufferedImage.TYPE_INT_RGB);
		renderedBinary = ConvertBufferedImage.checkDeclare(width,height,renderedBinary,BufferedImage.TYPE_INT_RGB);

		BoofSwingUtil.invokeNowOrLater(()-> {
			imagePanel.setPreferredSize(new Dimension(width, height));
			controlPanel.setImageSize(width,height);
		});
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase _input)
	{
		I input = (I)_input;

		long time0 = System.nanoTime();
		if( blurRadius > 0 ) {
			GBlurImageOps.gaussian(input, blur, -1, blurRadius, null);
		} else {
			blur.setTo(input);
		}
		List<LineParametric2D_F32> lines;
		synchronized (lockAlg) {
			lines = lineDetector.detect(blur);
		}

		imagePanel.setLines(lines,input.width,input.height);
		long time1 = System.nanoTime();

		imagePanel.input = buffered;

		if( logIntensity ) {
			PixelMath.log(alg.getTransform().getTransform(),transformLog);
			VisualizeImageData.grayMagnitude(transformLog, renderedTran,-1);
		} else {
			VisualizeImageData.grayMagnitude(alg.getTransform().getTransform(), renderedTran,-1);
		}

		VisualizeBinaryData.renderBinary(alg.getBinary(), false, renderedBinary);

		BoofSwingUtil.invokeNowOrLater(()->{
			imagePanel.handleViewChange();
			controlPanel.setProcessingTimeMS((time1-time0)*1e-6);
			imagePanel.repaint();
		});
	}

	private class Visualization extends ImageLinePanelZoom {
		BufferedImage input;
		Ellipse2D.Double c = new Ellipse2D.Double();

		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			if( view == 0 ) {
				super.paintInPanel(tran,g2);
			} else if( view == 2 ) {
				paintLinesInTransform(g2);
			}
		}

		public void handleViewChange() {
			switch(view) {
				case 0:setBufferedImage(input);break;
				case 1:setBufferedImage(renderedBinary);break;
				case 2:setBufferedImage(renderedTran);break;
			}
		}

		private void paintLinesInTransform(Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int r = 6;
			g2.setStroke(new BasicStroke(2));
			int selected = imagePanel.getSelected();
			synchronized (lockAlg) {
				List<LineParametric2D_F32>  found = alg.getFoundLines();
				for (int i = 0; found != null && i < found.size(); i++) {
					LineParametric2D_F32 l = alg.getFoundLines().get(i);

					c.setFrame((l.p.x + 0.5) * scale - r, (l.p.y + 0.5) * scale - r, 2 * r, 2 * r);

					if (i == selected) {
						g2.setColor(Color.GREEN);
					} else {
						g2.setColor(Color.RED);
					}
					g2.draw(c);
				}
			}
		}
	}

	private class ControlPanel extends ViewedImageInfoPanel
			implements ChangeListener, ActionListener
	{
		JComboBox<String> comboView = combo(0,"Lines","Edges","Parameter Space");
		JCheckBox checkLog = checkbox("Log Intensity",logIntensity);
		JSpinner spinnerMaxLines = spinner(config.maxLines,1,200,1);
		JSpinner spinnerBlur = spinner(blurRadius,0,20,1);
		JSpinner spinnerMinCount = spinner(config.minCounts,1,100,1);
		JSpinner spinnerLocalMax = spinner(config.localMaxRadius,1,100,2);
		JSpinner spinnerEdgeThresh = spinner(config.thresholdEdge,1,100,2);
		JSpinner spinnerRefRadius = spinner(config.refineRadius,0,20,1);

		public ControlPanel() {
			super(BoofSwingUtil.MIN_ZOOM,BoofSwingUtil.MAX_ZOOM,0.5,false);
			add(comboView);
			addAlignLeft(checkLog);
			addSeparator(120);
			addLabeled(spinnerMaxLines,"Max Lines");
			addLabeled(spinnerBlur,"Blur Radius");
			addLabeled(spinnerMinCount,"Min. Count");
			addLabeled(spinnerLocalMax,"Local Max");
			addLabeled(spinnerEdgeThresh,"Edge Thresh");
			addLabeled(spinnerRefRadius,"Refine Radius");
			addVerticalGlue();
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == spinnerMaxLines ) {
				config.maxLines = (Integer) spinnerMaxLines.getValue();
				createAlg();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerBlur ) {
				blurRadius = (Integer) spinnerBlur.getValue();
				createAlg();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerMinCount ) {
				config.minCounts = (Integer) spinnerMinCount.getValue();
				createAlg();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerLocalMax ) {
				config.localMaxRadius = (Integer) spinnerLocalMax.getValue();
				createAlg();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerEdgeThresh ) {
				config.thresholdEdge = ((Number) spinnerEdgeThresh.getValue()).floatValue();
				createAlg();
				reprocessImageOnly();
			} else if( e.getSource() == spinnerRefRadius ) {
				config.refineRadius = (Integer) spinnerRefRadius.getValue();
				createAlg();
				reprocessImageOnly();
			} else {
				super.stateChanged(e);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == comboView ) {
				view = comboView.getSelectedIndex();
				imagePanel.handleViewChange();
				imagePanel.repaint();
			} else if( e.getSource() == checkLog ) {
				logIntensity = checkLog.isSelected();
				reprocessImageOnly();
			}
		}
	}

	public static void main( String args[] ) {
		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Simple Objects",UtilIO.pathExample("simple_objects.jpg")));
		examples.add(new PathLabel("Indoors",UtilIO.pathExample("lines_indoors.jpg")));
		examples.add(new PathLabel("Outdoors", UtilIO.pathExample("outdoors01.jpg")));
		examples.add(new PathLabel("Drawn Lines",UtilIO.pathExample("shapes/black_lines_01.jpg")));
		examples.add(new PathLabel("Indoors Video",UtilIO.pathExample("lines_indoors.mjpeg")));

		SwingUtilities.invokeLater(()->{
			VisualizeHoughFoot<GrayF32, GrayF32> app = new VisualizeHoughFoot(examples,GrayF32.class);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Hough Foot");
		});
	}
}
