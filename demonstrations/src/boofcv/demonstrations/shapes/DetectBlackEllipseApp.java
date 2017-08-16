/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import georegression.struct.shapes.EllipseRotated_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
public class DetectBlackEllipseApp<T extends ImageGray<T>> extends DetectBlackShapeAppBase
{

	BinaryEllipseDetector<T> detector;

	public DetectBlackEllipseApp(List<String> examples , Class<T> imageType) {
		super(examples, imageType);
		setupGui(new VisualizePanel(), new DetectEllipseControlPanel(this) );
	}

	@Override
	protected void createDetector() {
		DetectEllipseControlPanel controls = (DetectEllipseControlPanel)DetectBlackEllipseApp.this.controls;
		synchronized (this) {
			detector = FactoryShapeDetector.ellipse(controls.getConfigEllipse(), imageClass);
		}
		imageThresholdUpdated();
	}

	@Override
	public void processImage(int sourceID, long frameID, final BufferedImage buffered, ImageBase input) {
		original = ConvertBufferedImage.checkCopy(buffered,original);
		work = ConvertBufferedImage.checkDeclare(buffered,work);

		binary.reshape(work.getWidth(), work.getHeight());
		this.input = (T)input;

		synchronized (this) {
			inputToBinary.process((T) input, binary);
			detector.process((T) input, binary);
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				viewUpdated();
			}
		});
	}

	public void configUpdate() {
		createDetector();
		// does process and render too
	}

	@Override
	public void imageThresholdUpdated() {
		DetectEllipseControlPanel controls = (DetectEllipseControlPanel)DetectBlackEllipseApp.this.controls;

		ConfigThreshold config = controls.getThreshold().createConfig();

		synchronized (this) {
			inputToBinary = FactoryThresholdBinary.threshold(config, imageClass);
		}
		reprocessImageOnly();
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			DetectEllipseControlPanel controls = (DetectEllipseControlPanel)DetectBlackEllipseApp.this.controls;

			synchronized ( DetectBlackEllipseApp.this ) {
				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				if (controls.bShowContour) {
					List<Contour> contours = detector.getAllContours();
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null,Color.CYAN, scale, g2);
				}

				if (controls.bShowShapes) {
					List<EllipseRotated_F64> ellipses = detector.getFoundEllipses().toList();

					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));

					for (EllipseRotated_F64 p : ellipses) {
						VisualizeShapes.drawEllipse(p,scale,g2);
					}
				}
			}
		}
	}

	public static void main(String[] args) {

		List<String> examples = new ArrayList<>();
		examples.add("fiducial/circle_asymmetric/image01.jpg");
		examples.add("shapes/polygons01.jpg");
		examples.add("shapes/shapes01.png");
		examples.add("shapes/shapes02.png");
		examples.add("fiducial/circle_asymmetric/image00.jpg");
		examples.add("fiducial/circle_asymmetric/movie.mp4");

		DetectBlackEllipseApp app = new DetectBlackEllipseApp(examples,GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.waitUntilInputSizeIsKnown();

		ShowImages.showWindow(app,"Detect Black Ellipses",true);
	}



}
