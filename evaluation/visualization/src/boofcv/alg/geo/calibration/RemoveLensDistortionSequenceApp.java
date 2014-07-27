/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

import java.awt.image.BufferedImage;

/**
 * Removes lens distortion from an image sequence and saves the undistorted images to disk
 *
 * @author Peter Abeles
 */
public class RemoveLensDistortionSequenceApp {

	public static void main( String args[] ) {
		MediaManager media = DefaultMediaManager.INSTANCE;
		String calibDir = "../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/";
		String fileName = "/home/pja/a/lines.mjpeg";
		String outputDir = "/home/pja/a/images/";

		// load camera calibration and create class to undistort image
		IntrinsicParameters param = UtilIO.loadXML(calibDir + "intrinsic.xml");

		ImageDistort<ImageUInt8,ImageUInt8> undistorter = DistortImageOps.createImageDistort(
				LensDistortionOps.allInside(param,null),TypeInterpolate.BILINEAR,ImageUInt8.class,ImageUInt8.class);

		MultiSpectral<ImageUInt8> input = null;
		MultiSpectral<ImageUInt8> output = null;

		SimpleImageSequence<ImageUInt8> sequence = media.openVideo(fileName, ImageType.single(ImageUInt8.class));

		BufferedImage undistorted = null;

		int num = 0;
		while( sequence.hasNext() ) {
			sequence.next();
			BufferedImage image = sequence.getGuiImage();

			input = ConvertBufferedImage.convertFromMulti(image,input,true,ImageUInt8.class);
			if( output == null )
				output = new MultiSpectral<ImageUInt8>(ImageUInt8.class,input.width,input.height,input.getNumBands());

			DistortImageOps.distortMS(input,output,undistorter);

			undistorted = ConvertBufferedImage.convertTo_U8(output, undistorted,true);

			System.out.println("Saving image " + num);
			UtilImageIO.saveImage(undistorted, outputDir + String.format("frame%06d.bmp", num++));
		}
	}
}
