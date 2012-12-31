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

package boofcv.alg.filter.derivative;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.factory.filter.derivative.FactoryDerivative.*;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class ShowImageDerivative<T extends ImageSingleBand, D extends ImageSingleBand>
	extends SelectAlgorithmAndInputPanel
{
	Class<T> imageType;
	Class<D> derivType;

	ListDisplayPanel panel = new ListDisplayPanel();

	T image;
	BufferedImage original;
	boolean processedImage = false;

	public ShowImageDerivative(Class<T> imageType, Class<D> derivType) {
		super(1);
		this.imageType = imageType;
		this.derivType = derivType;

		Helper h;

		h = new Helper(prewitt(imageType,derivType),hessianPrewitt(derivType));
		addAlgorithm(0, "Prewitt", h);
		h = new Helper(sobel(imageType,derivType),hessianSobel(derivType));
		addAlgorithm(0, "Sobel",h);
		h = new Helper(three(imageType,derivType),hessianThree(derivType));
		addAlgorithm(0, "Three",h);
		h = new Helper(gaussian(-1,3,imageType,derivType),hessianThree(derivType));
		addAlgorithm(0, "Gaussian", h);

		setMainGUI(panel);
	}

	public void process( final BufferedImage original ) {
		setInputImage(original);

		this.original = original;
		image = ConvertBufferedImage.convertFromSingle(original, null, imageType);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// adjust the preferred size for the list panel
				int width = panel.getListWidth();

//				setPreferredSize(new Dimension(original.getWidth()+width+10,original.getHeight()+30));
				doRefreshAll();
			}});
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, final Object cookie) {
		if( image == null )
			return;

		D derivX = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);

		panel.reset();

		Helper h = (Helper)cookie;

		D derivXX = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);
		D derivYY = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);
		D derivXY = GeneralizedImageOps.createSingleBand(derivType, image.width, image.height);

		h.gradient.process(image,derivX,derivY);
		h.hessian.process(derivX,derivY,derivXX,derivYY,derivXY);

		double max;

		max = GImageStatistics.maxAbs(derivX);
		panel.addImage(VisualizeImageData.colorizeSign(derivX,null,max),"X-derivative");
		max = GImageStatistics.maxAbs(derivY);
		panel.addImage(VisualizeImageData.colorizeSign(derivY,null,max),"Y-derivative");
		max = GImageStatistics.maxAbs(derivXX);
		panel.addImage(VisualizeImageData.colorizeSign(derivXX,null,max),"XX-derivative");
		max = GImageStatistics.maxAbs(derivYY);
		panel.addImage(VisualizeImageData.colorizeSign(derivYY,null,max),"YY-derivative");
		max = GImageStatistics.maxAbs(derivXY);
		panel.addImage(VisualizeImageData.colorizeSign(derivXY,null,max),"XY-derivative");

		processedImage = true;
		repaint();
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
		return processedImage;
	}

	private class Helper
	{
		public ImageGradient<T,D> gradient;
		public ImageHessian<D> hessian;

		private Helper(ImageGradient<T, D> gradient, ImageHessian<D> hessian) {
			this.gradient = gradient;
			this.hessian = hessian;
		}
	}

	public static void main(String args[]) {

		ShowImageDerivative<ImageFloat32,ImageFloat32> app
				= new ShowImageDerivative<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);
//		ShowImageDerivative<ImageUInt8, ImageSInt16> app
//				= new ShowImageDerivative<ImageUInt8,ImageSInt16>(ImageUInt8.class,ImageSInt16.class);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("sunflowers","../data/evaluation/sunflowers.png"));
		inputs.add(new PathLabel("beach","../data/evaluation/scale/beach02.jpg"));
		inputs.add(new PathLabel("xray","../data/applet/xray01.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Image Derivative");
	}
}
