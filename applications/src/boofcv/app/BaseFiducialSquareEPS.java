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

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageUInt8;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Base class for generating square fiducials EPS documents for printing.
 *
 * @author Peter Abeles
 */
public class BaseFiducialSquareEPS {
	public double width = 10; // in centimeters of fiducial

	public int threshold = 255/2; // threshold for converting to a binary image
	public double CM_TO_POINTS = 72.0/2.54;
	// should it add the file name and size to the document?
	public boolean displayInfo = false;

	public boolean showPreview = false;

	public int numCols = 1;
	public int numRows = 1;

	PrintStream out;

	// Total length of the target.  image + border
	double targetLength;
	double whiteBorder;
	double blackBorder;
	double innerWidth ;
	// length of both sides of the page
	double pageLength;
	double scale;

	public void process( double width, String inputPath , String outputName ) throws FileNotFoundException {
		this.width = width;

		String inputName = new File(inputPath).getName();

		System.out.println("Target width "+width+" (cm)  image = "+inputName);
		System.out.println("    input path = "+inputPath);

		ImageUInt8 image = UtilImageIO.loadImage(inputPath, ImageUInt8.class);

		if( image == null ) {
			System.err.println("Can't find image.  Path = "+inputPath);
			System.exit(0);
		}

		// make sure the image is square and divisible by 8
		int s = image.width - (image.width%8);
		if( image.width != s || image.height != s ) {
			ImageUInt8 tmp = new ImageUInt8(s, s);
			new FDistort(image, tmp).scaleExt().apply();
			image = tmp;
		}

		// print out the selected number in binary for debugging purposes
		out = new PrintStream(outputName);

		targetLength = width*CM_TO_POINTS;
		whiteBorder = Math.max(2*CM_TO_POINTS,targetLength/4.0);
		blackBorder = targetLength/4.0;
		innerWidth = targetLength/2.0;
		pageLength = targetLength+whiteBorder*2;
		scale = image.width/innerWidth;

		ImageUInt8 binary = ThresholdImageOps.threshold(image, null, threshold, false);
		if( showPreview )
			ShowImages.showWindow(VisualizeBinaryData.renderBinary(binary, false, null), "Binary Image");

		printHeader(inputName);

		out.println();
		out.print("  /drawImage {\n" +
				"  "+binary.width+" " + binary.height + " 1 [" + scale + " 0 0 " + scale + " 0 0]\n" +
				"  {<"+binaryToHex(binary)+">} image\n" +
				"} def\n");
		out.println();
		// draws the black border around the fiducial
		out.print(" /drawBorder\n"+
				"{\n" +
				" newpath b0 b0 moveto 0 ow rlineto bb 0 rlineto 0 -1 ow mul rlineto closepath fill\n" +
				" newpath b1 b2 moveto iw 0 rlineto 0 bb rlineto -1 iw mul 0 rlineto closepath fill\n" +
				" newpath b1 b0 moveto iw 0 rlineto 0 bb rlineto -1 iw mul 0 rlineto closepath fill\n" +
				" newpath b2 b0 moveto 0 ow rlineto bb 0 rlineto 0 -1 ow mul rlineto closepath fill\n" +
				"} def\n");

		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				insertFiducial(row,col,inputName);
			}
		}

		out.print("  showpage\n" +
				"%%EOF\n");

		System.out.println("Saved to "+new File(outputName).getAbsolutePath());
	}

	private void printHeader( String inputName ) {
		out.println("%!PS-Adobe-3.0 EPSF-3.0\n" +
				"%%Creator: BoofCV\n" +
				"%%Title: "+inputName+" w="+width+"cm\n" +
				"%%DocumentData: Clean7Bit\n" +
				"%%Origin: 0 0\n" +
				"%%BoundingBox: 0 0 "+(numCols*pageLength)+" "+(numRows*pageLength)+"\n" +
				"%%LanguageLevel: 3\n" +
				"%%Pages: 1\n" +
				"%%Page: 1 1\n" +
				"  /iw " + innerWidth + " def\n" +
				"  /ow " + (innerWidth + 2 * blackBorder) + " def\n" +
				"  /wb " + whiteBorder + " def\n" +
				"  /bb " + blackBorder + " def\n" +
				"  /b0 wb def\n" +
				"  /b1 { wb bb add} def\n" +
				"  /b2 { b1 " + innerWidth + " add} def\n" +
				"  /b3 { b2 bb add} def\n");
	}

	private void insertFiducial(int row, int col, String inputName) {
		out.print(
				"  /originX " + (col * pageLength) + " def\n" +
				"  /originY " + (row * pageLength) + " def\n" +
				"  originX originY translate\n" );
		out.println();
		out.println("  drawBorder");

		// print out encoding information for convenience
		if( displayInfo ) {
			out.print("  /Times-Roman findfont\n" +
					"7 scalefont setfont b1 " + (pageLength - 10) + " moveto (" + inputName + "   " + width + " cm) show\n");
		}

		out.println("% Center then draw the image");
		out.println("  b1 b1 translate");
		out.println("  drawImage");
		out.println("% Undo translations");
		out.println("  -1 b1 mul -1 b1 mul translate");
		out.println("  -1 originX mul -1 originY mul translate");
	}

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

	public void setGrid( int numRows , int numCols ) {
		this.numRows = numRows;
		this.numCols = numCols;
	}
}
