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

package boofcv.alg.interpolate;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

/**
 * Compares different types of interpolation algorithms by enlarging an image.
 *
 * @author Peter Abeles
 */
public class EvaluateInterpolateEnlargeApp<T extends ImageSingleBand>
	extends SelectAlgorithmAndInputPanel implements ComponentListener
{
	Class<T> imageType;
	MultiSpectral<T> color;
	MultiSpectral<T> scaledImage;

	ImagePanel panel = new ImagePanel();
	boolean hasProcessed = false;

	public EvaluateInterpolateEnlargeApp( Class<T> imageType ) {
		super(1);
		this.imageType = imageType;

		panel.setResize(false);
		setMainGUI(panel);

		color = new MultiSpectral<T>(imageType,1,1,3);
		scaledImage = new MultiSpectral<T>(imageType,1,1,3);

		addAlgorithm(0, "Nearest Neighbor",FactoryInterpolation.nearestNeighborPixelS(imageType));
		addAlgorithm(0, "Bilinear",FactoryInterpolation.bilinearPixelS(imageType));
		addAlgorithm(0, "Bicubic Kernel",FactoryInterpolation.bicubicS(-0.5f, 0, 255, imageType));
		addAlgorithm(0, "Polynomial 5",FactoryInterpolation.polynomialS(5, 0, 255, imageType));

		setPreferredSize(new Dimension(300,300));
		addComponentListener(this);
	}

	public void process(BufferedImage image) {
		setInputImage(image);

		color.reshape(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFromMulti(image,color,true,imageType);

		hasProcessed = true;
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}
	
	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( color == null || 0 == panel.getWidth() || 0 == panel.getHeight() ) {
			return;
		}

		InterpolatePixelS<T> interp = (InterpolatePixelS<T>)cookie;

		scaledImage.reshape(panel.getWidth(),panel.getHeight());
		PixelTransformAffine_F32 model = DistortSupport.transformScale(scaledImage,color);
		for( int i = 0; i < color.getNumBands(); i++ )
			DistortImageOps.distortSingle(color.getBand(i),scaledImage.getBand(i),model,null,interp);

		// numerical round off error can cause the interpolation to go outside
		// of pixel value bounds
//		GeneralizedImageOps.boundImage(scaledImage,0,255);

		BufferedImage out = ConvertBufferedImage.convertTo(scaledImage,null,true);
		panel.setBufferedImage(out);
		panel.repaint();
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

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
		EvaluateInterpolateEnlargeApp app = new EvaluateInterpolateEnlargeApp(ImageFloat32.class);
//		EvaluateInterpolateEnlargeApp app = new EvaluateInterpolateEnlargeApp(ImageUInt8.class);

		app.setPreferredSize(new Dimension(500,500));

		app.setBaseDirectory("../data/applet/");
		app.loadInputData("../data/applet/interpolation.txt");

//		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
//		inputs.add(new PathLabel("eye 1","../data/evaluation/eye01.jpg"));
//		inputs.add(new PathLabel("eye 2","../data/evaluation/eye02.jpg"));
//
//		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Interpolation Enlarge");
	}
}
