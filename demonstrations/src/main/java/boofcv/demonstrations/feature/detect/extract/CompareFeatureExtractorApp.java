/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.detect.extract;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.detect.selector.FeatureSelectLimit;
import boofcv.alg.feature.detect.selector.FeatureSelectNBest;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I16;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Displays a window showing the selected corner-laplace features across different scale spaces.
 *
 * @author Peter Abeles
 */
public class CompareFeatureExtractorApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends SelectAlgorithmAndInputPanel implements GeneralExtractConfigPanel.Listener {
	T grayImage;

	Class<T> imageType;

	GeneralExtractConfigPanel configPanel;

	boolean processImage = false;
	BufferedImage input;
	BufferedImage intensityImage;
	BufferedImage workImage;
	AnyImageDerivative<T, D> deriv;

	GeneralFeatureIntensity<T, D> intensityAlg;
	int minSeparation = 5;

	// which image is being viewed in the GUI
	int viewImage = 2;

	int radius = 2;
	int numFeatures = 200;
	float thresholdFraction = 0.1f;

	FancyInterestPointRender render = new FancyInterestPointRender();

	ImagePanel imagePanel;

	public CompareFeatureExtractorApp(Class<T> imageType, Class<D> derivType) {
		super(1);
		this.imageType = imageType;

		addAlgorithm(0, "Harris", FactoryIntensityPoint.harris(radius, 0.04f, false, imageType));
		addAlgorithm(0, "KLT", FactoryIntensityPoint.shiTomasi(radius, false, derivType));
		addAlgorithm(0, "FAST", FactoryIntensityPoint.fast(5, 11, derivType));
		addAlgorithm(0, "KitRos", FactoryIntensityPoint.kitros(derivType));
		addAlgorithm(0, "Laplace Det", FactoryIntensityPoint.hessian(HessianBlobIntensity.Type.DETERMINANT, derivType));
		addAlgorithm(0, "Laplace Trace", FactoryIntensityPoint.hessian(HessianBlobIntensity.Type.TRACE, derivType));

		deriv = GImageDerivativeOps.createAnyDerivatives(DerivativeType.SOBEL,imageType, derivType);

		JPanel gui = new JPanel();
		gui.setLayout(new BorderLayout());

		imagePanel = new ImagePanel();

		this.configPanel = new GeneralExtractConfigPanel();
		configPanel.setThreshold(thresholdFraction);
		configPanel.setFeatureSeparation(minSeparation);
		configPanel.setImageIndex(viewImage);
		configPanel.setListener(this);

		gui.add(configPanel, BorderLayout.WEST);
		gui.add(imagePanel, BorderLayout.CENTER);

		setMainGUI(gui);
	}

	public void process(BufferedImage input) {
		this.input = input;
		grayImage = ConvertBufferedImage.convertFromSingle(input, null, imageType);
		workImage = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_BGR);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				doRefreshAll();
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
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if (input == null)
			return;

		intensityAlg = (GeneralFeatureIntensity<T, D>) cookie;

		doProcess();
	}

	private synchronized void doProcess() {
//		System.out.println("radius "+radius+" min separation "+minSeparation+" thresholdFraction "+thresholdFraction+" numFeatures "+numFeatures);

		deriv.setInput(grayImage);
		D derivX = deriv.getDerivative(true);
		D derivY = deriv.getDerivative(false);
		D derivXX = deriv.getDerivative(true, true);
		D derivYY = deriv.getDerivative(false, false);
		D derivXY = deriv.getDerivative(true, false);

		// todo modifying buffered images which might be actively being displayed, could mess up swing

		intensityAlg.process(grayImage, derivX, derivY, derivXX, derivYY, derivXY);
		GrayF32 intensity = intensityAlg.getIntensity();
		intensityImage = VisualizeImageData.colorizeSign(
				intensityAlg.getIntensity(), null, ImageStatistics.maxAbs(intensity));

		float max = ImageStatistics.maxAbs(intensity);
		float threshold = max * thresholdFraction;

		FeatureSelectLimit selector = new FeatureSelectNBest();

		NonMaxSuppression extractor =
				FactoryFeatureExtractor.nonmax(new ConfigExtract(minSeparation, threshold, radius, true));
		GeneralFeatureDetector<T, D> detector = new GeneralFeatureDetector<>(intensityAlg, extractor, selector);
		detector.setMaxFeatures(numFeatures);
		detector.process(grayImage, derivX, derivY, derivXX, derivYY, derivXY);
		QueueCorner foundCorners = detector.getMaximums();

		render.reset();

		for (int i = 0; i < foundCorners.size(); i++) {
			Point2D_I16 p = foundCorners.get(i);
			render.addPoint(p.x, p.y, 3, Color.RED);
		}

		Graphics2D g2 = workImage.createGraphics();
		g2.drawImage(input, 0, 0, grayImage.width, grayImage.height, null);
		render.draw(g2);
		drawImage();

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
	public void changeImage(int index) {
		this.viewImage = index;
		drawImage();
	}

	private void drawImage() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				switch (viewImage) {
					case 0:
						imagePanel.setImage(input);
						break;

					case 1:
						imagePanel.setImage(intensityImage);
						break;

					case 2:
						imagePanel.setImage(workImage);
						break;
				}
				BufferedImage b = imagePanel.getImage();
				imagePanel.setPreferredSize(new Dimension(b.getWidth(), b.getHeight()));
				imagePanel.repaint();

				processImage = true;
			}
		});
	}

	@Override
	public synchronized void changeFeatureSeparation(int radius) {
		minSeparation = radius;
		doProcess();
	}

	@Override
	public synchronized void changeThreshold(double value) {
		this.thresholdFraction = (float) value;
		doProcess();
	}

	@Override
	public synchronized void changeNumFeatures(int total) {
		this.numFeatures = total;
		doProcess();
	}

	public static void main(String args[]) {
		CompareFeatureExtractorApp app = new CompareFeatureExtractorApp(GrayF32.class, GrayF32.class);

		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		inputs.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));
		inputs.add(new PathLabel("beach", UtilIO.pathExample("scale/beach02.jpg")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Feature Extraction", true);

		System.out.println("Done");
	}
}
