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

import boofcv.abst.feature.detect.interest.InterestPointScaleSpacePyramid;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.ScaleSpacePyramidPointPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidFloat;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Displays a window showing selected features across scale spaces using a pyramid as input.
 *
 * @author Peter Abeles
 */
public class DetectPointsInsidePyramidApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends SelectAlgorithmAndInputPanel {
	static int NUM_FEATURES = 100;

	@Nullable PyramidFloat<T> ss;
	Class<T> imageType;
	ScaleSpacePyramidPointPanel panel;
	boolean hasImage = false;

	T workImage;
	@Nullable InterestPointScaleSpacePyramid<T> det = null;

	public DetectPointsInsidePyramidApp( Class<T> imageType, Class<D> derivType ) {
		super(2);
		this.imageType = imageType;

		workImage = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		int r = 2;
		addAlgorithm(0, "Hessian Laplace", FactoryInterestPointAlgs.hessianLaplace(r, 0, NUM_FEATURES, imageType, derivType));
		addAlgorithm(0, "Harris Laplace", FactoryInterestPointAlgs.harrisLaplace(r, 0, NUM_FEATURES, imageType, derivType));
		addAlgorithm(0, "Hessian", FactoryInterestPointAlgs.hessianPyramid(r, 0, NUM_FEATURES, imageType, derivType));
		addAlgorithm(0, "Harris", FactoryInterestPointAlgs.harrisPyramid(r, 0, NUM_FEATURES, imageType, derivType));

		addAlgorithm(1, "Pyramid", 0);
		addAlgorithm(1, "Scale-Space", 1);


		panel = new ScaleSpacePyramidPointPanel(2.5);

		setMainGUI(panel);
	}

	public synchronized void process( BufferedImage input ) {
		setInputImage(input);
		workImage.reshape(input.getWidth(), input.getHeight());
		ConvertBufferedImage.convertFromSingle(input, workImage, imageType);

		panel.setBackground(input);
		hasImage = true;
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile( String fileName ) {}

	@Override
	public void refreshAll( Object[] cookies ) {
		det = null;
		ss = null;

		setActiveAlgorithm(0, null, cookies[0]);
		setActiveAlgorithm(1, null, cookies[1]);
	}

	@Override
	public synchronized void setActiveAlgorithm( int indexFamily, @Nullable String name, Object cookie ) {
		if (!hasImage)
			return;

		if (indexFamily == 0) {
			det = (InterestPointScaleSpacePyramid<T>)cookie;

			if (ss == null)
				return;
		} else {
			double[] scales = new double[]{1, 1.5, 2, 3, 4, 8, 12, 16, 24};
			if (((Number)cookie).intValue() == 0)
				ss = FactoryPyramid.scaleSpacePyramid(scales, imageType);
			else
				ss = FactoryPyramid.scaleSpace(scales, imageType);

			if (workImage != null)
				ss.process(workImage);

			panel.setSs(ss);
			if (det == null)
				return;
		}

		det.detect(ss);
		panel.setPoints(det.getInterestPoints());
		panel.repaint();
		panel.requestFocusInWindow();
	}

	@Override
	public synchronized void changeInput( String name, int index ) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if (image != null) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return hasImage;
	}

	public static void main( String[] args ) {

		DetectPointsInsidePyramidApp app = new DetectPointsInsidePyramidApp(GrayF32.class, GrayF32.class);

		java.util.List<PathLabel> inputs = new ArrayList<>();

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

		ShowImages.showWindow(app, "Pyramid Point Detection", true);
	}
}
