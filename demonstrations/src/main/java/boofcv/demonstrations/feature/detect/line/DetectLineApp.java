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

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.abst.feature.detect.line.DetectLineSegment;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFootSubimage;
import boofcv.factory.feature.detect.line.ConfigHoughGradient;
import boofcv.factory.feature.detect.line.ConfigLineRansac;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows detected lines inside of different images.
 *
 * @author Peter Abeles
 */
// todo configure: blur, edge threshold, non-max radius,  min counts
// todo show binary image, transform
@SuppressWarnings({"NullAway.Init"})
public class DetectLineApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends DemonstrationBase {
	Class<T> imageType;
	Class<D> derivType;

	T blur;
	T work;

	float edgeThreshold = 25;

	final ImageLinePanel gui = new ImageLinePanel();
	final ControlPanel controls = new ControlPanel();

	@Nullable DetectLine<T> lineDetector;
	@Nullable DetectLineSegment<T> segmentDetector;
	final Object lockDetector = new Object();

	public DetectLineApp( List<PathLabel> examples, Class<T> imageType, Class<D> derivType ) {
		super(examples, ImageType.single(imageType));

		this.imageType = imageType;
		this.derivType = derivType;

		blur = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		work = blur.createSameShape();

		add(controls, BorderLayout.WEST);
		add(gui, BorderLayout.CENTER);

		declareDetector();
	}

	private void declareDetector() {
		synchronized (lockDetector) {
			lineDetector = null;
			segmentDetector = null;

			ConfigHoughGradient configGradient = new ConfigHoughGradient();
			configGradient.maxLines = controls.maxLines;

			switch (controls.whichAlg) {
				case 0:
					lineDetector = FactoryDetectLine.houghLinePolar(configGradient, null, imageType);
					break;

				case 1:
					lineDetector = FactoryDetectLine.houghLineFoot(configGradient, null, imageType);
					break;

				case 2:
					lineDetector = FactoryDetectLine.houghLineFootSub(
							new ConfigHoughFootSubimage(3, 8, 5, edgeThreshold,
									controls.maxLines, 2, 2), imageType);
					break;

				case 3:
					segmentDetector = FactoryDetectLine.lineRansac(new ConfigLineRansac(40, 30, 2.36, true), imageType);
					break;
			}
		}
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		blur.reshape(width, height);
		work.reshape(width, height);
		SwingUtilities.invokeLater(() -> {
			gui.setPreferredSize(new Dimension(width, height));
		});
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, final ImageBase _input ) {
		T input = (T)_input;

		if (controls.blurRadius >= 1)
			GBlurImageOps.gaussian(input, blur, -1, controls.blurRadius, work);
		else
			blur.setTo(input);

		synchronized (lockDetector) {
			if (lineDetector != null) {
				List<LineParametric2D_F32> lines = lineDetector.detect(blur);
				SwingUtilities.invokeLater(() -> {
					gui.setImage(buffered);
					gui.setLines(lines);
					gui.repaint();
				});
			} else if (segmentDetector != null) {
				List<LineSegment2D_F32> lines = segmentDetector.detect(blur);
				SwingUtilities.invokeLater(() -> {
					gui.setImage(buffered);
					gui.setLineSegments(lines);
					gui.repaint();
				});
			}
		}
	}

	private class ControlPanel extends StandardAlgConfigPanel
			implements ActionListener, ChangeListener {

		JComboBox comboAlg;
		JSpinner spinnerMaxLines;
		JSpinner spinnerBlur;

		int whichAlg = 0;
		int maxLines = 10;
		int blurRadius = 2;

		public ControlPanel() {

			comboAlg = combo(whichAlg, "Hough Polar", "Hough Foot", "Hough Foot Sub Image", "Grid Line");
			spinnerMaxLines = spinner(maxLines, 1, 100, 1);
			spinnerBlur = spinner(blurRadius, 0, 20, 1);

			addAlignCenter(comboAlg);
			addLabeled(spinnerMaxLines, "Lines");
			addLabeled(spinnerBlur, "Blur Radius");
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (e.getSource() == comboAlg) {
				whichAlg = comboAlg.getSelectedIndex();

				switch (whichAlg) {
					case 0:
					case 1:
					case 2:
						spinnerMaxLines.setEnabled(true);
						break;

					default:
						spinnerMaxLines.setEnabled(false);
				}
			}
			declareDetector();
			reprocessImageOnly();
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (e.getSource() == spinnerBlur) {
				blurRadius = ((Number)spinnerBlur.getValue()).intValue();
			} else if (e.getSource() == spinnerMaxLines) {
				maxLines = ((Number)spinnerMaxLines.getValue()).intValue();
			}
			declareDetector();
			reprocessImageOnly();
		}
	}

	public static void main( String[] args ) {
		Class imageType = GrayF32.class;
		Class derivType = GrayF32.class;

		java.util.List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Objects", UtilIO.pathExample("simple_objects.jpg")));
		examples.add(new PathLabel("Indoors", UtilIO.pathExample("lines_indoors.jpg")));
		examples.add(new PathLabel("Chessboard", UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));
		examples.add(new PathLabel("Apartment", UtilIO.pathExample("lines_indoors.mjpeg")));

		SwingUtilities.invokeLater(() -> {
			DetectLineApp app = new DetectLineApp(examples, imageType, derivType);

			app.openExample(examples.get(0));
			app.display("Line Detector");
		});
	}
}
