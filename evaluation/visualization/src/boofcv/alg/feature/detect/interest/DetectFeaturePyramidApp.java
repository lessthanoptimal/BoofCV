/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.alg.transform.gss.ScaleSpacePyramid;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.feature.ScaleSpacePyramidPointPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Displays a window showing selected features across scale spaces using a pyramid as input.
 *
 * @author Peter Abeles
 */
public class DetectFeaturePyramidApp <T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmImagePanel implements ProcessInput
{
	static int NUM_FEATURES = 400;
	int r = 2;
	ScaleSpacePyramid<T> ss;
	Class<T> imageType;
	ScaleSpacePyramidPointPanel panel;
	boolean hasImage = false;

	public DetectFeaturePyramidApp( Class<T> imageType , Class<D> derivType ) {
		super(1);
		this.imageType = imageType;

		addAlgorithm(0, "Hessian",FactoryInterestPointAlgs.hessianPyramid(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm(0, "Hessian Laplace",FactoryInterestPointAlgs.hessianLaplacePyramid(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm(0, "Harris",FactoryInterestPointAlgs.harrisPyramid(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm(0, "Harris Laplace",FactoryInterestPointAlgs.harrisLaplacePyramid(r,1,NUM_FEATURES,imageType,derivType));

		ss = new ScaleSpacePyramid<T>(imageType,1,1.5,2,4,8,12,24);

		panel = new ScaleSpacePyramidPointPanel(ss,r);

		setMainGUI(panel);
	}

	public synchronized void process( BufferedImage input ) {
		setInputImage(input);
		T workImage = ConvertBufferedImage.convertFrom(input,null,imageType);
		ss.setImage(workImage);
		panel.setBackground(input);
		hasImage = true;
		doRefreshAll();
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( !hasImage )
			return;

		final InterestPointScaleSpacePyramid<T> det = (InterestPointScaleSpacePyramid<T>)cookie;
		det.detect(ss);
		panel.setPoints(det.getInterestPoints());
		panel.repaint();
		panel.requestFocusInWindow();
	}

	@Override
	public synchronized void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
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

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Feature Pyramid");
		System.out.println("Done");
	}
}
