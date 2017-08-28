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

import boofcv.alg.color.ColorHsv;
import boofcv.alg.color.ColorYuv;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

import java.awt.image.BufferedImage;

/**
 * Simple demonstration for converting between color spaces in BoofCV. Currently RGB, YUV, HSV, and YCbCr are
 * supported.
 *
 * @author Peter Abeles
 */
public class ExampleColorSpace {

	public static void main( String args[] ) {
		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("sunflowers.jpg"));

		// Convert input image into a BoofCV RGB image
		Planar<GrayF32> rgb = ConvertBufferedImage.convertFromPlanar(image, null,true, GrayF32.class);

		//---- convert RGB image into different color formats
		Planar<GrayF32> hsv = rgb.createSameShape();
		ColorHsv.rgbToHsv_F32(rgb, hsv);

		Planar<GrayF32> yuv = rgb.createSameShape();
		ColorYuv.yuvToRgb_F32(rgb, yuv);

		//---- Convert individual pixels into different formats
		float[] pixelHsv = new float[3];
		ColorHsv.rgbToHsv(10,50.6f,120,pixelHsv);
		System.out.printf("Found RGB->HSV = %5.2f %5.3f %5.1f\n",pixelHsv[0],pixelHsv[1],pixelHsv[2]);

		float[] pixelRgb = new float[3];
		ColorHsv.hsvToRgb(pixelHsv[0],pixelHsv[1],pixelHsv[2],pixelRgb);
		System.out.printf("Found HSV->RGB = %5.1f %5.1f %5.1f expected 10 50.6 120\n",
				pixelRgb[0],pixelRgb[1],pixelRgb[2]);

		float[] pixelYuv = new float[3];
		ColorYuv.rgbToYuv(10,50.6f,120,pixelYuv);
		System.out.printf("Found RGB->YUV = %5.1f %5.1f %5.1f\n",pixelYuv[0],pixelYuv[1],pixelYuv[2]);

		ColorYuv.yuvToRgb(pixelYuv[0],pixelYuv[1],pixelYuv[2],pixelRgb);
		System.out.printf("Found YUV->RGB = %5.1f %5.1f %5.1f expected 10 50.6 120\n",
				pixelRgb[0],pixelRgb[1],pixelRgb[2]);
	}
}
