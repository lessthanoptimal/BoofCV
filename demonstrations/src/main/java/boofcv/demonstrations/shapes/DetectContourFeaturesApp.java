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

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.polyline.keypoint.ContourInterestPointDetector;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays information on detected contour interest points/features
 *
 * @author Peter Abeles
 */
	// TODO contour 4 vs 8 connect
public class DetectContourFeaturesApp<T extends ImageGray<T>>
		extends DetectBlackShapeAppBase
{
	ContourInterestPointDetector detector;

	List<Contour> contours;
	List<GrowQueue_I32> features;

	public DetectContourFeaturesApp(List<String> examples , Class<T> imageType) {
		super(examples, imageType);

		setupGui(new VisualizePanel(),new DetectContourFeatureControlPanel(this));
	}

	@Override
	protected void createDetector(boolean initializing) {
		if( !initializing)
			BoofSwingUtil.checkGuiThread();

		DetectContourFeatureControlPanel controls = (DetectContourFeatureControlPanel)DetectContourFeaturesApp.this.controls;

		synchronized (this) {
			detector = new ContourInterestPointDetector(true,controls.period,controls.featureThreshold);
		}
		imageThresholdUpdated();
	}


	public void configUpdate() {
		createDetector(false);
		// does process and render too
	}

	@Override
	public void imageThresholdUpdated() {
		DetectContourFeatureControlPanel controls = (DetectContourFeatureControlPanel)DetectContourFeaturesApp.this.controls;

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
		DetectContourFeatureControlPanel controls = (DetectContourFeatureControlPanel)DetectContourFeaturesApp.this.controls;

		contours = BinaryImageOps.contour(binary, controls.connectRule,null);

		synchronized ( DetectContourFeaturesApp.this ) {
			features = new ArrayList<>();
			for (int i = 0; i < contours.size(); i++) {
				detector.process(contours.get(i).external);
				features.add(detector.getIndexes().copy());
			}
		}
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {

			DetectContourFeatureControlPanel controls = (DetectContourFeatureControlPanel)DetectContourFeaturesApp.this.controls;

			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized ( DetectContourFeaturesApp.this ) {
				if( contours == null || features == null )
					return;


				if (controls.bShowContour) {
					g2.setStroke(new BasicStroke(1));

					VisualizeBinaryData.render(contours, null,Color.CYAN, scale, g2);
				}

				if (controls.bShowLines) {
					Line2D.Double l = new Line2D.Double();
					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));

					for (int which = 0; which < contours.size(); which++) {
						List<Point2D_I32> contour = contours.get(which).external;
						GrowQueue_I32 f = features.get(which);

						for (int i = 0,j=f.size-1; i < f.size; j=i,i++) {
							Point2D_I32 a = contour.get(f.get(i));
							Point2D_I32 b = contour.get(f.get(j));
							VisualizeFiducial.drawLine(g2,l,a.x*scale,a.y*scale,b.x*scale,b.y*scale);
						}

					}
				}

				if (controls.bShowFeatures) {
					BasicStroke strokeThin = new BasicStroke(2);
					BasicStroke strokeThick = new BasicStroke(3);

					for (int which = 0; which < contours.size(); which++) {
						List<Point2D_I32> contour = contours.get(which).external;
						GrowQueue_I32 f = features.get(which);

						for (int i = 0; i < f.size; i++) {
							Point2D_I32 p = contour.get(f.get(i));

							g2.setColor(Color.WHITE);
							g2.setStroke(strokeThick);
							VisualizeFeatures.drawCircle(g2, scale * (p.x), scale * (p.y), 5);
							g2.setColor(Color.BLUE);
							g2.setStroke(strokeThin);
							VisualizeFeatures.drawCircle(g2, scale * (p.x), scale * (p.y), 5);
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
		examples.add("shapes/shapes02.png");
		examples.add("shapes/concave01.jpg");
		examples.add("shapes/line_text_test_image.png");
		examples.add("fiducial/binary/image0000.jpg");
		examples.add("calibration/stereo/Bumblebee2_Square/left10.jpg");
		examples.add("fiducial/square_grid/movie.mp4");

		DetectContourFeaturesApp app = new DetectContourFeaturesApp(examples,GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.waitUntilInputSizeIsKnown();
		app.display("Contour Feature Points");
	}



}
