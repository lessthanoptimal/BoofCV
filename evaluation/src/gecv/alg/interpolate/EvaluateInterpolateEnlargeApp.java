/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.interpolate;

import gecv.abst.filter.distort.GeneralizedDistortImageOps;
import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.GeneralizedImageOps;
import gecv.gui.image.ListDisplayPanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Compares different types of interpolation algorithms by enlarging an image.
 *
 * @author Peter Abeles
 */
public class EvaluateInterpolateEnlargeApp<T extends ImageBase> {

	T grayOriginal;
	int scaledWidth;
	int scaledHeight;

	ListDisplayPanel scalePanel = new ListDisplayPanel();

	public EvaluateInterpolateEnlargeApp( BufferedImage input ,
										  int scaledWidth , int scaledHeight ,
										  Class<T> imageType ) {
		grayOriginal = ConvertBufferedImage.convertFrom(input,null,imageType);
		this.scaledWidth = scaledWidth;
		this.scaledHeight = scaledHeight;
		scalePanel.addImage(input,"Original");
		scalePanel.setDataDisplaySize(scaledWidth,scaledHeight);
		ShowImages.showWindow(scalePanel,"Scaled Images");

	}

	public void addInterpolation( String name , InterpolatePixel<T> alg ) {
		T scaledImg = (T)grayOriginal._createNew(scaledWidth,scaledHeight);

		GeneralizedDistortImageOps.scale(grayOriginal,scaledImg,alg);

		// numerical round off error can cause the interpolation to go outside
		// of pixel value bounds
		GeneralizedImageOps.boundImage(scaledImg,0,255);

		BufferedImage out = ConvertBufferedImage.convertTo(scaledImg,null);
		scalePanel.addImage(out,name);
	}

	public static void main( String args[] ) {

		BufferedImage image = UtilImageIO.loadImage("evaluation/data/eye01.jpg");

		int w = image.getWidth()*8;
		int h = image.getHeight()*8;


		EvaluateInterpolateEnlargeApp<ImageFloat32> app = new EvaluateInterpolateEnlargeApp<ImageFloat32>(image,w,h, ImageFloat32.class);

		app.addInterpolation("NN",FactoryInterpolation.nearestNeighborPixel(ImageFloat32.class));
		app.addInterpolation("Bilinear",FactoryInterpolation.bilinearPixel(ImageFloat32.class));
		app.addInterpolation("Bicubic",FactoryInterpolation.bicubic(ImageFloat32.class,-0.5f));
	}
}
