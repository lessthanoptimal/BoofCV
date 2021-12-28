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

import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryBinaryContourFinder;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigSplitMergeLineFit;
import boofcv.factory.shape.FactoryPointsToPolyline;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.UtilIO;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray_I32;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Application which lets you select and configure different polyline fitting algorithms
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectPolylineApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase implements ShapeGuiListener {
	PolylineAppControlPanel controlPanel = new PolylineAppControlPanel(this);

	BinaryLabelContourFinder binaryToContour = FactoryBinaryContourFinder.linearChang2004();
	PointsToPolyline contourToPolyline;

	List<List<Point2D_I32>> polylines = new ArrayList<>();
	GrayS32 labeled = new GrayS32(1, 1);
	List<Contour> contours;
	ConfigLength minimumContourSize;

	public DetectPolylineApp( List<String> examples, Class<T> imageType ) {
		super(examples, imageType);

		controlPanel.threshold.addHistogramGraph();
		setupGui(new VisualizePanel(), controlPanel);
	}

	@Override
	protected void createDetector( boolean initializing ) {
		if (!initializing)
			BoofSwingUtil.checkGuiThread();

		PolylineAppControlPanel controls = (PolylineAppControlPanel)DetectPolylineApp.this.controls;
		PolylineControlPanel polyControls = controls.polylinePanel;

		minimumContourSize = ConfigLength.fixed(controls.minimumContourSize);

		binaryToContour.setConnectRule(controls.connectRule);

		synchronized (this) {
			switch (polyControls.whichAlgorithm) {
				case 0: {
					ConfigPolylineSplitMerge config = polyControls.getConfigSplitMerge();
					contourToPolyline = FactoryPointsToPolyline.splitMerge(config);
				}
				break;

				case 1: {
					ConfigSplitMergeLineFit config = polyControls.getConfigSplitMergeOld();
					contourToPolyline = FactoryPointsToPolyline.splitMerge(config);
				}
				break;

				default:
					throw new RuntimeException("Egads");
			}
			contourToPolyline.setMinimumSides(polyControls.minSides);
			contourToPolyline.setMaximumSides(polyControls.maxSides);
			contourToPolyline.setConvex(polyControls.convex);
		}
		imageThresholdUpdated();
	}

	@Override
	public void configUpdate() {
		createDetector(false);
		// does process and render too
	}

	@Override
	public void imageThresholdUpdated() {
		PolylineAppControlPanel controls = (PolylineAppControlPanel)DetectPolylineApp.this.controls;

		ConfigThreshold config = controls.getThreshold().createConfig();

		synchronized (this) {
			inputToBinary = FactoryThresholdBinary.threshold(config, imageClass);
		}
		reprocessImageOnly();
	}

	int count = 0;

	@Override
	protected void detectorProcess( ImageGray input, GrayU8 binary ) {
//		System.out.println("processing image "+count++);
		binaryToContour.process(binary, labeled);

		contours = BinaryImageOps.convertContours(binaryToContour);

		int minContourPixels = minimumContourSize.computeI(Math.min(input.width, input.height));

		polylines.clear();
		DogArray_I32 indices = new DogArray_I32();
		for (int i = 0; i < contours.size(); i++) {
			List<Point2D_I32> contour = contours.get(i).external;
			if (contour.size() < minContourPixels)
				continue;
			if (contourToPolyline.process(contour, indices)) {
				List<Point2D_I32> l = new ArrayList<>();
				for (int j = 0; j < indices.size; j++) {
					l.add(contour.get(indices.get(j)));
				}
				polylines.add(l);
			}
		}

		// this is thread safe
		controlPanel.threshold.updateHistogram(input);
	}

	class VisualizePanel extends ShapeVisualizePanel {
		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			BoofSwingUtil.antialiasing(g2);

			PolylineAppControlPanel controls = (PolylineAppControlPanel)DetectPolylineApp.this.controls;


			synchronized (DetectPolylineApp.this) {

				if (controls.bShowContour && contours != null) {
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null, Color.CYAN, 1.0, scale, g2);
				}

				if (controls.bShowLines) {
					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));
					for (List<Point2D_I32> p : polylines) {
						int red = 255*((p.size() - 3)%4)/3;
						int green = 255*(p.size()%5)/4;
						int blue = 255*((p.size() + 2)%6)/5;

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

		DetectPolylineApp app = new DetectPolylineApp(examples, GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.waitUntilInputSizeIsKnown();
		app.display("Detect Polylines");
	}
}
