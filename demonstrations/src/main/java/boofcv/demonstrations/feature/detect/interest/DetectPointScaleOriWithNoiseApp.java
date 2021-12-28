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

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.detect.interest.FeatureLaplacePyramid;
import boofcv.demonstrations.feature.detect.ImageCorruptPanel;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
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
@SuppressWarnings({"NullAway.Init"})
public class DetectPointScaleOriWithNoiseApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends SelectAlgorithmAndInputPanel implements ImageCorruptPanel.Listener {

	static int maxFeatures = 400;
	static int maxScaleFeatures = maxFeatures/3;

	public double[] scales = new double[]{1, 1.5, 2, 3, 4, 6, 8, 10, 12, 14, 16, 18};
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

	public DetectPointScaleOriWithNoiseApp( Class<T> imageType, Class<D> derivType ) {
		super(1);
		this.imageType = imageType;

		FeatureLaplacePyramid<T, D> flss = FactoryInterestPointAlgs.hessianLaplace(radius, thresh, maxScaleFeatures, imageType, derivType);
		addAlgorithm(0, "Hess Lap SS", FactoryInterestPoint.wrapDetector(flss, scales, false, imageType));
		FeatureLaplacePyramid<T, D> flp = FactoryInterestPointAlgs.hessianLaplace(radius, thresh, maxScaleFeatures, imageType, derivType);
		addAlgorithm(0, "Hess Lap P", FactoryInterestPoint.wrapDetector(flp, scales, true, imageType));
		addAlgorithm(0, "FastHessian", FactoryInterestPoint.<T>fastHessian(
				new ConfigFastHessian(thresh, 2, maxScaleFeatures, 2, 9, 4, 4), imageType));
		addAlgorithm(0, "SIFT", FactoryInterestPoint.sift(null, new ConfigSiftDetector(2*maxScaleFeatures), imageType));

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

		final InterestPointDetector<T> det = (InterestPointDetector<T>)cookie;
		det.detect(corruptImage);

		render.reset();
		for (int i = 0; i < det.getNumberOfFeatures(); i++) {
			Point2D_F64 p = det.getLocation(i);
			int radius = (int)Math.ceil(det.getRadius(i));
			render.addCircle((int)p.x, (int)p.y, radius);
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
		DetectPointScaleOriWithNoiseApp app = new DetectPointScaleOriWithNoiseApp(GrayF32.class, GrayF32.class);
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

		ShowImages.showWindow(app, "Point Feature", true);
	}
}
