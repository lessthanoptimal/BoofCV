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

import boofcv.abst.filter.binary.BinaryContourInterface;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectBlackEllipseApp<T extends ImageGray<T>> extends DetectBlackShapeAppBase {

	BinaryEllipseDetector<T> detector;

	public DetectBlackEllipseApp( List<String> examples, Class<T> imageType ) {
		super(examples, imageType);
		setupGui(new VisualizePanel(), new DetectEllipseControlPanel(this));
	}

	@Override
	protected void createDetector( boolean initializing ) {
		if (!initializing)
			BoofSwingUtil.checkGuiThread();

		DetectEllipseControlPanel controls = (DetectEllipseControlPanel)DetectBlackEllipseApp.this.controls;
		synchronized (this) {
			detector = FactoryShapeDetector.ellipse(controls.getConfigEllipse(), imageClass);
		}
		imageThresholdUpdated();
	}

	@Override
	protected void detectorProcess( ImageGray input, GrayU8 binary ) {
		detector.process((T)input, binary);
	}

	public void configUpdate() {
		createDetector(false);
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

	class VisualizePanel extends ShapeVisualizePanel {
		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			DetectEllipseControlPanel controls = (DetectEllipseControlPanel)DetectBlackEllipseApp.this.controls;

			synchronized (DetectBlackEllipseApp.this) {
				BoofSwingUtil.antialiasing(g2);

				if (controls.bShowContour) {
					BinaryContourInterface contour = detector.getEllipseDetector().getContourFinder();
					List<Contour> contours = BinaryImageOps.convertContours(contour);

					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null, Color.CYAN, 1.0, scale, g2);
				}

				if (controls.bShowShapes) {
					List<BinaryEllipseDetector.EllipseInfo> ellipses = detector.getFound().toList();

					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));

					for (BinaryEllipseDetector.EllipseInfo p : ellipses) {
						VisualizeShapes.drawEllipse(p.ellipse, scale, g2);
					}
				}
			}
		}
	}

	public static void main( String[] args ) {

		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("fiducial/circle_hexagonal/image00.jpg"));
		examples.add(UtilIO.pathExample("shapes/polygons01.jpg"));
		examples.add(UtilIO.pathExample("shapes/shapes01.png"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("fiducial/circle_hexagonal/image01.jpg"));
		examples.add(UtilIO.pathExample("fiducial/circle_hexagonal/movie.mp4"));

		DetectBlackEllipseApp app = new DetectBlackEllipseApp(examples, GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.display("Detect Black Ellipses");
	}
}
