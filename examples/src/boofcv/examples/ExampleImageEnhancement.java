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

package boofcv.examples;

import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class ExampleImageEnhancement {

	public static void histogramGlobal() {
		BufferedImage buffered = UtilImageIO.loadImage("/home/pja/Image.jpg");
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(buffered,(ImageUInt8)null);
		ImageUInt8 adjusted = new ImageUInt8(gray.width, gray.height);

		int histogram[] = new int[256];
		int transform[] = new int[256];

		ImageStatistics.histogram(gray,histogram);
		EnhanceImageOps.equalize(histogram, transform);
		EnhanceImageOps.applyTransform(gray, transform, adjusted);

//		ShowImages.showWindow(gray,"Original");
		ShowImages.showWindow(adjusted,"Global Histogram");
	}

	public static void histogramLocal() {
		BufferedImage buffered = UtilImageIO.loadImage("/home/pja/Image.jpg");
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(buffered,(ImageUInt8)null);
		ImageUInt8 adjusted = new ImageUInt8(gray.width, gray.height);

		int histogram[] = new int[256];
		int transform[] = new int[256];

		EnhanceImageOps.equalizeLocal(gray, 50, adjusted, histogram, transform);

		ShowImages.showWindow(gray,"Original");
		ShowImages.showWindow(adjusted,"Local Histogram");
	}

	public static void main( String args[] ) {
		histogramGlobal();
		histogramLocal();
	}

}
