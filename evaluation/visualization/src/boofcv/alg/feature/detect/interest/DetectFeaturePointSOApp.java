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

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.detect.ImageCorruptPanel;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a window point features with scale and orientation information
 *
 * @author Peter Abeles
 */
public class DetectFeaturePointSOApp<T extends ImageSingleBand, D extends ImageSingleBand>
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

	public DetectFeaturePointSOApp(Class<T> imageType, Class<D> derivType) {
		super(1);
		this.imageType = imageType;

		FeatureLaplacePyramid<T, D> flss = FactoryInterestPointAlgs.hessianLaplace(radius, thresh, maxScaleFeatures, imageType, derivType);
		addAlgorithm(0, "Hess Lap SS", FactoryInterestPoint.wrapDetector(flss, scales, false, imageType));
		FeatureLaplacePyramid<T, D> flp = FactoryInterestPointAlgs.hessianLaplace(radius, thresh, maxScaleFeatures, imageType, derivType);
		addAlgorithm(0, "Hess Lap P", FactoryInterestPoint.wrapDetector(flp, scales, true,imageType));
		addAlgorithm(0, "FastHessian", FactoryInterestPoint.<T>fastHessian(
				new ConfigFastHessian(thresh, 2, maxScaleFeatures, 2, 9, 4, 4)));
		if( imageType == ImageFloat32.class )
			addAlgorithm(0, "SIFT", FactoryInterestPoint.siftDetector(null,new ConfigSiftDetector(2,10,maxScaleFeatures,5)));

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

		final InterestPointDetector<T> det = (InterestPointDetector<T>) cookie;
		det.detect(corruptImage);

		double detectorRadius = BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS;

		render.reset();
		for (int i = 0; i < det.getNumberOfFeatures(); i++) {
			Point2D_F64 p = det.getLocation(i);
			int radius = (int) Math.ceil(det.getScale(i) * detectorRadius);
			render.addCircle((int) p.x, (int) p.y, radius);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ConvertBufferedImage.convertTo(corruptImage, workImage, true);
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
		DetectFeaturePointSOApp app = new DetectFeaturePointSOApp(ImageFloat32.class, ImageFloat32.class);
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
