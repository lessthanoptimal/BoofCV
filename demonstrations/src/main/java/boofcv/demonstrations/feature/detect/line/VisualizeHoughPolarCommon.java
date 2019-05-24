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


import boofcv.abst.feature.detect.line.DetectLineHoughPolarBinary;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.gui.feature.ImageLinePanelZoom;
import boofcv.io.PathLabel;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.metric.UtilAngle;
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
import java.util.List;

/**
 * Computes the Hough Polar transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public abstract class VisualizeHoughPolarCommon
	extends DemonstrationBase {

	Visualization imagePanel = new Visualization();
	ControlPanel controlPanel;

	//--------------------------
	final Object lockAlg = new Object();
	DetectLineHoughPolarBinary alg;
	//--------------------------
	ConfigHoughPolar config = new ConfigHoughPolar(5, 10, 2, Math.PI / 180, 25, 5);
	int blurRadius = 2;
	int view = 0;
	boolean logIntensity = false;

	GrayF32 transformLog = new GrayF32(1, 1);

	BufferedImage renderedTran;
	BufferedImage renderedBinary;

	public VisualizeHoughPolarCommon(List<PathLabel> examples , ImageType imageType ) {
		super(true, true, examples, imageType);

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

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, imagePanel);
	}

	protected abstract void createAlg();

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
				List<LineParametric2D_F32> lines = alg.getFoundLines();
				for (int i = 0; lines != null && i < lines.size(); i++) {
					LineParametric2D_F32 l = lines.get(i);
					alg.getTransform().lineToCoordinate(l, location);

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
			implements ChangeListener, ActionListener {
		JComboBox<String> comboView = combo(0, "Lines", "Edges", "Parameter Space");
		JCheckBox checkLog = checkbox("Log Intensity", logIntensity);
		JSpinner spinnerResRange = spinner(config.resolutionRange, 1, 50, 1);
		JSpinner spinnerResAngle = spinner(UtilAngle.degree(config.resolutionAngle), 0.1, 20, 1);
		JSpinner spinnerMaxLines = spinner(config.maxLines, 1, 200, 1);
		JSpinner spinnerBlur = spinner(blurRadius, 0, 20, 1);
		JSpinner spinnerMinCount = spinner(config.minCounts, 1, 100, 1);
		JSpinner spinnerLocalMax = spinner(config.localMaxRadius, 1, 100, 2);
		JSpinner spinnerEdgeThresh = spinner(config.thresholdEdge, 1, 100, 2);

		public ControlPanel() {
			super(BoofSwingUtil.MIN_ZOOM, BoofSwingUtil.MAX_ZOOM, 0.5, false);
			add(comboView);
			addAlignLeft(checkLog);
			addSeparator(120);
			addLabeled(spinnerResRange, "Res. Range");
			addLabeled(spinnerResAngle, "Res. Angle");
			addLabeled(spinnerMaxLines, "Max Lines");
			addLabeled(spinnerBlur, "Blur Radius");
			addLabeled(spinnerMinCount, "Min. Count");
			addLabeled(spinnerLocalMax, "Local Max");
			addLabeled(spinnerEdgeThresh, "Edge Thresh");
			addVerticalGlue();
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() == spinnerResRange) {
				config.resolutionRange = (Double) spinnerResRange.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerResAngle) {
				config.resolutionAngle = UtilAngle.radian((Double) spinnerResAngle.getValue());
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerMaxLines) {
				config.maxLines = (Integer) spinnerMaxLines.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerBlur) {
				blurRadius = (Integer) spinnerBlur.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerMinCount) {
				config.minCounts = (Integer) spinnerMinCount.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerLocalMax) {
				config.localMaxRadius = (Integer) spinnerLocalMax.getValue();
				createAlg();
				reprocessImageOnly();
			} else if (e.getSource() == spinnerEdgeThresh) {
				config.thresholdEdge = ((Number) spinnerEdgeThresh.getValue()).floatValue();
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
	}
}
