/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.imageprocessing;

import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.core.image.ImageGenerator;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Example showing how to compute different image derivatives using built in functions.
 *
 * @author Peter Abeles
 */
public class ExampleImageDerivative {
	public static void main(String[] args) {
		BufferedImage input = UtilImageIO.loadImage("../data/evaluation/simple_objects.jpg");

		// We will use floating point images here, but ImageUInt8 with ImageSInt16 for derivatives also works
		ImageFloat32 grey = new ImageFloat32(input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFrom(input, grey);

		// First order derivative, also known as the gradient
		ImageFloat32 derivX = new ImageFloat32(grey.width,grey.height);
		ImageFloat32 derivY = new ImageFloat32(grey.width,grey.height);

		GImageDerivativeOps.sobel(grey, derivX, derivY, BorderType.EXTENDED);

		// Second order derivative, also known as the Hessian
		ImageFloat32 derivXX = new ImageFloat32(grey.width,grey.height);
		ImageFloat32 derivXY = new ImageFloat32(grey.width,grey.height);
		ImageFloat32 derivYY = new ImageFloat32(grey.width,grey.height);

		GImageDerivativeOps.hessianSobel(derivX, derivY, derivXX, derivXY, derivYY, BorderType.EXTENDED);

		// There's also a built in function for computing arbitrary derivatives
		ImageGenerator<ImageFloat32> genSInt16 = FactoryImageGenerator.create(ImageFloat32.class);
		AnyImageDerivative<ImageFloat32,ImageFloat32> derivative =
				new AnyImageDerivative<ImageFloat32,ImageFloat32>(GradientSobel.getKernelX(false),ImageFloat32.class,genSInt16);

		// the boolean sequence indicates if its an X or Y derivative
		derivative.setInput(grey);
		ImageFloat32 derivXYX = derivative.getDerivative(true, false, true);

		// Visualize the results
		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(ConvertBufferedImage.convertTo(grey,null),"Input Grey");
		gui.addImage(VisualizeImageData.colorizeSign(derivX, null, -1),"Sobel X");
		gui.addImage(VisualizeImageData.colorizeSign(derivY, null, -1),"Sobel Y");
		// Use colors to show X and Y derivatives in one image.  Looks pretty.
		gui.addImage(VisualizeImageData.colorizeGradient(derivX, derivY, -1),"Sobel Y and Y");
		gui.addImage(VisualizeImageData.colorizeSign(derivXX, null,-1),"Sobel XX");
		gui.addImage(VisualizeImageData.colorizeSign(derivXY, null,-1),"Sobel XY");
		gui.addImage(VisualizeImageData.colorizeSign(derivYY, null,-1),"Sobel YY");
		gui.addImage(VisualizeImageData.colorizeSign(derivXYX, null,-1),"Sobel XYX");

		ShowImages.showWindow(gui,"Image Derivatives",true);
	}
}
