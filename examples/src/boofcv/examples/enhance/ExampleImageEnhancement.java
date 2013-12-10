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

package boofcv.examples.enhance;

import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageUInt8;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Demonstration of various ways an image can be "enhanced".  Image enhancement typically refers to making it easier
 * for people to view the image and pick out its details.
 *
 * @author Peter Abeles
 */
public class ExampleImageEnhancement {

	/**
	 * Histogram adjustment algorithms aim to spread out pixel intensity values uniformly across the allowed range.
	 * This if an image is dark, it will have greater contrast and be brighter.
	 */
	public static void histogram() {
//		BufferedImage buffered = UtilImageIO.loadImage("../data/applet/enhance/dull.jpg");
		BufferedImage buffered = UtilImageIO.loadImage("../data/applet/enhance/dark.jpg");
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(buffered,(ImageUInt8)null);
		ImageUInt8 adjusted = new ImageUInt8(gray.width, gray.height);

		int histogram[] = new int[256];
		int transform[] = new int[256];

		ListDisplayPanel panel = new ListDisplayPanel();

		ImageStatistics.histogram(gray,histogram);
		EnhanceImageOps.equalize(histogram, transform);
		EnhanceImageOps.applyTransform(gray, transform, adjusted);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted,null),"Global");

		EnhanceImageOps.equalizeLocal(gray, 50, adjusted, histogram, transform);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted,null),"Local");

		panel.addImage(ConvertBufferedImage.convertTo(gray,null),"Original");

		panel.setPreferredSize(new Dimension(gray.width,gray.height));
		ShowImages.showWindow(panel,"Histogram");
	}

	/**
	 * When an image is sharpened the intensity of edges are made more extreme while flat regions remain unchanged.
	 */
	public static void sharpen() {
//		BufferedImage buffered = UtilImageIO.loadImage("../data/applet/enhance/dull.jpg");
		BufferedImage buffered = UtilImageIO.loadImage("../data/applet/enhance/dark.jpg");
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(buffered,(ImageUInt8)null);
		ImageUInt8 adjusted = new ImageUInt8(gray.width, gray.height);


		ListDisplayPanel panel = new ListDisplayPanel();

		EnhanceImageOps.sharpen4(gray, adjusted);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted,null),"Sharpen-4");

		EnhanceImageOps.sharpen8(gray, adjusted);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted,null),"Sharpen-8");

		panel.addImage(ConvertBufferedImage.convertTo(gray,null),"Original");

		panel.setPreferredSize(new Dimension(gray.width,gray.height));
		ShowImages.showWindow(panel,"Sharpen");
	}

	public static void main( String args[] )
	{
		histogram();
		sharpen();
	}

}
