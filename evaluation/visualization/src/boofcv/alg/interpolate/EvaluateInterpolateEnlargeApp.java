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

package boofcv.alg.interpolate;

import boofcv.alg.distort.DistortImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.ProcessImage;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageUInt8;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

/**
 * Compares different types of interpolation algorithms by enlarging an image.
 *
 * @author Peter Abeles
 */
public class EvaluateInterpolateEnlargeApp<T extends ImageBase>
	extends SelectAlgorithmImagePanel implements ProcessImage , ComponentListener
{
	Class<T> imageType;
	T gray;
	T scaledImage;

	ImagePanel panel = new ImagePanel();
	boolean hasProcessed = false;

	public EvaluateInterpolateEnlargeApp( Class<T> imageType ) {
		super(1);
		this.imageType = imageType;

		panel.setResize(false);
		setMainGUI(panel);

		scaledImage = GeneralizedImageOps.createImage(imageType,1,1);

		addAlgorithm(0, "Nearest Neighbor",FactoryInterpolation.nearestNeighborPixel(imageType));
		addAlgorithm(0, "Bilinear",FactoryInterpolation.bilinearPixel(imageType));
		addAlgorithm(0, "Bicubic Kernel",FactoryInterpolation.bicubic(imageType,-0.5f));

		setPreferredSize(new Dimension(300,300));
		addComponentListener(this);
	}

	public void process(BufferedImage image) {
		setInputImage(image);

		gray = ConvertBufferedImage.convertFrom(image,null,imageType);

		hasProcessed = true;
		doRefreshAll();
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}
	
	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( gray == null || 0 == panel.getWidth() || 0 == panel.getHeight() ) {
			return;
		}

		InterpolatePixel<T> interp = (InterpolatePixel<T>)cookie;


		scaledImage.reshape(panel.getWidth(),panel.getHeight());
		DistortImageOps.scale(gray,scaledImage,interp);

		// numerical round off error can cause the interpolation to go outside
		// of pixel value bounds
//		GeneralizedImageOps.boundImage(scaledImage,0,255);

		BufferedImage out = ConvertBufferedImage.convertTo(scaledImage,null);
		panel.setBufferedImage(out);
		panel.repaint();
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
		return hasProcessed;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		setActiveAlgorithm(0,null, getAlgorithmCookie(0));
	}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentHidden(ComponentEvent e) {}

	public static void main( String args[] ) {
//		EvaluateInterpolateEnlargeApp app = new EvaluateInterpolateEnlargeApp(ImageFloat32.class);
		EvaluateInterpolateEnlargeApp app = new EvaluateInterpolateEnlargeApp(ImageUInt8.class);

		app.setPreferredSize(new Dimension(500,500));
		ImageListManager manager = new ImageListManager();
		manager.add("eye 1","data/eye01.jpg");
		manager.add("eye 2","data/eye02.jpg");

		app.setImageManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Interpolation Enlarge");
	}
}
