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

package boofcv.app;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageUInt8;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Outputs an EPS document describing a binary square fiducial that encodes the specified number
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareImageEPS {

	public static String outputName = "fiducial_image.eps";
	public static String inputPath = UtilIO.getPathToBase()+"data/applet/fiducial/image/dog.png";
	public static double width = 10;

	public static double CM_TO_POINTS = 72.0/2.54;


	public static String binaryToHex( ImageUInt8 binary ) {

		if( binary.width%8 != 0 )
			throw new RuntimeException("Width must be divisible by 8");

		StringBuilder s = new StringBuilder(binary.width*binary.height/4);

		for (int y = binary.height-1; y >= 0 ; y--) {
			int i = y*binary.width;
			for (int x = 0; x < binary.width; x += 8, i+= 8) {
				int value = 0;
				for (int j = 0; j < 8; j++) {
					value |= binary.data[i+j] << (7-j);
				}

				String hex = Integer.toHexString(value);
				if( hex.length() == 1 )
					hex = "0"+hex;
				s.append(hex);
			}
		}

		return s.toString();
	}

	public static void main(String[] args) throws FileNotFoundException {

		if( args.length == 2 ) {
			width = Double.parseDouble(args[0]);
			inputPath = args[1];
		}

		String inputName = new File(inputPath).getName();

		System.out.println("Target width "+width+" (cm)  image = "+inputName);

		ImageUInt8 image = UtilImageIO.loadImage(inputPath,ImageUInt8.class);

		if( image.width != 304 || image.height != 304 ) {
			ImageUInt8 out = new ImageUInt8(304,304);
			DistortImageOps.scale(image,out, TypeInterpolate.BILINEAR);
			image = out;
		}

		ImageUInt8 binary = ThresholdImageOps.threshold(image,null,256/2,false);

//		BinaryImageOps.invert(binary,binary);
//		ShowImages.showWindow(VisualizeBinaryData.renderBinary(binary,null),"Binary Image");

		// print out the selected number in binary for debugging purposes
		PrintStream out = new PrintStream(outputName);

		double sideLength = width*CM_TO_POINTS;
		double squareLength = sideLength/6;
		double pageLength = sideLength+squareLength*2;

		double scale = binary.width/(squareLength*4);

		out.println("%!PS-Adobe-3.0 EPSF-3.0\n" +
				"%%Creator: BoofCV\n" +
				"%%Title: "+inputName+" w="+width+"cm\n" +
				"%%DocumentData: Clean7Bit\n" +
				"%%Origin: 0 0\n" +
//				"%%BoundingBox: xmin ymin xmax ymax\n" +
				"%%BoundingBox: 0 0 "+pageLength+" "+pageLength+"\n" +
				"%%LanguageLevel: 3\n" +
				"%%Pages: 1\n" +
				"%%Page: 1 1\n");
		out.println(
				"  /w { 1 " + squareLength + " mul} def\n" +
						"  /w2 { w " + squareLength + " add} def\n" +
						"  /w3 { w2 " + squareLength + " add} def\n" +
						"  /w4 { w3 " + squareLength + " add} def\n" +
						"  /w5 { w4 " + squareLength + " add} def\n" +
						"  /w6 { w5 " + squareLength + " add} def\n" +
						"  /w7 { w6 " + squareLength + " add} def\n" +
						"  /w8 { w7 " + squareLength + " add} def\n" +
						"  /pagewidth " + sideLength + " def\n" +
						"  /box {newpath moveto w 0 rlineto 0 w rlineto w neg 0 rlineto closepath fill} def\n" +
						"% bottom top left right borders..\n" +
						"  newpath w w moveto w7 w lineto w7 w2 lineto w w2 lineto closepath fill\n" +
						"  newpath w w6 moveto w7 w6 lineto w7 w7 lineto w w7 lineto closepath fill\n" +
						"  newpath w w2 moveto w2 w2 lineto w2 w6 lineto w w6 lineto closepath fill\n" +
						"  newpath w6 w2 moveto w7 w2 lineto w7 w6 lineto w6 w6 lineto closepath fill\n");

		// print out encoding information for convenience
		out.print("  /Times-Roman findfont\n" +
				"7 scalefont setfont w " + (pageLength - 10) + " moveto (" + inputName + "   " + width + " cm) show\n");

		out.println("% Drawing the image");
		out.println("w2 w2 translate");
		out.println(binary.width+" "+binary.height+" 1 ["+scale+" 0 0 "+scale+" 0 0]");
		out.println("{<" + binaryToHex(binary) + ">} image");
		out.print("  showpage\n" +
				"%%EOF\n");

	}
}
