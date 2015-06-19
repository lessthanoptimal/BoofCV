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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for generating square fiducials EPS documents for printing.
 *
 * @author Peter Abeles
 */
// TODO When in grid mode.  just put labels above fiducials.  size printed once in the bottom
public class BaseFiducialSquareEPS {

	public int threshold = 255/2; // threshold for converting to a binary image
	public double UNIT_TO_POINTS;
	public static final double CM_TO_POINTS = 72.0/2.54;
	// should it add the file name and size to the document?
	public boolean printInfo = true;

	public boolean showPreview = false;

	// base unit of input lengths
	Unit unit = Unit.CENTIMETER;

	// grid pattern
	public int numCols = 1;
	public int numRows = 1;

	boolean autofillGrid = false;

	PrintStream out;

	// Total length of the target.  image + black border
	double targetLength;
	// length of the white border surrounding the fiducial
	double whiteBorder;
	// length of the black border
	double blackBorder;
	// length of the inner pattern
	double innerWidth ;
	// Length of the fiducial plus the white border
	double totalWidth;
	// width and height of the page
	double pageWidth,pageHeight;
	// border added around the page.
	double pageBorder;
	// the size of the requested border around the document.  the actual page border will be this minus the white border
	double pageBorderRequest = 1*CM_TO_POINTS;
	// offset to center everything
	double centerOffsetX,centerOffsetY;

	// image scale factor
	double scale;

	String outputName;

	List<String> imagePaths = new ArrayList<String>();

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public void addImage( String inputPath ) {
		this.imagePaths.add(inputPath);
	}

	public boolean isPrintInfo() {
		return printInfo;
	}

	public void setPrintInfo(boolean displayInfo) {
		this.printInfo = displayInfo;
	}

	public boolean isShowPreview() {
		return showPreview;
	}

	public void setShowPreview(boolean showPreview) {
		this.showPreview = showPreview;
	}

	public void setPageBorder( double size , Unit units) {
		pageBorderRequest = units.convert(size,Unit.CENTIMETER)*CM_TO_POINTS;
	}

	public void setOutputName( String outputName )  {
		this.outputName = outputName;
	}

	public void generateGrid( double fiducialWidth , double whiteBorder , int numCols , int numRows )
			throws IOException
	{
		this.numRows = numRows;
		this.numCols = numCols;
		generate(fiducialWidth, whiteBorder, -1, -1);
	}

	public void generateGrid( double fiducialWidth , double whiteBorder , PaperSize paper )
			throws IOException
	{
		double pageWidthUnit = paper.getUnit().convert(paper.getWidth(),unit);
		double pageHeightUnit = paper.getUnit().convert(paper.getHeight(),unit);

		this.autofillGrid = true;
		generate(fiducialWidth, whiteBorder, pageWidthUnit,pageHeightUnit);
	}

	public void generateSingle( double fiducialWidth ) throws IOException {
		double whiteBorder = Math.max(2, fiducialWidth / 4.0);
		generateSingle(fiducialWidth, whiteBorder);
	}

	public void generateSingle( double fiducialWidth , PaperSize paper ) throws IOException {
		double whiteBorder = Math.max(2, fiducialWidth / 4.0);

		double pageWidthUnit = paper.getUnit().convert(paper.getWidth(),unit);
		double pageHeightUnit = paper.getUnit().convert(paper.getHeight(),unit);

		numCols = numRows = 1;
		generate(fiducialWidth, whiteBorder, -1,pageHeightUnit);
	}

	public void generateSingle( double fiducialWidthCM , double whiteBorderCM ) throws IOException {
		numCols = numRows = 1;
		generate(fiducialWidthCM, whiteBorderCM, -1,-1);
	}

