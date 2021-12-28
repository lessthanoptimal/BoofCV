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

import boofcv.abst.feature.detect.line.HoughGradient_to_DetectLine;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughGradient;
import boofcv.factory.feature.detect.line.ConfigParamFoot;
import boofcv.factory.feature.detect.line.ConfigParamPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
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
import javax.swing.event.ChangeListener;
import java.awt.*;
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
@SuppressWarnings({"NullAway.Init"})
public class VisualizeHoughGradient<I extends ImageGray<I>, D extends ImageGray<D>> extends DemonstrationBase {

	Class<I> imageType;
	Class<D> derivType;

	I blur;

	Visualization imagePanel = new Visualization();
	ControlPanel controlPanel;

	//--------------------------
	final Object lockAlg = new Object();
	HoughGradient_to_DetectLine<I, D> lineDetector; // use a high level line detector since it already has boilerplate code in it
	//--------------------------

	ConfigHoughGradient configHough = new ConfigHoughGradient();
	ConfigParamFoot configFoot = new ConfigParamFoot();
	ConfigParamPolar configPolar = new ConfigParamPolar();
	int blurRadius = 5;
	int view = 0;
	boolean logIntensity = false;
	Type type = Type.FOOT;
	boolean mergeSimilar = true;
	boolean showLines = true;

	GrayF32 transformLog = new GrayF32(1, 1);

	BufferedImage renderedTran;
	BufferedImage renderedBinary;

	// is this the first time the image has been opened?
	boolean firstOpenImage = true;

