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

package boofcv.demonstrations.shapes;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.FitData;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_I32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Fits shapes to contours from binary images
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ShapeFitContourApp extends DemonstrationBase implements ThresholdControlPanel.Listener {
	// displays intensity image
	VisualizePanel gui = new VisualizePanel();

	// converted input image
	GrayU8 inputPrev = new GrayU8(1, 1);
	GrayU8 binary = new GrayU8(1, 1);
	GrayU8 filtered = new GrayU8(1, 1);
	// if it has processed an image or not
	boolean processImage = false;

	InputToBinary<GrayU8> inputToBinary;

	// Found contours
	List<Contour> contours;

	BufferedImage original;
	BufferedImage work = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

	ShapeFitContourPanel controlPanel;

	public ShapeFitContourApp( List<String> examples ) {
		super(examples, ImageType.single(GrayU8.class));

		this.gui.autoScaleCenterOnSetImage = false;
		controlPanel = new ShapeFitContourPanel(this);

		gui.setListener(scale -> {
			controlPanel.setZoom(scale);
		});

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, gui);

		ConfigThreshold config = controlPanel.getThreshold().createConfig();
		inputToBinary = FactoryThresholdBinary.threshold(config, GrayU8.class);
	}

	@Override
	public synchronized void processImage( int sourceID, long frameID,
										   final @Nullable BufferedImage buffered, @Nullable ImageBase input ) {
		if (buffered != null && input != null) {
			original = ConvertBufferedImage.checkCopy(buffered, original);
			work = ConvertBufferedImage.checkDeclare(buffered, work);

			binary.reshape(input.getWidth(), input.getHeight());
			filtered.reshape(input.getWidth(), input.getHeight());
			inputPrev.setTo((GrayU8)input);

			SwingUtilities.invokeLater(() -> {
				Dimension d = gui.getPreferredSize();
				if (d.getWidth() < buffered.getWidth() || d.getHeight() < buffered.getHeight()) {
					gui.setPreferredSize(new Dimension(buffered.getWidth(), buffered.getHeight()));
				}
			});
		} else {
			input = inputPrev;
		}

		process((GrayU8)input);
	}

	public synchronized void viewUpdated() {
		if (contours == null)
			return;

		int view = controlPanel.getSelectedView();

		Graphics2D g2 = work.createGraphics();

		if (view == 0) {
			g2.drawImage(original, 0, 0, null);
		} else if (view == 1) {
			VisualizeBinaryData.renderBinary(binary, false, work);
		} else {
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, work.getWidth(), work.getHeight());
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				gui.setScale(controlPanel.getZoom());
				gui.setImage(work);
				gui.repaint();
			}
		});
	}

	public void process( final GrayU8 input ) {

		// threshold the input image
		inputToBinary.process(input, binary);

		// reduce noise with some filtering
		BinaryImageOps.erode8(binary, 1, filtered);
		BinaryImageOps.dilate8(filtered, 1, binary);

		// Find the contour around the shapes
		contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
		processImage = true;

		viewUpdated();
	}

	@Override
	public void imageThresholdUpdated() {
		ConfigThreshold config = controlPanel.getThreshold().createConfig();
		inputToBinary = FactoryThresholdBinary.threshold(config, GrayU8.class);
		reprocessImageOnly();
	}

	protected void renderVisuals( Graphics2D g2, double scale ) {
		BoofSwingUtil.antialiasing(g2);

		int activeAlg = controlPanel.getSelectedAlgorithm();

		g2.setStroke(new BasicStroke(3));

		if (controlPanel.contoursVisible) {
			g2.setStroke(new BasicStroke(1));
			VisualizeBinaryData.render(contours, null, Color.CYAN, 1.0, scale, g2);
		}

		if (activeAlg == 0) {

			double cornerPalty = controlPanel.getCornerPenalty();
			int minimumSplitPixels = controlPanel.getMinimumSplitPixels();

			for (Contour c : contours) {
				List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(
						c.external, true, minimumSplitPixels, cornerPalty);

				g2.setColor(Color.RED);
				visualizePolygon(g2, scale, vertexes);

				for (List<Point2D_I32> internal : c.internal) {
					vertexes = ShapeFittingOps.fitPolygon(internal, true, minimumSplitPixels, cornerPalty);

					g2.setColor(Color.GREEN);
					visualizePolygon(g2, scale, vertexes);
				}
			}
		} else if (activeAlg == 1) {
			// Filter small contours since they can generate really wacky ellipses
			for (Contour c : contours) {
				if (c.external.size() > 10) {
					FitData<EllipseRotated_F64> ellipse = ShapeFittingOps.fitEllipse_I32(c.external, 0, false, null);

					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(2.5f));
					VisualizeShapes.drawEllipse(ellipse.shape, scale, g2);
				}

				for (List<Point2D_I32> internal : c.internal) {
					if (internal.size() <= 10)
						continue;
					FitData<EllipseRotated_F64> ellipse = ShapeFittingOps.fitEllipse_I32(internal, 0, false, null);

					g2.setColor(Color.GREEN);
					g2.setStroke(new BasicStroke(2.5f));
					VisualizeShapes.drawEllipse(ellipse.shape, scale, g2);
				}
			}
		}
	}

	private void visualizePolygon( Graphics2D g2, double scale, List<PointIndex_I32> vertexes ) {
		g2.setStroke(new BasicStroke(2));
		VisualizeShapes.drawPolygon(vertexes, true, scale, g2);

		if (controlPanel.isCornersVisible()) {
			g2.setColor(Color.BLUE);
			g2.setStroke(new BasicStroke(2f));
			for (PointIndex_I32 p : vertexes) {
				VisualizeFeatures.drawCircle(g2, scale*(p.x + 0.5), scale*(p.y + 0.5), 5);
			}
		}
	}

	class VisualizePanel extends ShapeVisualizePanel {
		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			synchronized (ShapeFitContourApp.this) {
				renderVisuals(g2, scale);
			}
		}
	}

	public static void main( String[] args ) {

		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("particles01.jpg"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("shapes/line_text_test_image.png"));

		ShapeFitContourApp app = new ShapeFitContourApp(examples);

		app.openFile(new File(examples.get(0)));

		app.display("Contour Shape Fitting");
	}
}
