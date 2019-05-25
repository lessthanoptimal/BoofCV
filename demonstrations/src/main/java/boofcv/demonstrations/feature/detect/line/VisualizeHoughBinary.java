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


import boofcv.abst.feature.detect.line.HoughBinary_to_DetectLine;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.feature.detect.line.ConfigHoughBinary;
import boofcv.factory.feature.detect.line.ConfigParamPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.JConfigLength;
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanelZoom;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
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
 * Computes the Hough Polar transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public class VisualizeHoughBinary
	extends DemonstrationBase {

	Visualization imagePanel = new Visualization();
	ControlPanel controlPanel;

	//--------------------------
	final Object lockAlg = new Object();
	HoughBinary_to_DetectLine<GrayU8> alg;
	//--------------------------
	ConfigHoughBinary configHough = new ConfigHoughBinary();
	ConfigParamPolar configPolar = new ConfigParamPolar();
	int blurRadius = 2;
	int view = 0;
	boolean logIntensity = false;

	GrayF32 transformLog = new GrayF32(1, 1);

	BufferedImage renderedTran;
	BufferedImage renderedBinary;

	public VisualizeHoughBinary(List<PathLabel> examples ) {
		super(true, true, examples, ImageType.single(GrayU8.class));

		controlPanel = new ControlPanel();
		controlPanel.setListener((zoom) -> imagePanel.setScale(zoom));
		imagePanel.addMouseWheelListener(controlPanel);
		imagePanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Point2D_F64 p = imagePanel.pixelToPoint(e.getX(), e.getY());
				controlPanel.setCursor(p.x, p.y);

				if (SwingUtilities.isLeftMouseButton(e)) {
					imagePanel.centerView(p.x, p.y);

					// let the user select a line
					if (view == 0) {
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

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, imagePanel);
	}

	protected void createAlg() {
		synchronized (lockAlg) {
			alg = (HoughBinary_to_DetectLine<GrayU8>)FactoryDetectLine.houghLinePolar(configHough,configPolar,null, GrayU8.class);
		}
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		synchronized (lockAlg) {
			int tranWidth = alg.getHough().getTransform().width;
			int tranHeight = alg.getHough().getTransform().height;
			renderedTran = ConvertBufferedImage.checkDeclare(tranWidth, tranHeight, renderedTran, BufferedImage.TYPE_INT_RGB);
			renderedBinary = ConvertBufferedImage.checkDeclare(width, height, renderedBinary, BufferedImage.TYPE_INT_RGB);
		}

		BoofSwingUtil.invokeNowOrLater(()-> {
			imagePanel.setPreferredSize(new Dimension(width, height));
			controlPanel.setImageSize(width,height);
		});
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {
		long time0 = System.nanoTime();
		long time1;
		synchronized (lockAlg) {
			List<LineParametric2D_F32> lines = alg.detect((GrayU8)input);
			imagePanel.setLines(lines,input.width,input.height);
			time1 = System.nanoTime();

			imagePanel.input = buffered;

			if (logIntensity) {
				PixelMath.log(alg.getHough().getTransform(), transformLog);
				renderedTran = VisualizeImageData.grayMagnitude(transformLog, renderedTran, -1);
			} else {
				renderedTran = VisualizeImageData.grayMagnitude(alg.getHough().getTransform(), renderedTran, -1);
			}

			VisualizeBinaryData.renderBinary(alg.getBinary(), false, renderedBinary);
		}

		BoofSwingUtil.invokeNowOrLater(()->{
			imagePanel.handleViewChange();
			controlPanel.setProcessingTimeMS((time1-time0)*1e-6);
			imagePanel.repaint();
		});
	}

	protected class Visualization extends ImageLinePanelZoom {
		BufferedImage input;
		Ellipse2D.Double c = new Ellipse2D.Double();

		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			if (view == 0) {
				super.paintInPanel(tran, g2);
			} else if (view == 2) {
				paintLinesInTransform(g2);
			}
		}

		private void paintLinesInTransform(Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int r = 6;
			g2.setStroke(new BasicStroke(2));
			int selected = imagePanel.getSelected();
			Point2D_F64 location = new Point2D_F64();
			synchronized (lockAlg) {
				List<LineParametric2D_F32> lines = alg.getHough().getLinesMerged();
				for (int i = 0; lines != null && i < lines.size(); i++) {
					LineParametric2D_F32 l = lines.get(i);
					alg.getHough().getParameters().lineToCoordinate(l, location);

					// +0.5 to put in the center
					c.setFrame((location.x + 0.5) * scale - r, (location.y + 0.5) * scale - r, 2 * r, 2 * r);
//			System.out.println(x+" "+y+"  "+renderedTran.getWidth()+" "+renderedTran.getHeight());

					if (i == selected) {
						g2.setColor(Color.GREEN);
					} else {
						g2.setColor(Color.RED);
					}
					g2.draw(c);
				}
			}
		}

		public void handleViewChange() {
			switch (view) {
				case 0:
					setBufferedImage(input);
					break;
				case 1:
					setBufferedImage(renderedBinary);
					break;
				case 2:
					setBufferedImage(renderedTran);
					break;
			}
		}
	}

	protected class ControlPanel extends ViewedImageInfoPanel
			implements ChangeListener, ActionListener , JConfigLength.Listener
	{
		JComboBox<String> comboView = combo(0, "Lines", "Edges", "Parameter Space");
		JCheckBox checkLog = checkbox("Log Intensity", logIntensity);
		JSpinner spinnerResRange = spinner(configPolar.resolutionRange, 0.1, 50, 0.5);
		JSpinner spinnerBinsAngle = spinner(configPolar.numBinsAngle, 10, 1000, 1);
		JSpinner spinnerMaxLines = spinner(configHough.maxLines, 0, 200, 1);
		JSpinner spinnerBlur = spinner(blurRadius, 0, 20, 1);
		JConfigLength lengthCounts = new JConfigLength(this,false);
		JSpinner spinnerLocalMax = spinner(configHough.localMaxRadius, 1, 100, 2);

		public ControlPanel() {
			super(BoofSwingUtil.MIN_ZOOM, BoofSwingUtil.MAX_ZOOM, 0.5, false);

			lengthCounts.setValue(configHough.minCounts);
			lengthCounts.setMaximumSize(lengthCounts.getPreferredSize());

			add(comboView);
			addAlignLeft(checkLog);
			addSeparator(120);
			addLabeled(spinnerResRange, "Res. Range");
			addLabeled(spinnerBinsAngle, "Bins Angle");
			addLabeled(spinnerMaxLines, "Max Lines");
			addLabeled(spinnerBlur, "Blur Radius");
			addLabeled(lengthCounts, "Min. Count");
			addLabeled(spinnerLocalMax, "Local Max");
			addVerticalGlue();
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() == spinnerResRange) {
				configPolar.resolutionRange = (Double) spinnerResRange.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerBinsAngle) {
				configPolar.numBinsAngle = (Integer)spinnerBinsAngle.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerMaxLines) {
				configHough.maxLines = (Integer) spinnerMaxLines.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerBlur) {
				blurRadius = (Integer) spinnerBlur.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerLocalMax) {
				configHough.localMaxRadius = (Integer) spinnerLocalMax.getValue();
				createAlg();
				reprocessImageOnly();
			} else {
				super.stateChanged(e);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == comboView) {
				view = comboView.getSelectedIndex();
				imagePanel.handleViewChange();
				imagePanel.repaint();
			} else if (e.getSource() == checkLog) {
				logIntensity = checkLog.isSelected();
				reprocessImageOnly();
			}
		}

		@Override
		public void changeConfigLength(JConfigLength source, double fraction, double length) {
			configHough.minCounts = new ConfigLength(length,fraction);
			createAlg();
			reprocessImageOnly();
		}
	}

	public static void main(String[] args) {
		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Drawn Lines",UtilIO.pathExample("shapes/black_lines_01.jpg")));

		SwingUtilities.invokeLater(()->{
			VisualizeHoughBinary app = new VisualizeHoughBinary(examples);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Hough Binary");
		});
	}
}
