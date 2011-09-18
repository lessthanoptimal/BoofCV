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

package boofcv.alg.transform.gss;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.transform.gss.FactoryGaussianScaleSpace;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.ProcessImage;
import boofcv.gui.SelectImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Displays the scale space for a particular image.
 *
 * @author Peter Abeles
 */
public class VisualizeScaleSpaceApp <T extends ImageBase, D extends ImageBase>
	extends SelectImagePanel implements ProcessImage
{
	GaussianScaleSpace<T,D> ss;

	Class<T> imageType;
	ListDisplayPanel gui = new ListDisplayPanel();
	boolean processedImage = false;

	public VisualizeScaleSpaceApp( Class<T> imageType ) {
		this.imageType = imageType;

		ss = FactoryGaussianScaleSpace.nocache(imageType);
		ss.setScales(1,1.2,2.4,3.6,4.8,6.0,10,15,20);

		setMainGUI(gui);
	}

	public void process(BufferedImage image) {
		setInputImage(image);
		T gray = ConvertBufferedImage.convertFrom(image,null,imageType);

		ss.setImage(gray);
		gui.reset();

		for( int i = 0; i < ss.getTotalScales(); i++ ) {
			ss.setActiveScale(i);
			double scale = ss.getCurrentScale();
			T scaledImage = ss.getScaledImage();
			BufferedImage b = ConvertBufferedImage.convertTo(scaledImage,null);
			gui.addImage(b,String.format("Scale %6.2f",scale));
		}
		processedImage = true;
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getImageManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	public static void main( String args[] ) {
		VisualizeScaleSpaceApp app = new VisualizeScaleSpaceApp(ImageFloat32.class);

		ImageListManager manager = new ImageListManager();
		manager.add("boat","data/standard/boat.png");
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");

		app.setImageManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Scale Space");
	}
}
