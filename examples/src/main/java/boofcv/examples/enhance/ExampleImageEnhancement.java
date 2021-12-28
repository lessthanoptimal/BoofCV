/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.enhance.GEnhanceImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Demonstration of various ways an image can be "enhanced". Image enhancement typically refers to making it easier
 * for people to view the image and pick out its details.
 *
 * @author Peter Abeles
 */
public class ExampleImageEnhancement {

	static String imagePath = "enhance/dark.jpg";
//	static String imagePath = "enhance/dull.jpg";

	static ListDisplayPanel mainPanel = new ListDisplayPanel();

	/**
	 * Histogram adjustment algorithms aim to spread out pixel intensity values uniformly across the allowed range.
	 * This if an image is dark, it will have greater contrast and be brighter.
	 */
	public static void histogram() {
		BufferedImage buffered = UtilImageIO.loadImageNotNull(UtilIO.pathExample(imagePath));
		GrayU8 gray = ConvertBufferedImage.convertFrom(buffered, (GrayU8)null);
		GrayU8 adjusted = gray.createSameShape();

		int[] histogram = new int[256];
		int[] transform = new int[256];

		ListDisplayPanel panel = new ListDisplayPanel();

		ImageStatistics.histogram(gray, 0, histogram);
		EnhanceImageOps.equalize(histogram, transform);
		EnhanceImageOps.applyTransform(gray, transform, adjusted);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted, null), "Global");

		EnhanceImageOps.equalizeLocal(gray, 50, adjusted, 256, null);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted, null), "Local");

		panel.addImage(ConvertBufferedImage.convertTo(gray, null), "Original");

		panel.setPreferredSize(new Dimension(gray.width, gray.height));
		mainPanel.addItem(panel, "Histogram");
	}

	/**
	 * Same as above but on a color image.
	 */
	public static void histogramColor() {
		BufferedImage buffered = UtilImageIO.loadImageNotNull(UtilIO.pathExample(imagePath));
		Planar<GrayU8> color = ConvertBufferedImage.convertFrom(buffered, true, ImageType.PL_U8);
		Planar<GrayU8> adjusted = color.createSameShape();

		int[] histogram = new int[256];
		int[] transform = new int[256];

		ListDisplayPanel panel = new ListDisplayPanel();

		// Apply the correction to each color band independently. Alternatively, you could compute the adjustment
		// on a gray scale image then apply the same transform to each band
		for (int bandIdx = 0; bandIdx < color.getNumBands(); bandIdx++) {
			ImageStatistics.histogram(color.getBand(bandIdx), 0, histogram);
			EnhanceImageOps.equalize(histogram, transform);
			EnhanceImageOps.applyTransform(color.getBand(bandIdx), transform, adjusted.getBand(bandIdx));
		}
		panel.addImage(ConvertBufferedImage.convertTo(adjusted, null, true), "Global");

		GEnhanceImageOps.equalizeLocal(color, 50, adjusted, 256, null);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted, null, true), "Local");
		panel.addImage(buffered, "Original");

		panel.setPreferredSize(new Dimension(color.width, color.height));
		mainPanel.addItem(panel, "Histogram Color");
	}

	/**
	 * When an image is sharpened the intensity of edges are made more extreme while flat regions remain unchanged.
	 */
	public static void sharpen() {
		BufferedImage buffered = UtilImageIO.loadImageNotNull(UtilIO.pathExample(imagePath));
		GrayU8 gray = ConvertBufferedImage.convertFrom(buffered, (GrayU8)null);
		GrayU8 adjusted = gray.createSameShape();


		ListDisplayPanel panel = new ListDisplayPanel();

		EnhanceImageOps.sharpen4(gray, adjusted);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted, null), "Sharpen-4");

		EnhanceImageOps.sharpen8(gray, adjusted);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted, null), "Sharpen-8");

		panel.addImage(ConvertBufferedImage.convertTo(gray, null), "Original");

		panel.setPreferredSize(new Dimension(gray.width, gray.height));
		mainPanel.addItem(panel, "Sharpen");
	}

	/**
	 * Same as above but on a color image
	 */
	public static void sharpenColor() {
		BufferedImage buffered = UtilImageIO.loadImageNotNull(UtilIO.pathExample(imagePath));
		Planar<GrayU8> color = ConvertBufferedImage.convertFrom(buffered, true, ImageType.PL_U8);
		Planar<GrayU8> adjusted = color.createSameShape();

		ListDisplayPanel panel = new ListDisplayPanel();

		GEnhanceImageOps.sharpen4(color, adjusted);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted, null, true), "Sharpen-4");

		GEnhanceImageOps.sharpen8(color, adjusted);
		panel.addImage(ConvertBufferedImage.convertTo(adjusted, null, true), "Sharpen-8");

		panel.addImage(buffered, "Original");

		panel.setPreferredSize(new Dimension(color.width, color.height));
		mainPanel.addItem(panel, "Sharpen Color");
	}

	public static void main( String[] args ) {
		histogram();
		histogramColor();
		sharpen();
		sharpenColor();
		ShowImages.showWindow(mainPanel, "Enhancement", true);
	}
}
