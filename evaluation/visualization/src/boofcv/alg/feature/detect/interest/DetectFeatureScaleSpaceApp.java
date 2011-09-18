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

import boofcv.abst.feature.detect.interest.InterestPointScaleSpace;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.transform.gss.FactoryGaussianScaleSpace;
import boofcv.gui.ProcessImage;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.feature.ScaleSpacePointPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Displays a window showing selected features across scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFeatureScaleSpaceApp<T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmImagePanel implements ProcessImage
{

	static int NUM_FEATURES = 400;
	int r = 2;
	GaussianScaleSpace<T,D> ss;
	Class<T> imageType;
	ScaleSpacePointPanel panel;
	boolean hasImage = false;

	public DetectFeatureScaleSpaceApp( Class<T> imageType , Class<D> derivType ) {
		super(1);
		this.imageType = imageType;

		addAlgorithm(0, "Hessian",FactoryInterestPointAlgs.hessianScaleSpace(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm(0, "Hessian Laplace",FactoryInterestPointAlgs.hessianLaplace(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm(0, "Harris",FactoryInterestPointAlgs.harrisScaleSpace(r,1,NUM_FEATURES,imageType,derivType));
		addAlgorithm(0, "Harris Laplace",FactoryInterestPointAlgs.harrisLaplace(r,1,NUM_FEATURES,imageType,derivType));

		ss = FactoryGaussianScaleSpace.nocache(imageType);
		ss.setScales(1,1.5,2,4,8,12,24);

		panel = new ScaleSpacePointPanel(ss,r);

		setMainGUI(panel);
	}

	public void process( BufferedImage input ) {
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
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( !hasImage )
			return;

		final InterestPointScaleSpace<T,D> det = (InterestPointScaleSpace<T,D>)cookie;
		det.detect(ss);
		panel.setPoints(det.getInterestPoints());
		panel.repaint();
		panel.requestFocusInWindow();
	}

	@Override
	public synchronized void changeImage(String name, int index) {
		ImageListManager manager = getImageManager();

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
		DetectFeatureScaleSpaceApp app = new DetectFeatureScaleSpaceApp(ImageFloat32.class,ImageFloat32.class);

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setImageManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Feature Scale Space");

		System.out.println("Done");
	}
}
