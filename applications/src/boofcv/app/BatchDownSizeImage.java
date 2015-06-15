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

package boofcv.app;

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;

/**
 * Loads a set of images, resizes them to a smaller size using an intelligent algorithm then saves them.
 *
 * @author Peter Abeles
 */
public class BatchDownSizeImage {

	public static void printHelpAndExit(String[] args) {
		System.out.println("Expected 4 arguments, had "+args.length+" instead");
		System.out.println();
		System.out.println("<file path regex> <output directory> <width> <height>");
		System.out.println("If height == 0 then it will select the height which maintains the same aspect ratio");
		System.out.println("path/to/input/image\\d*.jpg path/to/output 640 0");
		System.exit(0);
	}

	public static void main(String[] args) {
		if( args.length != 4 )
			printHelpAndExit(args);

		String fileRegex = args[0];
		String output = args[1];
		int width = Integer.parseInt(args[2]);
		int height = Integer.parseInt(args[3]);

		File out = new File(output);
		if( !out.exists() )
			if( !out.mkdirs() )
				throw new IllegalArgumentException("Can't create output directory: "+output);

		File d = new File(fileRegex).getParentFile();

		if( !d.exists()) throw new IllegalArgumentException("Can't find directory "+d.getPath());

		String regex = new File(fileRegex).getName();

		File[] images = BoofMiscOps.findMatches(d, regex);

		if( images.length == 0 ) {
			System.out.println(fileRegex);
			throw new IllegalArgumentException("No images found.  Is the path/regex correct?");
		}

		WritableRaster info = UtilImageIO.loadImage(images[0].getPath()).getRaster();

		ImageBase input;
		if( info.getNumBands() == 1 ) {
			input = new ImageUInt8(info.getWidth(),info.getHeight());
		} else {
			input = new MultiSpectral<ImageUInt8>(
					ImageUInt8.class,info.getWidth(),info.getHeight(),info.getNumBands());
		}

		if( height == 0 ) {
			height = input.getHeight()*width/input.getWidth();
		}

		if( input.getWidth() < width || input.getHeight() < height ) {
			System.err.println("Input image = "+input.getWidth()+" "+input.getHeight());
			System.err.println("Desired     = "+width+" "+height);
			throw new IllegalArgumentException("The new width and height must be smaller than the original image.");
		}

		ImageBase small = input._createNew(width,height);

		for ( File f : images) {
			System.out.println("Processing: "+f.getName());

			BufferedImage orig = UtilImageIO.loadImage(f.getPath());
			ConvertBufferedImage.convertFrom(orig,input,true);

			AverageDownSampleOps.down(input,small);

			String nout = f.getName();
			nout = nout.substring(0,nout.length()-3) + "png";

			File fout = new File(output,nout);
			UtilImageIO.saveImage(small,fout.getPath());
		}
		System.out.println("Done");
	}
}
