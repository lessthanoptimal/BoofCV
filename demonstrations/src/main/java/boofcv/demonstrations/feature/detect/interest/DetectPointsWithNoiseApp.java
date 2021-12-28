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

package boofcv.demonstrations.feature.detect.interest;

import boofcv.abst.feature.detect.interest.ConfigFastCorner;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.ConfigHarrisCorner;
import boofcv.abst.feature.detect.interest.ConfigShiTomasi;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.EasyGeneralFeatureDetector;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.demonstrations.feature.detect.ImageCorruptPanel;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I16;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a window showing the selected point features. Local maximums and minimums are shown using different
 * colors.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectPointsWithNoiseApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends SelectAlgorithmAndInputPanel implements ImageCorruptPanel.Listener {

	static int maxFeatures = 400;
	static int maxScaleFeatures = maxFeatures/3;

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

	public DetectPointsWithNoiseApp( Class<T> imageType, Class<D> derivType ) {
		super(1);
		this.imageType = imageType;


		GeneralFeatureDetector<T, D> alg;
		ConfigGeneralDetector configExtract = new ConfigGeneralDetector(maxFeatures, radius, thresh);


		alg = FactoryDetectPoint.createHarris(configExtract, null, derivType);
		addAlgorithm(0, "Harris", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));
		alg = FactoryDetectPoint.createHarris(configExtract, new ConfigHarrisCorner(true, radius), derivType);
		addAlgorithm(0, "Harris Weighted", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));
		alg = FactoryDetectPoint.createShiTomasi(configExtract, null, derivType);
		addAlgorithm(0, "Shi-Tomasi", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));
		alg = FactoryDetectPoint.createShiTomasi(configExtract, new ConfigShiTomasi(true, radius), derivType);
		addAlgorithm(0, "Shi-Tomasi Weighted", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));
		configExtract.detectMinimums = true;
		configExtract.threshold = 10;
		alg = FactoryDetectPoint.createFast(configExtract, new ConfigFastCorner(10, 9), imageType);
		configExtract.detectMinimums = false;
		configExtract.threshold = thresh;
		addAlgorithm(0, "Fast", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));
		alg = FactoryDetectPoint.createKitRos(configExtract, derivType);
		addAlgorithm(0, "KitRos", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));
		alg = FactoryDetectPoint.createMedian(configExtract, imageType);
		addAlgorithm(0, "Median", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));
		alg = FactoryDetectPoint.createHessianDeriv(configExtract, HessianBlobIntensity.Type.DETERMINANT, derivType);
		addAlgorithm(0, "Hessian", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));
		configExtract.detectMinimums = true;
		alg = FactoryDetectPoint.createHessianDeriv(configExtract, HessianBlobIntensity.Type.TRACE, derivType);
		addAlgorithm(0, "Laplace", new EasyGeneralFeatureDetector<>(alg, imageType, derivType));

		JPanel viewArea = new JPanel(new BorderLayout());
		corruptPanel = new ImageCorruptPanel();
		corruptPanel.setListener(this);
		panel = new ImagePanel();

		viewArea.add(corruptPanel, BorderLayout.WEST);
		viewArea.add(panel, BorderLayout.CENTER);
		setMainGUI(viewArea);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);
		this.input = input;
		grayImage = ConvertBufferedImage.convertFromSingle(input, null, imageType);
		corruptImage = (T)grayImage.createNew(grayImage.width, grayImage.height);
		workImage = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_BGR);
		panel.setImage(workImage);
		panel.setPreferredSize(new Dimension(workImage.getWidth(), workImage.getHeight()));
		doRefreshAll();

		SwingUtilities.invokeLater(() -> {
			revalidate();
			processImage = true;
		});
	}

	@Override
	public void loadConfigurationFile( String fileName ) {
	}

	@Override
	public void refreshAll( Object[] cookies ) {
		setActiveAlgorithm(0, "", cookies[0]);
	}

	@Override
	public synchronized void setActiveAlgorithm( int indexFamily, String name, Object cookie ) {
		if (input == null)
			return;

		// corrupt the input image
		corruptPanel.corruptImage(grayImage, corruptImage);

		final EasyGeneralFeatureDetector<T, D> det = (EasyGeneralFeatureDetector<T, D>)cookie;
		det.detect(corruptImage, null);

		render.reset();
		if (det.getDetector().isDetectMinimums()) {
			QueueCorner l = det.getMinimums();
			for (int i = 0; i < l.size; i++) {
				Point2D_I16 p = l.get(i);
				render.addPoint(p.x, p.y, 3, Color.BLUE);
			}
		}
		if (det.getDetector().isDetectMaximums()) {
			QueueCorner l = det.getMaximums();
			for (int i = 0; i < l.size; i++) {
				Point2D_I16 p = l.get(i);
				render.addPoint(p.x, p.y, 3, Color.RED);
			}
		}

		SwingUtilities.invokeLater(() -> {
			ConvertBufferedImage.convertTo(corruptImage, workImage, true);
			Graphics2D g2 = workImage.createGraphics();
			g2.setStroke(new BasicStroke(3));
			render.draw(g2);
			panel.repaint();
		});
	}

	@Override
	public void changeInput( String name, int index ) {
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

	public static void main( String[] args ) {
		DetectPointsWithNoiseApp app = new DetectPointsWithNoiseApp(GrayF32.class, GrayF32.class);
//		DetectPointsWithNoiseApp app = new DetectPointsWithNoiseApp(GrayU8.class,GrayS16.class);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Square Grid", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Square/frame06.jpg")));
		inputs.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));
		inputs.add(new PathLabel("amoeba", UtilIO.pathExample("amoeba_shapes.jpg")));
		inputs.add(new PathLabel("beach", UtilIO.pathExample("scale/beach02.jpg")));
		inputs.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			BoofMiscOps.sleep(10);
		}

		ShowImages.showWindow(app, "Detect Scale Point" +
				" with Noise", true);
	}
}
