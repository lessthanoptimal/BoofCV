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
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.Configuration;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

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
public class DetectBlackPolygonApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase
{
	BinaryPolygonDetector<T> detector;

	public DetectBlackPolygonApp(List<String> examples , Class<T> imageType) {
		super(examples, imageType);

		setupGui(new VisualizePanel(),new DetectPolygonControlPanel(this));
	}

	@Override
	protected void createDetector() {
		DetectPolygonControlPanel controls = (DetectPolygonControlPanel)DetectBlackPolygonApp.this.controls;

		Configuration configRefine = null;

		if( controls.refineType == PolygonRefineType.LINE ) {
			configRefine = controls.getConfigLine();
		} else if( controls.refineType == PolygonRefineType.CORNER ) {
			configRefine = controls.getConfigCorner();
		}
		controls.getConfigPolygon().refine = configRefine;

		synchronized (this) {
			detector = FactoryShapeDetector.polygon(controls.getConfigPolygon(), imageClass);
		}
		imageThresholdUpdated();
	}


	public void configUpdate() {
		createDetector();
		// does process and render too
	}

	@Override
	public void imageThresholdUpdated() {
		DetectPolygonControlPanel controls = (DetectPolygonControlPanel)DetectBlackPolygonApp.this.controls;

		ConfigThreshold config = controls.getThreshold().createConfig();

		synchronized (this) {
			inputToBinary = FactoryThresholdBinary.threshold(config, imageClass);
		}
		reprocessImageOnly();
	}

	@Override
	public void processImage(int sourceID, long frameID, final BufferedImage buffered, ImageBase input) {
		System.out.flush();
		this.input = (T)input;

		original = ConvertBufferedImage.checkCopy(buffered,original);
		work = ConvertBufferedImage.checkDeclare(buffered,work);

		binary.reshape(work.getWidth(), work.getHeight());

		synchronized (this) {
			inputToBinary.process((T)input, binary);
			detector.process((T) input, binary);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				viewUpdated();
			}
		});
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {

			DetectPolygonControlPanel controls = (DetectPolygonControlPanel)DetectBlackPolygonApp.this.controls;

			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized ( DetectBlackPolygonApp.this ) {

				if (controls.bShowContour) {
					List<Contour> contours = detector.getAllContours();
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null,Color.CYAN, scale, g2);
				}

				if (controls.bShowLines) {
					List<Polygon2D_F64> polygons = detector.getFoundPolygons().toList();

					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						int red = 255 * ((p.size() - 3) % 4) / 3;
						int green = 255 * ((p.size()) % 5) / 4;
						int blue = 255 * ((p.size() + 2) % 6) / 5;

						g2.setColor(new Color(red, green, blue));

						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowCorners) {
					List<Polygon2D_F64> polygons = detector.getFoundPolygons().toList();

					g2.setColor(Color.BLUE);
					g2.setStroke(new BasicStroke(1));
					for (Polygon2D_F64 p : polygons) {
						for (int i = 0; i < p.size(); i++) {
							Point2D_F64 c = p.get(i);
							VisualizeFeatures.drawCircle(g2, scale * c.x, scale * c.y, 5);
						}
					}
				}
			}
		}
	}

	public static void main(String[] args) {

		List<String> examples = new ArrayList<>();
		examples.add("shapes/polygons01.jpg");
		examples.add("shapes/shapes01.png");
		examples.add("shapes/shapes02.png");
		examples.add("shapes/concave01.jpg");
		examples.add("shapes/line_text_test_image.png");
		examples.add("shapes/polygons_border_01.jpg");
		examples.add("fiducial/binary/image0000.jpg");
		examples.add("calibration/stereo/Bumblebee2_Square/left10.jpg");
		examples.add("fiducial/square_grid/movie.mp4");

		DetectBlackPolygonApp app = new DetectBlackPolygonApp(examples,GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.waitUntilInputSizeIsKnown();

		ShowImages.showWindow(app,"Detect Black Polygons",true);
	}



}