	public VisualizeHoughGradient( List<PathLabel> examples, Class<I> imageType ) {
		super(true, true, examples, ImageType.single(imageType));
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		// override default configurations
		configHough.maxLines = 10;

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
		blur = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, imagePanel);
	}

	private void createAlg() {
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
			lineDetector = switch (type) {
				case FOOT -> lineDetector = (HoughGradient_to_DetectLine)
						FactoryDetectLine.houghLineFoot(configHough, configFoot, imageType);
				case POLAR -> lineDetector = (HoughGradient_to_DetectLine)
						FactoryDetectLine.houghLinePolar(configHough, configPolar, imageType);
			};
		}

		SwingUtilities.invokeLater(() -> {
			boolean enabled = type == Type.POLAR;
			controlPanel.spinnerBinsAngle.setEnabled(enabled);
			controlPanel.spinnerResRange.setEnabled(enabled);
		});
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		blur.reshape(width, height);

		synchronized (lockAlg) {
			int tranWidth = lineDetector.getHough().getTransform().width;
			int tranHeight = lineDetector.getHough().getTransform().height;
			renderedTran = ConvertBufferedImage.checkDeclare(tranWidth, tranHeight, renderedTran, BufferedImage.TYPE_INT_RGB);
		}
		renderedBinary = ConvertBufferedImage.checkDeclare(width, height, renderedBinary, BufferedImage.TYPE_INT_RGB);

		BoofSwingUtil.invokeNowOrLater(() -> {
			imagePanel.setPreferredSize(new Dimension(width, height));
			controlPanel.setImageSize(width, height);
		});

		// let it know a new image has been opened so that the view can be reset later
		firstOpenImage = true;
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase _input ) {
		I input = (I)_input;

		long time0 = System.nanoTime();
		if (blurRadius > 0) {
			GBlurImageOps.gaussian(input, blur, -1, blurRadius, null);
		} else {
			blur.setTo(input);
		}
		List<LineParametric2D_F32> lines;
		synchronized (lockAlg) {
			lines = lineDetector.detect(blur);
		}

		imagePanel.setLines(lines, input.width, input.height);
		long time1 = System.nanoTime();

		imagePanel.input = buffered;

		int tranWidth = lineDetector.getHough().getTransform().width;
		int tranHeight = lineDetector.getHough().getTransform().height;
		renderedTran = ConvertBufferedImage.checkDeclare(tranWidth, tranHeight, renderedTran, BufferedImage.TYPE_INT_RGB);

		if (logIntensity) {
			PixelMath.log(lineDetector.getHough().getTransform(), 1.0f, transformLog);
			VisualizeImageData.grayMagnitude(transformLog, renderedTran, -1);
		} else {
			VisualizeImageData.grayMagnitude(lineDetector.getHough().getTransform(), renderedTran, -1);
		}

		VisualizeBinaryData.renderBinary(lineDetector.getBinary(), false, renderedBinary);

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

	private class Visualization extends ImageLinePanelZoom {
		BufferedImage input;
		Ellipse2D.Double c = new Ellipse2D.Double();
		Point2D_F64 coordinate = new Point2D_F64();

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

		private void paintLinesInTransform( Graphics2D g2 ) {
			BoofSwingUtil.antialiasing(g2);

			int r = 6;
			g2.setStroke(new BasicStroke(2));
			int selected = imagePanel.getSelected();
			synchronized (lockAlg) {
				List<LineParametric2D_F32> found = lineDetector.getHough().getLinesMerged();
				for (int i = 0; found != null && i < found.size(); i++) {
					LineParametric2D_F32 l = found.get(i);

					lineDetector.getHough().getParameters().lineToCoordinate(l, coordinate);

					c.setFrame((coordinate.x + 0.5)*scale - r, (coordinate.y + 0.5)*scale - r, 2*r, 2*r);

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
			implements ChangeListener, ActionListener {
		JComboBox<String> comboView = combo(0, "Lines", "Edges", "Transformed");
		JCheckBox checkLog = checkbox("Log Intensity", logIntensity);
		JComboBox<String> comboType = combo(type.ordinal(), "Foot", "Polar");
		JSpinner spinnerMaxLines = spinner(configHough.maxLines, 0, 200, 1);
		JSpinner spinnerBlur = spinner(blurRadius, 0, 20, 1);
		JSpinner spinnerMinCount = spinner(configHough.minCounts, 1, 100, 1);
		JSpinner spinnerLocalMax = spinner(configHough.localMaxRadius, 1, 100, 2);
		JSpinner spinnerRefRadius = spinner(configHough.refineRadius, 0, 20, 1);
		JCheckBox checkMergeSimilar = checkbox("Merge Similar", mergeSimilar);
		ControlPanelEdgeThreshold controlEdgeThreshold = new ControlPanelEdgeThreshold(configHough.edgeThreshold,
				VisualizeHoughGradient.this::handleConfigChange);
		JCheckBox checkShowLines = checkbox("Show Lines", showLines);

		JSpinner spinnerResRange = spinner(configPolar.resolutionRange, 0.1, 50, 0.5);
		JSpinner spinnerBinsAngle = spinner(configPolar.numBinsAngle, 10, 1000, 1);

		public ControlPanel() {
			super(BoofSwingUtil.MIN_ZOOM, BoofSwingUtil.MAX_ZOOM, 0.5, false);

			controlEdgeThreshold.setBorder(BorderFactory.createTitledBorder("Edge Threshold"));
			controlEdgeThreshold.setMaximumSize(controlEdgeThreshold.getPreferredSize());
			addLabeled(comboView, "View");
			addAlignLeft(checkLog, "Transform parameter space visualization by applying log");
			addAlignLeft(checkShowLines, "Show lines");
			addSeparator(140);
			addLabeled(comboType, "Param. Type", "Line equation parameterization");
			addLabeled(spinnerMaxLines, "Max Lines", "Maximum number of lines it will detect");
			addLabeled(spinnerBlur, "Blur Radius", "Amount of blur applied to the image");
			addLabeled(spinnerMinCount, "Min. Count", "Minimum number of counts/votes inside the transformed image");
			addLabeled(spinnerLocalMax, "Local Max", "Non-maximum suppression radius");
			addLabeled(spinnerRefRadius, "Refine Radius", "Radius of mean-shift refinement. Set to zero to turn off.");
			addAlignLeft(checkMergeSimilar, "Check to see if two lines are similar and if so merge them");
			addLabeled(spinnerResRange, "Res. Range", "Resolution of range discretization (pixels) in transformed");
			addLabeled(spinnerBinsAngle, "Bins Angle", "Number of bins for angles in transform");
			addAlignCenter(BoofSwingUtil.wrapBorder(controlEdgeThreshold), "Threshold for thresholding an edge");
			addVerticalGlue();
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerResRange) {
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
			} else if (source == spinnerMinCount) {
				configHough.minCounts = (Integer)spinnerMinCount.getValue();
				handleConfigChange();
			} else if (source == spinnerLocalMax) {
				configHough.localMaxRadius = (Integer)spinnerLocalMax.getValue();
				handleConfigChange();
			} else if (source == spinnerRefRadius) {
				configHough.refineRadius = (Integer)spinnerRefRadius.getValue();
				handleConfigChange();
			} else if (source == comboType) {
				type = Type.values()[comboType.getSelectedIndex()];
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
			}
		}
	}

	private void handleConfigChange() {
		createAlg();
		reprocessImageOnly();
	}

	enum Type {
		FOOT,
		POLAR
	}

	public static void main( String[] args ) {
		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Simple Objects", UtilIO.pathExample("simple_objects.jpg")));
		examples.add(new PathLabel("Indoors", UtilIO.pathExample("lines_indoors.jpg")));
		examples.add(new PathLabel("Outdoors", UtilIO.pathExample("outdoors01.jpg")));
		examples.add(new PathLabel("Drawn Lines", UtilIO.pathExample("shapes/black_lines_01.jpg")));
		examples.add(new PathLabel("Indoors Video", UtilIO.pathExample("lines_indoors.mjpeg")));

		SwingUtilities.invokeLater(() -> {
			VisualizeHoughGradient<GrayF32, GrayF32> app = new VisualizeHoughGradient(examples, GrayF32.class);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Hough Gradient");
		});
	}
}
