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

package boofcv.alg.filter.derivative;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.ProcessImage;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class ShowImageDerivative<T extends ImageBase, D extends ImageBase>
	extends SelectAlgorithmImagePanel implements ProcessImage
{
	Class<T> imageType;
	Class<D> derivType;

	ListDisplayPanel panel = new ListDisplayPanel();

	D derivX,derivY;
	T image;
	BufferedImage original;
	boolean processedImage = false;

	public ShowImageDerivative(Class<T> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;

		addAlgorithm("Prewitt",FactoryDerivative.prewitt(imageType,derivType));
		addAlgorithm("Sobel",FactoryDerivative.sobel(imageType,derivType));
		addAlgorithm("Three",FactoryDerivative.three(imageType,derivType));
		addAlgorithm("Gaussian", FactoryDerivative.gaussian(-1,3,imageType,derivType));

		addMainGUI(panel);
	}

	@Override
	public void process( final BufferedImage original ) {
		setInputImage(original);

		this.original = original;
		image = ConvertBufferedImage.convertFrom(original,null,imageType);

		derivX = GeneralizedImageOps.createImage(derivType,image.width,image.height);
		derivY = GeneralizedImageOps.createImage(derivType,image.width,image.height);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// adjust the preferred size for the list panel
				int width = panel.getListWidth();

				setPreferredSize(new Dimension(original.getWidth()+width+10,original.getHeight()+30));
				processedImage = true;
				refreshAlgorithm();
			}});
	}

	@Override
	public void setActiveAlgorithm(String name, final Object cookie) {
		if( image == null )
			return;

		ImageGradient<T,D> gradient = (ImageGradient<T,D>)cookie;

		panel.reset();

		gradient.process(image,derivX,derivY);

		double maxX = GPixelMath.maxAbs(derivX);
		double maxY = GPixelMath.maxAbs(derivY);
		panel.addImage(VisualizeImageData.colorizeSign(derivX,null,maxX),"X-derivative");
		panel.addImage(VisualizeImageData.colorizeSign(derivY,null,maxY),"Y-derivative");

		repaint();
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getImageManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main(String args[]) {

		ShowImageDerivative<ImageFloat32,ImageFloat32> app
				= new ShowImageDerivative<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setImageManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Image Derivative");
	}
}
