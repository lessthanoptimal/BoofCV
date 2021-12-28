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

package boofcv.demonstrations.feature.detect.line;

import boofcv.abst.feature.detect.line.HoughBinary_to_DetectLine;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughBinary;
import boofcv.factory.feature.detect.line.ConfigHoughGradient;
import boofcv.factory.feature.detect.line.ConfigParamPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.ViewedImageInfoPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.controls.JConfigLength;
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
import java.awt.*;
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
@SuppressWarnings({"NullAway.Init"})
public class VisualizeHoughBinary<I extends ImageGray<I>, D extends ImageGray<D>>
		extends DemonstrationBase {

	Visualization imagePanel = new Visualization();
	ControlPanel controlPanel;

	//--------------------------
	final Object lockAlg = new Object();
	HoughBinary_to_DetectLine<I, D> lineDetector;
	//--------------------------
	ConfigHoughBinary configHough = new ConfigHoughBinary();
	ConfigParamPolar configPolar = new ConfigParamPolar();
	int blurRadius = 4;
	int view = 0;
	boolean logIntensity = false;
	boolean mergeSimilar = true;
	boolean showLines = true;

	// is this the first time the image has been opened?
	boolean firstOpenImage = true;

	GrayF32 transformLog = new GrayF32(1, 1);

	// workspace for Gaussian blur
	I blur;

	BufferedImage renderedTran;
	BufferedImage renderedBinary;

	public VisualizeHoughBinary( List<PathLabel> examples, Class<I> imageType ) {
		super(true, true, examples, ImageType.single(imageType));

		blur = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		controlPanel = new ControlPanel();
		controlPanel.setListener(( zoom ) -> imagePanel.setScale(zoom));
		imagePanel.addMouseWheelListener(controlPanel);
		imagePanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				Point2D_F64 p = imagePanel.pixelToPoint(e.getX(), e.getY());
				controlPanel.setCursor(p.x, p.y);

				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}

				// let the user select a line
				float tolerance = 10.0f/(float)imagePanel.getScale();
				int s = -1;
				if (view == 0) {
					s = imagePanel.findLine(p.x, p.y, tolerance);
				} else if (view == 2) {
					s = findLineInTransformed((int)p.x, (int)p.y, tolerance);
				}
				if (s != -1) {
					// deselect if clicked twice
					if (imagePanel.getSelected() == s)
						s = -1;
				} else {
					imagePanel.centerView(p.x, p.y);
				}
				imagePanel.setSelected(s);
				imagePanel.repaint();
			}
		});
		imagePanel.setListener(scale -> controlPanel.setScale(scale));

		createAlg();

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, imagePanel);
	}

	protected void createAlg() {
		if (mergeSimilar) {
			ConfigHoughGradient tmp = new ConfigHoughGradient();
			configHough.mergeAngle = tmp.mergeAngle;
			configHough.mergeDistance = tmp.mergeDistance;
		} else {
			// disable merging similar lines
			configHough.mergeAngle = -1;
			configHough.mergeDistance = -1;
		}
		synchronized (lockAlg) {
			lineDetector = (HoughBinary_to_DetectLine<I, D>)
					FactoryDetectLine.houghLinePolar(configHough, configPolar, getImageType(0).getImageClass());
		}
	}

	private int findLineInTransformed( int x, int y, double tolerance ) {
		Point2D_F64 location = new Point2D_F64();
		synchronized (lockAlg) {
			double bestDistance = tolerance;
			int bestIndex = -1;
			List<LineParametric2D_F32> lines = lineDetector.getHough().getLinesMerged();
			for (int i = 0; lines != null && i < lines.size(); i++) {
				LineParametric2D_F32 l = lines.get(i);
				lineDetector.getHough().getParameters().lineToCoordinate(l, location);
				if (location.distance2(x, y) < bestDistance) {
					bestDistance = location.distance2(x, y);
					bestIndex = i;
				}
			}
			return bestIndex;
		}
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		synchronized (lockAlg) {
			int tranWidth = lineDetector.getHough().getTransform().width;
			int tranHeight = lineDetector.getHough().getTransform().height;
			renderedTran = ConvertBufferedImage.checkDeclare(tranWidth, tranHeight, renderedTran, BufferedImage.TYPE_INT_RGB);
			renderedBinary = ConvertBufferedImage.checkDeclare(width, height, renderedBinary, BufferedImage.TYPE_INT_RGB);
		}

		BoofSwingUtil.invokeNowOrLater(() -> {
			imagePanel.setPreferredSize(new Dimension(width, height));
			controlPanel.setImageSize(width, height);
		});

		// let it know a new image has been opened so that the view can be reset later
		firstOpenImage = true;
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		long time0 = System.nanoTime();
		long time1;
		synchronized (lockAlg) {
			if (blurRadius > 0) {
				GBlurImageOps.gaussian((I)input, blur, -1, blurRadius, null);
			} else {
				blur.setTo((I)input);
			}

			List<LineParametric2D_F32> lines = lineDetector.detect(blur);
			imagePanel.setLines(lines, input.width, input.height);
			time1 = System.nanoTime();

			imagePanel.input = buffered;

			if (logIntensity) {
				PixelMath.log(lineDetector.getHough().getTransform(), 1.0f, transformLog);
				renderedTran = VisualizeImageData.grayMagnitude(transformLog, renderedTran, -1);
			} else {
				renderedTran = VisualizeImageData.grayMagnitude(lineDetector.getHough().getTransform(), renderedTran, -1);
			}

			VisualizeBinaryData.renderBinary(lineDetector.getBinary(), false, renderedBinary);
		}

		BoofSwingUtil.invokeNowOrLater(() -> {
			imagePanel.handleViewChange(view);
			if (firstOpenImage) {
				firstOpenImage = false;
				imagePanel.autoScaleCenterOnSetImage = true;
			}
			controlPanel.setProcessingTimeMS((time1 - time0)*1e-6);
			imagePanel.repaint();
		});
	}

	@SuppressWarnings({"NullAway.Init"})
	protected class Visualization extends ImageLinePanelZoom {
		BufferedImage input;
		Ellipse2D.Double c = new Ellipse2D.Double();

		@Override
		protected synchronized void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			if (!showLines)
				return;
			if (view == 0) {
				super.paintInPanel(tran, g2);
			} else if (view == 2) {
				paintLinesInTransform(g2);
			}
		}

		private void paintLinesInTransform( Graphics2D g2 ) {
			BoofSwingUtil.antialiasing(g2);

			int r = 6;
			g2.setStroke(new BasicStroke(2));
			int selected = imagePanel.getSelected();
			Point2D_F64 location = new Point2D_F64();
			synchronized (lockAlg) {
				List<LineParametric2D_F32> lines = lineDetector.getHough().getLinesMerged();
				for (int i = 0; lines != null && i < lines.size(); i++) {
					LineParametric2D_F32 l = lines.get(i);
					lineDetector.getHough().getParameters().lineToCoordinate(l, location);

					// +0.5 to put in the center
					c.setFrame((location.x + 0.5)*scale - r, (location.y + 0.5)*scale - r, 2*r, 2*r);
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

		public void handleViewChange( int newView ) {
			boolean centerAndRescale = false;
			switch (newView) {
				case 0 -> {
					centerAndRescale = view == 2;
					setImage(input);
				}
				case 1 -> {
					centerAndRescale = view == 2;
					setImage(renderedBinary);
				}
				case 2 -> {
					centerAndRescale = view < 2;
					setImage(renderedTran);
				}
			}
			view = newView;
			autoScaleCenterOnSetImage = centerAndRescale;
		}
	}

	protected class ControlPanel extends ViewedImageInfoPanel {
		JComboBox<String> comboView = combo(0, "Lines", "Binary", "Transformed");
		JCheckBox checkLog = checkbox("Log Intensity", logIntensity);
		JCheckBox checkShowLines = checkbox("Show Lines", showLines);
		JComboBox<String> comboBinarization = combo(configHough.binarization.ordinal(),
				(Object[])ConfigHoughBinary.Binarization.values());
		JSpinner spinnerResRange = spinner(configPolar.resolutionRange, 0.1, 50, 0.5);
		JSpinner spinnerBinsAngle = spinner(configPolar.numBinsAngle, 10, 1000, 1);
		JSpinner spinnerMaxLines = spinner(configHough.maxLines, 0, 200, 1);
		JSpinner spinnerBlur = spinner(blurRadius, 0, 20, 1);
		JConfigLength lengthCounts = configLength(configHough.minCounts, 0, Double.MAX_VALUE);
		JSpinner spinnerLocalMax = spinner(configHough.localMaxRadius, 1, 100, 2);
		JCheckBox checkMergeSimilar = checkbox("Merge Similar", mergeSimilar);
		ControlPanelEdgeThreshold controlEdgeThreshold = new ControlPanelEdgeThreshold(configHough.thresholdEdge,
				VisualizeHoughBinary.this::handleConfigChange);

		public ControlPanel() {
			super(BoofSwingUtil.MIN_ZOOM, BoofSwingUtil.MAX_ZOOM, 0.5, false);

			controlEdgeThreshold.setBorder(BorderFactory.createTitledBorder("Edge Threshold"));

			lengthCounts.setValue(configHough.minCounts);
			lengthCounts.setMaximumSize(lengthCounts.getPreferredSize());

			handleChangeBinarization();

			addLabeled(comboView, "View");
			addAlignLeft(checkLog, "Transform parameter space visualization by applying log");
			addAlignLeft(checkShowLines, "Renders found lines");
			addSeparator(140);
			addLabeled(comboBinarization, "Binarization", "Method used to compute a binary image of points on lines");
			addLabeled(spinnerMaxLines, "Max Lines", "Maximum number of lines it will detect");
			addLabeled(spinnerBlur, "Blur Radius", "Amount of blur applied prior to processing");
			addLabeled(spinnerResRange, "Res. Range", "Resolution of range discretization in transform (pixels)");
			addLabeled(spinnerBinsAngle, "Bins Angle", "Number of bins for angles in transform");
			addLabeled(lengthCounts, "Min. Count", "Minimum number of counts for detected line");
			addLabeled(spinnerLocalMax, "Local Max", "Local maximum suppression radius");
			addAlignLeft(checkMergeSimilar, "Merge lines which are similar");
			addAlignCenter(BoofSwingUtil.wrapBorder(controlEdgeThreshold), "Threshold for thresholding an edge");
			addVerticalGlue();
		}

		private void handleChangeBinarization() {
			boolean enabled = configHough.binarization == ConfigHoughBinary.Binarization.EDGE;
			BoofSwingUtil.recursiveEnable(controlEdgeThreshold, enabled);
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == comboBinarization) {
				configHough.binarization =
						ConfigHoughBinary.Binarization.values()[comboBinarization.getSelectedIndex()];
				handleChangeBinarization();
				handleConfigChange();
			} else if (source == spinnerResRange) {
				configPolar.resolutionRange = (Double)spinnerResRange.getValue();
				handleConfigChange();
			} else if (source == spinnerBinsAngle) {
				configPolar.numBinsAngle = (Integer)spinnerBinsAngle.getValue();
				handleConfigChange();
			} else if (source == spinnerMaxLines) {
				configHough.maxLines = (Integer)spinnerMaxLines.getValue();
				handleConfigChange();
			} else if (source == spinnerBlur) {
				blurRadius = (Integer)spinnerBlur.getValue();
				handleConfigChange();
			} else if (source == spinnerLocalMax) {
				configHough.localMaxRadius = (Integer)spinnerLocalMax.getValue();
				handleConfigChange();
			} else if (source == comboView) {
				imagePanel.handleViewChange(comboView.getSelectedIndex());
				imagePanel.repaint();
			} else if (source == checkLog) {
				logIntensity = checkLog.isSelected();
				reprocessImageOnly();
			} else if (source == checkMergeSimilar) {
				mergeSimilar = checkMergeSimilar.isSelected();
				handleConfigChange();
			} else if (source == checkShowLines) {
				showLines = checkShowLines.isSelected();
				imagePanel.repaint();
			} else if (source == lengthCounts) {
				configHough.minCounts.setTo(lengthCounts.getValue());
				handleConfigChange();
			}
		}
	}

	private void handleConfigChange() {
		createAlg();
		reprocessImageOnly();
	}

	public static void main( String[] args ) {
		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Simple Objects", UtilIO.pathExample("simple_objects.jpg")));
		examples.add(new PathLabel("Indoors", UtilIO.pathExample("lines_indoors.jpg")));
		examples.add(new PathLabel("Outdoors", UtilIO.pathExample("outdoors01.jpg")));
		examples.add(new PathLabel("Drawn Lines", UtilIO.pathExample("shapes/black_lines_01.jpg")));
		examples.add(new PathLabel("Indoors Video", UtilIO.pathExample("lines_indoors.mjpeg")));

		SwingUtilities.invokeLater(() -> {
			var app = new VisualizeHoughBinary<GrayF32, GrayF32>(examples, GrayF32.class);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Hough Binary");
		});
	}
}
