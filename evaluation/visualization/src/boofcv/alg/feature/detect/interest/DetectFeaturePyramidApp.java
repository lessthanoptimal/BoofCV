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

import boofcv.abst.feature.detect.interest.InterestPointScaleSpacePyramid;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.feature.ScaleSpacePyramidPointPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidFloat;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Displays a window showing selected features across scale spaces using a pyramid as input.
 *
 * @author Peter Abeles
 */
public class DetectFeaturePyramidApp <T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel
{
	static int NUM_FEATURES = 100;

	PyramidFloat<T> ss;
	Class<T> imageType;
	ScaleSpacePyramidPointPanel panel;
	boolean hasImage = false;

	T workImage;
	InterestPointScaleSpacePyramid<T> det = null;

	public DetectFeaturePyramidApp( Class<T> imageType , Class<D> derivType ) {
		super(2);
		this.imageType = imageType;

		workImage = GeneralizedImageOps.createSingleBand(imageType,1,1);

		int r = 2;
		addAlgorithm(0, "Hessian Laplace", FactoryInterestPointAlgs.hessianLaplace(r, 0, NUM_FEATURES, imageType, derivType));
		addAlgorithm(0, "Harris Laplace", FactoryInterestPointAlgs.harrisLaplace(r, 0, NUM_FEATURES, imageType, derivType));
		addAlgorithm(0, "Hessian", FactoryInterestPointAlgs.hessianPyramid(r, 0, NUM_FEATURES, imageType, derivType));
		addAlgorithm(0, "Harris", FactoryInterestPointAlgs.harrisPyramid(r, 0, NUM_FEATURES, imageType, derivType));

		addAlgorithm(1 , "Pyramid", 0);
		addAlgorithm(1 , "Scale-Space", 1);


		panel = new ScaleSpacePyramidPointPanel(BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS);

		setMainGUI(panel);
	}

	public synchronized void process( BufferedImage input ) {
		setInputImage(input);
		workImage.reshape(input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFromSingle(input, workImage, imageType);

		panel.setBackground(input);
		hasImage = true;
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		det = null;
		ss = null;

		setActiveAlgorithm(0,null,cookies[0]);
		setActiveAlgorithm(1,null,cookies[1]);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( !hasImage )
			return;

		if( indexFamily == 0 ) {
			det = (InterestPointScaleSpacePyramid<T>)cookie;

			if( ss == null )
				return;
		} else {
			double scales[] = new double[]{1,1.5,2,3,4,8,12,16,24};
			if( ((Number)cookie).intValue() == 0 )
				ss = FactoryPyramid.scaleSpacePyramid(scales, imageType);
			else
				ss = FactoryPyramid.scaleSpace(scales, imageType);

			if( workImage != null )
				ss.process(workImage);

			panel.setSs(ss);
			if( det == null )
				return;
		}

		det.detect(ss);
		panel.setPoints(det.getInterestPoints());
		panel.repaint();
		panel.requestFocusInWindow();
	}

	@Override
	public synchronized void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return hasImage;
	}

	public static void main( String args[] ) {

		DetectFeaturePyramidApp app = new DetectFeaturePyramidApp(ImageFloat32.class,ImageFloat32.class);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("amoeba","../data/evaluation/amoeba_shapes.jpg"));
		inputs.add(new PathLabel("sunflowers","../data/evaluation/sunflowers.png"));
		inputs.add(new PathLabel("beach","../data/evaluation/scale/beach02.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Feature Pyramid");
	}
}
