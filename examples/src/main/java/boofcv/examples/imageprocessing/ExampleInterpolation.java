/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;

/**
 * Interpolation is used to convert an image, which is discrete by its nature, into a (piecewise) smooth function.
 * Interpolation is in many CV applications, such as feature detection, and when distorting images.  In this
 * example a low resolution is scaled up using several different techniques to make the differences easily visible.
 * For computer vision applications bilinear interpolation is almost always used.
 *
 * @author Peter Abeles
 */
public class ExampleInterpolation {

	public static void main(String[] args) {
		String imagePath;
		imagePath = "eye01.jpg";
//		imagePath = "small_sunflower.jpg";

		BufferedImage buffered = UtilImageIO.loadImage(UtilIO.pathExample(imagePath));
		ListDisplayPanel gui = new ListDisplayPanel();

		gui.addImage(buffered,"Original");

		// For sake of simplicity assume it's a gray scale image.  Interpolation functions exist for planar and
		// interleaved color images too
		GrayF32 input  = ConvertBufferedImage.convertFrom(buffered, (GrayF32)null);
		GrayF32 scaled = input.createNew(500,500*input.height/input.width);

		for( InterpolationType type : InterpolationType.values() ) {
			// Create the single band (gray scale) interpolation function for the input image
			InterpolatePixelS<GrayF32> interp = FactoryInterpolation.
					createPixelS(0,255,type,BorderType.EXTENDED,input.getDataType());

			// Tell it which image is being interpolated
			interp.setImage(input);

			// Manually apply scaling to the input image.  See FDistort() for a built in function which does
			// the same thing and is slightly more efficient
			for (int y = 0; y < scaled.height; y++) {
				// iterate using the 1D index for added performance.  Altertively there is the set(x,y) operator
				int indexScaled = scaled.startIndex + y*scaled.stride;
				float origY = y*input.height/(float)scaled.height;

				for (int x = 0; x < scaled.width; x++) {
					float origX = x*input.width/(float)scaled.width;

					scaled.data[indexScaled++] = interp.get(origX,origY);
				}
			}

			// Add the results to the output
			BufferedImage out = ConvertBufferedImage.convertTo(scaled,null,true);
			gui.addImage(out,type.toString());
		}

		ShowImages.showWindow(gui,"Example Interpolation", true);
	}
}