	private void generate(double fiducialWidthUnit, double whiteBorderUnit,
						  double pageWidthUnit, double pageHeightUnit) throws IOException {

		UNIT_TO_POINTS = 72.0/(2.54*Unit.conversion(Unit.CENTIMETER,unit));

		if( autofillGrid ) {
			if( pageWidthUnit <= 0 || pageHeightUnit <=  0)
				throw new IllegalArgumentException("If autofillGrid is turned on then the page size must be specified");

			double pageBorderUnit = Math.max(0,pageBorderRequest/UNIT_TO_POINTS - whiteBorderUnit);

			double totalWidthUnit = fiducialWidthUnit+2*whiteBorderUnit;
			this.numRows = (int)Math.floor((pageHeightUnit-2*pageBorderUnit)/totalWidthUnit);
			this.numCols = (int)Math.floor((pageWidthUnit-2*pageBorderUnit)/totalWidthUnit);
		}

		String outputName;
		if( this.outputName == null ) {
			String inputPath = imagePaths.get(0);
			File dir = new File(inputPath).getParentFile();
			outputName = new File(inputPath).getName();
			outputName = outputName.substring(0,outputName.length()-3) + "eps";
			outputName = new File(dir,outputName).getCanonicalPath();
		} else {
			outputName = this.outputName;
		}


		String imageName;
		if( imagePaths.size() == 1 ) {
			imageName = new File(imagePaths.get(0)).getName();
		} else {
			imageName = "Multiple Patterns";
		}

		System.out.println("Fiducial width "+ fiducialWidthUnit +" ("+unit.abbreviation+")");

		// print out the selected number in binary for debugging purposes
		out = new PrintStream(outputName);

		targetLength = fiducialWidthUnit* UNIT_TO_POINTS;
		whiteBorder = whiteBorderUnit* UNIT_TO_POINTS;
		blackBorder = targetLength/4.0;
		innerWidth = targetLength/2.0;
		totalWidth = targetLength+whiteBorder*2;

		if( pageBorderRequest > 0 ) {
			pageBorder = Math.max(0,pageBorderRequest-whiteBorder);
		} else {
			pageBorder = 0;
		}

		if( pageWidthUnit <= 0 ) {
			pageWidth = numCols*totalWidth+2*pageBorder;
		} else {
			pageWidth = pageWidthUnit*UNIT_TO_POINTS;
		}

		if( pageHeightUnit <= 0 ) {
			pageHeight = numRows*totalWidth+2*pageBorder;
		} else {
			pageHeight = pageHeightUnit*UNIT_TO_POINTS;
		}

		// compute the offset to put it in the center of the page
		centerOffsetX = (pageWidth-totalWidth*numCols-2*pageBorder)/2.0;
		centerOffsetY = (pageHeight-totalWidth*numRows-2*pageBorder)/2.0;

		printHeader(imageName,fiducialWidthUnit);

		printImageDefinitions(fiducialWidthUnit);

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
				insertFiducial(row,col);
			}
		}

		out.print("  showpage\n" +
				"%%EOF\n");

		System.out.println("Saved to "+new File(outputName).getAbsolutePath());
	}

	private void printImageDefinitions( double fiducialWidthUnit ) {
		for( int i = 0; i < imagePaths.size(); i++ ) {
			String imageName = new File(imagePaths.get(i)).getName();
			ImageUInt8 image = UtilImageIO.loadImage(imagePaths.get(i), ImageUInt8.class);

			if( image == null ) {
				System.err.println("Can't find image.  Path = "+ imagePaths.get(i));
				System.exit(0);
			} else {
				System.out.println("  loaded "+imageName);
			}

			// make sure the image is square and divisible by 8
			int s = image.width - (image.width%8);
			if( image.width != s || image.height != s ) {
				ImageUInt8 tmp = new ImageUInt8(s, s);
				new FDistort(image, tmp).scaleExt().apply();
				image = tmp;
			}

			scale = image.width/innerWidth;
			ImageUInt8 binary = ThresholdImageOps.threshold(image, null, threshold, false);
			if( showPreview )
				ShowImages.showWindow(VisualizeBinaryData.renderBinary(binary, false, null), "Binary Image");

			out.println();
			out.print("  /"+getImageName(i)+" {\n" +
					"  "+binary.width+" " + binary.height + " 1 [" + scale + " 0 0 " + scale + " 0 0]\n" +
					"  {<"+binaryToHex(binary)+">} image\n" +
					"} def\n");
			out.println();
			if(printInfo) {
				out.print(" /"+getDisplayName(i)+"\n" +
						"{\n" +
						"  /Times-Roman findfont\n" + "7 scalefont setfont b1 " + (totalWidth - 10) +
						" moveto (" + imageName + "   " + fiducialWidthUnit + " "+unit.abbreviation+") show\n"+
						"} def\n" );
			}
		}
	}

	private String getImageName( int num ) {
		return String.format("drawImage%03d",num);
	}

	private String getDisplayName( int num ) {
		return String.format("displayInfo%03d",num);
	}

	private void printHeader( String inputName , double widthCM) {
		out.println("%!PS-Adobe-3.0 EPSF-3.0\n" +
				"%%Creator: BoofCV\n" +
				"%%Title: "+inputName+" w="+ widthCM +" "+unit.abbreviation+"\n" +
				"%%DocumentData: Clean7Bit\n" +
				"%%Origin: 0 0\n" +
				"%%BoundingBox: 0 0 "+pageWidth+" "+pageHeight+"\n" +
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

	private void insertFiducial(int row, int col ) {
		out.print(
				"  /originX " + (centerOffsetX + pageBorder + col * totalWidth) + " def\n" +
				"  /originY " + (centerOffsetY + pageBorder + row * totalWidth) + " def\n" +
				"  originX originY translate\n" );
		out.println();
		out.println("  drawBorder");

		int imageNum = (row*numCols+col)%imagePaths.size();

		// print out encoding information for convenience
		if(printInfo) {
			out.println("  " + getDisplayName(imageNum));
		}

		out.println("% Center then draw the image");
		out.println("  b1 b1 translate");
		out.println("  "+getImageName(imageNum));
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
