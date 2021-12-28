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

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

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
public class DetectBlackPolygonApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase implements ShapeGuiListener {
	DetectPolygonBinaryGrayRefine<T> detector;
	DetectBlackPolygonAppControlPanel controlPanel = new DetectBlackPolygonAppControlPanel(this);

	public DetectBlackPolygonApp( List<String> examples, Class<T> imageType ) {
		super(examples, imageType);

		controlPanel.polygonPanel.thresholdPanel.addHistogramGraph();
		setupGui(new VisualizePanel(), controlPanel);
	}

	@Override
	protected void createDetector( boolean initializing ) {
		if (!initializing)
			BoofSwingUtil.checkGuiThread();

		synchronized (this) {
			ConfigPolygonDetector config = getConfigPolygonDetector();

			detector = FactoryShapeDetector.polygon(config, imageClass);
		}
		imageThresholdUpdated();
	}

	public ConfigPolygonDetector getConfigPolygonDetector() {
		DetectBlackPolygonAppControlPanel controls = (DetectBlackPolygonAppControlPanel)DetectBlackPolygonApp.this.controls;
		ConfigPolygonDetector config = controls.polygonPanel.getConfigPolygon();
		return config;
	}

	@Override
	public void configUpdate() {
		createDetector(false);
		// does process and render too
	}

	@Override
	public void imageThresholdUpdated() {
		DetectBlackPolygonAppControlPanel controls = (DetectBlackPolygonAppControlPanel)DetectBlackPolygonApp.this.controls;

		ConfigThreshold config = controls.polygonPanel.getThresholdPanel().createConfig();

		synchronized (this) {
			inputToBinary = FactoryThresholdBinary.threshold(config, imageClass);
		}
		reprocessImageOnly();
	}

	int count = 0;

	@Override
	protected void detectorProcess( ImageGray input, GrayU8 binary ) {
//		System.out.println("processing image "+count++);
		detector.process((T)input, binary);
		detector.refineAll();

		controlPanel.polygonPanel.thresholdPanel.updateHistogram(input);
	}

	class VisualizePanel extends ShapeVisualizePanel {
		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {

			DetectBlackPolygonAppControlPanel controls = (DetectBlackPolygonAppControlPanel)DetectBlackPolygonApp.this.controls;

			BoofSwingUtil.antialiasing(g2);

			synchronized (DetectBlackPolygonApp.this) {

				if (controls.bShowContour) {
					g2.setStroke(new BasicStroke(1));

					BinaryContourFinder contour = detector.getDetector().getContourFinder();

					List<Contour> contours = BinaryImageOps.convertContours(contour);


					VisualizeBinaryData.render(contours, null, Color.CYAN, 1.0, scale, g2);
				}

				if (controls.bShowLines) {
					List<Polygon2D_F64> polygons = detector.getPolygons(null, null);

					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						int red = 255*((p.size() - 3)%4)/3;
						int green = 255*(p.size()%5)/4;
						int blue = 255*((p.size() + 2)%6)/5;

						g2.setColor(new Color(red, green, blue));

						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowCorners) {
					List<Polygon2D_F64> polygons = detector.getPolygons(null, null);

					g2.setColor(Color.BLUE);
					g2.setStroke(new BasicStroke(1));
					for (Polygon2D_F64 p : polygons) {
						for (int i = 0; i < p.size(); i++) {
							Point2D_F64 c = p.get(i);
							VisualizeFeatures.drawCircle(g2, scale*c.x, scale*c.y, 5);
						}
					}
				}
			}
		}
	}

	public static void main( String[] args ) {

		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("shapes/polygons01.jpg"));
		examples.add(UtilIO.pathExample("shapes/polygons_border_01.jpg"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("shapes/concave01.jpg"));
		examples.add(UtilIO.pathExample("shapes/line_text_test_image.png"));
		examples.add(UtilIO.pathExample("fiducial/binary/image0000.jpg"));
		examples.add(UtilIO.pathExample("calibration/stereo/Bumblebee2_Square/left10.jpg"));
		examples.add(UtilIO.pathExample("fiducial/square_grid/movie.mp4"));

		DetectBlackPolygonApp app = new DetectBlackPolygonApp(examples, GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.waitUntilInputSizeIsKnown();
		app.display("Detect Black Polygons");
	}
}
