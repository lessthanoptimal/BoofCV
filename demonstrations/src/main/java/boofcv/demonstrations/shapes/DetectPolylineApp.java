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

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.abst.shapes.polyline.NewSplitMerge_to_PointsToPolyline;
import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.struct.ConfigLength;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

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
public class DetectPolylineApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase implements ShapeGuiListener
{
	LinearContourLabelChang2004 binaryToContour = new LinearContourLabelChang2004(ConnectRule.FOUR);
	PointsToPolyline contourToPolyline;

	List<List<Point2D_I32>> polylines = new ArrayList<>();
	GrayS32 labeled = new GrayS32(1,1);
	List<Contour> contours;
	ConfigLength minimumContourSize;

	public DetectPolylineApp(List<String> examples , Class<T> imageType) {
		super(examples, imageType);

		setupGui(new VisualizePanel(),new DetectPolygonControlPanel(this));
	}

	@Override
	protected void createDetector(boolean initializing) {
		if( !initializing)
			BoofSwingUtil.checkGuiThread();

		DetectPolygonControlPanel controls = (DetectPolygonControlPanel)DetectPolylineApp.this.controls;

		synchronized (this) {
			ConfigPolygonDetector config = controls.getConfigPolygon();

			if( controls.bRefineGray ) {
				config.refineGray = controls.refineGray;
			} else {
				config.refineGray = null;
			}

			ConfigPolylineSplitMerge c = new ConfigPolylineSplitMerge();
			c.minSides = config.detector.minimumSides;
			c.maxSides = config.detector.maximumSides;
			c.convex = config.detector.convex;
			c.cornerScorePenalty = 0.1;
			c.thresholdSideSplitScore = 0;//0.15;
//			c.maxNumberOfSideSamples = 50;
			c.minimumSideLength = 4;

			minimumContourSize = config.detector.minimumContour;
			contourToPolyline = new NewSplitMerge_to_PointsToPolyline(c);
		}
		imageThresholdUpdated();
	}


	public void configUpdate() {
		createDetector(false);
		// does process and render too
	}

	@Override
	public void imageThresholdUpdated() {
		DetectPolygonControlPanel controls = (DetectPolygonControlPanel)DetectPolylineApp.this.controls;

		ConfigThreshold config = controls.getThreshold().createConfig();

		synchronized (this) {
			inputToBinary = FactoryThresholdBinary.threshold(config, imageClass);
		}
		reprocessImageOnly();
	}

	int count = 0;
	@Override
	protected void detectorProcess(ImageGray input, GrayU8 binary) {
//		System.out.println("processing image "+count++);
		binaryToContour.process(binary,labeled);

		contours = BinaryImageOps.convertContours(binaryToContour.getPackedPoints(),binaryToContour.getContours());

		int minContourPixels = minimumContourSize.computeI(Math.min(input.width,input.height));

		polylines.clear();
		GrowQueue_I32 indices = new GrowQueue_I32();
		for (int i = 0; i < contours.size(); i++) {
			List<Point2D_I32> contour = contours.get(i).external;
			if( contour.size() < minContourPixels )
				continue;
			if( contourToPolyline.process(contour,indices) ) {
				List<Point2D_I32> l = new ArrayList<>();
				for (int j = 0; j < indices.size; j++) {
					l.add( contour.get( indices.get(j)));
				}
				polylines.add(l);
			}
		}
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {

			DetectPolygonControlPanel controls = (DetectPolygonControlPanel)DetectPolylineApp.this.controls;

			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized ( DetectPolylineApp.this ) {

				if (controls.bShowContour && contours != null ) {
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null,Color.CYAN, scale, g2);
				}

				if (controls.bShowLines) {
					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));
					for (List<Point2D_I32> p : polylines) {
						int red = 255 * ((p.size() - 3) % 4) / 3;
						int green = 255 * ((p.size()) % 5) / 4;
						int blue = 255 * ((p.size() + 2) % 6) / 5;

						g2.setColor(new Color(red, green, blue));

						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowCorners) {
					g2.setColor(Color.BLUE);
					g2.setStroke(new BasicStroke(1));
					for (List<Point2D_I32> p : polylines) {
						for (int i = 0; i < p.size(); i++) {
							Point2D_I32 c = p.get(i);
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
		examples.add("shapes/polygons_border_01.jpg");
//		examples.add("shapes/shapes01.png"); This is a pathological case specifically design to screw up threshold algs
		examples.add("shapes/shapes02.png");
		examples.add("shapes/concave01.jpg");
		examples.add("shapes/line_text_test_image.png");
		examples.add("fiducial/binary/image0000.jpg");
		examples.add("calibration/stereo/Bumblebee2_Square/left10.jpg");
		examples.add("fiducial/square_grid/movie.mp4");

		DetectPolylineApp app = new DetectPolylineApp(examples,GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.waitUntilInputSizeIsKnown();
		app.display("Detect Black Polygons");
	}



}
