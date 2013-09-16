/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.interest.ConfigFast;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.alg.feature.detect.ImageCorruptPanel;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.BoofDefaults;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I16;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a window showing the selected point features.  Local maximums and minimums are shown using different
 * colors.
 *
 * @author Peter Abeles
 */
public class DetectFeaturePointApp<T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel implements ImageCorruptPanel.Listener {

	static int maxFeatures = 400;
	static int maxScaleFeatures = maxFeatures / 3;

	public double[] scales = new double[]{1, 1.5, 2, 3, 4, 6, 8, 12};
	int radius = 2;
	float thresh = 1;
	T grayImage;
	T corruptImage;
	Class<T> imageType;
	boolean processImage = false;
	BufferedImage input;
	BufferedImage workImage;
	FancyInterestPointRender render = new FancyInterestPointRender();

	ImagePanel panel;
	ImageCorruptPanel corruptPanel;

	public DetectFeaturePointApp(Class<T> imageType, Class<D> derivType) {
		super(1);
		this.imageType = imageType;


		GeneralFeatureDetector<T, D> alg;
		ConfigGeneralDetector configExtract = new ConfigGeneralDetector(maxFeatures,radius,thresh);

		alg = FactoryDetectPoint.createHarris(configExtract, false, derivType);


		addAlgorithm(0, "Harris", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));
		alg = FactoryDetectPoint.createHarris(configExtract, true, derivType);
		addAlgorithm(0, "Harris Weighted", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));
		alg = FactoryDetectPoint.createShiTomasi(configExtract, false, derivType);
		addAlgorithm(0, "Shi-Tomasi", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));
		alg = FactoryDetectPoint.createShiTomasi(configExtract, true, derivType);
		addAlgorithm(0, "Shi-Tomasi Weighted", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));
		alg = FactoryDetectPoint.createFast(
				new ConfigFast(10,9),new ConfigGeneralDetector(maxFeatures,radius,10), imageType);
		addAlgorithm(0, "Fast", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));
		alg = FactoryDetectPoint.createKitRos(configExtract, derivType);
		addAlgorithm(0, "KitRos", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));
		alg = FactoryDetectPoint.createMedian(configExtract, imageType);
		addAlgorithm(0, "Median", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));
		alg = FactoryDetectPoint.createHessian(HessianBlobIntensity.Type.DETERMINANT, configExtract, derivType);
		addAlgorithm(0, "Hessian", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));
		alg = FactoryDetectPoint.createHessian(HessianBlobIntensity.Type.TRACE, configExtract, derivType);
		addAlgorithm(0, "Laplace", new EasyGeneralFeatureDetector<T,D>(alg,imageType,derivType));

		JPanel viewArea = new JPanel(new BorderLayout());
		corruptPanel = new ImageCorruptPanel();
		corruptPanel.setListener(this);
		panel = new ImagePanel();

		viewArea.add(corruptPanel, BorderLayout.WEST);
		viewArea.add(panel, BorderLayout.CENTER);
		setMainGUI(viewArea);
	}

	public void process(BufferedImage input) {
		setInputImage(input);
		this.input = input;
		grayImage = ConvertBufferedImage.convertFromSingle(input, null, imageType);
		corruptImage = (T) grayImage._createNew(grayImage.width, grayImage.height);
		workImage = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_BGR);
		panel.setBufferedImage(workImage);
		panel.setPreferredSize(new Dimension(workImage.getWidth(), workImage.getHeight()));
		doRefreshAll();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				revalidate();
				processImage = true;
			}
		});
	}

	@Override
	public void loadConfigurationFile(String fileName) {
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0, null, cookies[0]);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if (input == null)
			return;

		// corrupt the input image
		corruptPanel.corruptImage(grayImage, corruptImage);

		final EasyGeneralFeatureDetector<T,D> det = (EasyGeneralFeatureDetector<T,D>) cookie;
		det.detect(corruptImage,null);

		double detectorRadius = BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS;

		render.reset();
		if( det.getDetector().isDetectMinimums() ) {
			QueueCorner l = det.getMinimums();
			for( int i = 0; i < l.size; i++ ) {
				Point2D_I16 p = l.get(i);
				render.addPoint(p.x, p.y, 3, Color.BLUE);
			}
		}
		if( det.getDetector().isDetectMaximums() ) {
			QueueCorner l = det.getMaximums();
			for( int i = 0; i < l.size; i++ ) {
				Point2D_I16 p = l.get(i);
				render.addPoint(p.x, p.y, 3, Color.RED);
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ConvertBufferedImage.convertTo(corruptImage, workImage,true);
				Graphics2D g2 = workImage.createGraphics();
				g2.setStroke(new BasicStroke(3));
				render.draw(g2);
				panel.repaint();
			}
		});
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if (image != null) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processImage;
	}

	@Override
	public synchronized void corruptImageChange() {
		doRefreshAll();
	}

	public static void main(String args[]) {
		DetectFeaturePointApp app = new DetectFeaturePointApp(ImageFloat32.class, ImageFloat32.class);
//		DetectFeaturePointApp app = new DetectFeaturePointApp(ImageUInt8.class,ImageSInt16.class);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("shapes", "../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("amoeba","../data/evaluation/amoeba_shapes.jpg"));
		inputs.add(new PathLabel("sunflowers", "../data/evaluation/sunflowers.png"));
		inputs.add(new PathLabel("beach", "../data/evaluation/scale/beach02.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Point Feature");
	}
}
