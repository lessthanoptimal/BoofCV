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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Base class for generating square fiducials EPS documents for printing.  Fiducials are placed in a regular grid.
 * The width of each element in the grid is the fiducial's width (pattern + black border) and a white border.  The
 * grid starts in the page's lower left and corner.
 * </p>
 *
 * <pre>
 * Border:  The border is a no-go zone where the fiducial can't be printed inside of.  This is only taken in account
 *          when automatic centering or layout of the grid on the page is requested.
 * Offset: Where the fiducial is offset inside the page.  Always used.  If centering is requested then the offset
 *         is automatically computed and any user provided value ignored.
 * PrintInfo: If true it will draw a string above the fiducial with the fiducial's  name and it's size
 * </pre>
 *
 * @author Peter Abeles
 */
public class BaseFiducialSquareEPS {

	// threshold for converting to a binary image
	public int threshold = 255/2;
	public double UNIT_TO_POINTS;
	public static final double CM_TO_POINTS = 72.0/2.54;
	// should it add the file name and size to the document?
	public boolean printInfo = true;

	public boolean showPreview = false;

	// base unit of input lengths
	Unit unit = Unit.CENTIMETER;

	// if true it will select the size of the grid
	boolean autofillGrid = false;
	// Request has been made to automatically center the fiducial in the page
	boolean centerRequested = true;

	// should it print the grid
	boolean printGrid = false;

	// Number of elements in the grid
	public int numCols = 1;
	public int numRows = 1;

	//============= These Parameters specify how it's printed
	// Total length of the target.  image + black border
	double fiducialBoxWidth;
	// length of the white border surrounding the fiducial
	double whiteBorder;
	// length of the black border
	double blackBorder;
	// length of the inner pattern
	double innerWidth ;
	// Length of the fiducial plus the white border
	double fiducialTotalWidth;
	// width and height of the page
	double pageWidth,pageHeight;
	// offset of the page.  basically changes the origin
	double offsetX, offsetY;

	//============= These parameters are used to automatically generate some of the above parmeters
	// No printing is allowed inside the border and this information is used to adjust offsets with the user request
	double pageBorderX = 1*CM_TO_POINTS;
	double pageBorderY = 1*CM_TO_POINTS;

	// name of the output file
	String outputFileName;

	// stream in which the output file is written to
	PrintStream out;

	// Paths to image files containing fiducial patterns
	List<String> imagePaths = new ArrayList<String>();

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public void setCentering(boolean centerFiducial) {
		this.centerRequested = centerFiducial;
	}

	public boolean isPrintGrid() {
		return printGrid;
	}

	public void setPrintGrid(boolean printGrid) {
		this.printGrid = printGrid;
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

	public void setPageBorder( double borderX , double borderY , Unit units) {
		pageBorderX = units.convert(borderX,Unit.CENTIMETER)*CM_TO_POINTS;
		pageBorderY = units.convert(borderY,Unit.CENTIMETER)*CM_TO_POINTS;
	}

	public void setOffset( double offsetX , double offsetY , Unit units) {
		this.offsetX = units.convert(offsetX,Unit.CENTIMETER)*CM_TO_POINTS;
		this.offsetY = units.convert(offsetY,Unit.CENTIMETER)*CM_TO_POINTS;
	}

	public void setOutputFileName(String outputFileName)  {
		this.outputFileName = outputFileName;
	}

	public void generateGrid( double fiducialWidth , double whiteBorder , int numCols , int numRows , PaperSize paper )
			throws IOException
	{
		double pageWidthUnit, pageHeightUnit;
		if( paper != null ) {
			pageWidthUnit = paper.getUnit().convert(paper.getWidth(), unit);
			pageHeightUnit = paper.getUnit().convert(paper.getHeight(), unit);
		} else {
			pageWidthUnit = -1;
			pageHeightUnit = -1;
		}
		this.numRows = numRows;
		this.numCols = numCols;

		if( numRows < 1 || numCols < 1 )
			this.autofillGrid = true;

		if( whiteBorder < 0 ) {
			double cm2 = Unit.CENTIMETER.convert(2.0,unit);
			whiteBorder = Math.max(cm2, fiducialWidth / 4.0);
		}
		generate(fiducialWidth, whiteBorder, pageWidthUnit, pageHeightUnit);
	}

	/**
	 * Configures and saves a PDF of the fiducial.
	 *
	 * @param fiducialWidthUnit Width of the fiducial
	 * @param whiteBorderUnit Thickness of the border around the fiducial
	 * @param pageWidthUnit Width of the document. If <= 0 the width will be automatically selected
	 * @param pageHeightUnit Height of the document. If <= 0 the height will be automatically selected
	 * @throws IOException
	 */
	private void generate(double fiducialWidthUnit, double whiteBorderUnit,
						  double pageWidthUnit, double pageHeightUnit) throws IOException {

		String outputName;
		if( this.outputFileName == null ) {
			String inputPath = imagePaths.get(0);
			File dir = new File(inputPath).getParentFile();
			outputName = new File(inputPath).getName();
			outputName = outputName.substring(0,outputName.length()-3) + "eps";
			outputName = new File(dir,outputName).getCanonicalPath();
		} else {
			outputName = this.outputFileName;
		}


		String imageName;
		if( imagePaths.size() == 1 ) {
			imageName = new File(imagePaths.get(0)).getName();
		} else {
			imageName = "Multiple Patterns";
		}

		configureDocument(fiducialWidthUnit, whiteBorderUnit, pageWidthUnit, pageHeightUnit);

		// print out the selected number in binary for debugging purposes
		out = new PrintStream(outputName);
		generateDocument(fiducialWidthUnit, imageName);

		System.out.println("Saved to "+new File(outputName).getAbsolutePath());
	}

	/**
	 * Compute how to build the documetn and setup all parameters
	 */
	private void configureDocument(double fiducialWidthUnit, double whiteBorderUnit,
								   double pageWidthUnit, double pageHeightUnit ) throws FileNotFoundException
	{
		UNIT_TO_POINTS = 72.0/(2.54* Unit.conversion(Unit.CENTIMETER, unit));

		if( autofillGrid ) {
			if( pageWidthUnit <= 0 || pageHeightUnit <=  0)
				throw new IllegalArgumentException("If autofillGrid is turned on then the page size must be specified");

			autoSelectGridSize(fiducialWidthUnit, whiteBorderUnit, pageWidthUnit, pageHeightUnit);
		}

		System.out.println("Fiducial width "+ fiducialWidthUnit +" ("+unit.abbreviation+")");

		fiducialBoxWidth = fiducialWidthUnit* UNIT_TO_POINTS;
		whiteBorder = whiteBorderUnit* UNIT_TO_POINTS;
		blackBorder = fiducialBoxWidth /4.0;
		innerWidth = fiducialBoxWidth /2.0;
		fiducialTotalWidth = fiducialBoxWidth +whiteBorder*2;

		//  zone in which the target can't be placed unless it will print inside the border, which is not allowed
		double deadZoneX = Math.max(0,pageBorderX-whiteBorder);
		double deadZoneY = Math.max(0,pageBorderY-whiteBorder);

		if( pageWidthUnit <= 0 ) {
			pageWidth = fiducialTotalWidth *numCols + deadZoneX;
		} else {
			pageWidth = pageWidthUnit*UNIT_TO_POINTS;
		}
		if( pageHeightUnit <= 0 ) {
			pageHeight = fiducialTotalWidth *numRows + deadZoneY;
		} else {
			pageHeight = pageHeightUnit*UNIT_TO_POINTS;
		}

		// Center the fiducial inside the page.  Reduce the size of the grid if required to fit it inside the
		// page, including the border
		if( centerRequested ) {
			centerPage(deadZoneX, deadZoneY);
		}
	}

	/**
	 * Centers the image while taking in account page border where it can't print.  Reduces the number of elements in
	 * the grid if necessary.
	 */
	private void centerPage(double deadZoneX, double deadZoneY) {
		double validX0 = pageBorderX;
		double validX1 = pageWidth-pageBorderX;
		double validY0 = pageBorderY;
		double validY1 = pageHeight-pageBorderY;

		boolean allGood = false;
		while( numCols > 0 && numRows > 0 && !allGood) {
			allGood = true;

			// center the current target inside the page
			offsetX = (pageWidth  - fiducialTotalWidth * numCols - 2 * deadZoneX) / 2.0;
			offsetY = (pageHeight - fiducialTotalWidth * numRows - 2 * deadZoneY) / 2.0;

			// Find the edges which bound the regions where printing is done
			double edgeBlackLeft = offsetX+whiteBorder;
			double edgeBlackRight = offsetX+ fiducialTotalWidth *numCols-whiteBorder;
			double edgeBlackBottom = offsetY+whiteBorder;
			double edgeBlackTop = offsetY+ fiducialTotalWidth *numRows-whiteBorder;

			if( edgeBlackLeft < validX0 ||edgeBlackRight > validX1 ) {
				allGood = false;
				numCols--;
			}

			if( edgeBlackBottom < validY0 ||edgeBlackTop > validY1 ) {
				allGood = false;
				numRows--;
			}
		}

		if( numCols == 0 || numRows == 0 )
			throw new IllegalArgumentException("Can't place fiducial inside the page and not go outside the page border");
	}

	/**
	 * Given the page size and other parameters figure out how many fiducials it can fit along the rows and columns
	 */
	private void autoSelectGridSize(double fiducialWidthUnit, double whiteBorderUnit, double pageWidthUnit, double pageHeightUnit) {

		// find how far away from the page's edge does it need to stay
		double pageBorderUnitX = Math.max(pageBorderX/UNIT_TO_POINTS , whiteBorderUnit);
		double pageBorderUnitY = Math.max(pageBorderY/UNIT_TO_POINTS , whiteBorderUnit);

		// area of fiducial and white border
		double totalWidthUnit = fiducialWidthUnit+2*whiteBorderUnit;

		// compute how much of the page it can use
		double effectiveX = pageWidthUnit  - 2*pageBorderUnitX + 2*whiteBorderUnit;
		double effectiveY = pageHeightUnit - 2*pageBorderUnitY + 2*whiteBorderUnit;

		this.numCols = (int)Math.floor(effectiveX/totalWidthUnit);
		this.numRows = (int)Math.floor(effectiveY/totalWidthUnit);
	}

	/**
	 * Creates an EPS document from all the specifications in the class
	 * @param fiducialWidthUnit Width of fiducial (including border) in user specified units.
	 * @param documentTitle Title of the document
	 */
	private void generateDocument(double fiducialWidthUnit, String documentTitle) {
		printHeader(documentTitle,fiducialWidthUnit);
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

		if( printGrid )
			printGrid();

		out.print("  showpage\n" +
				"%%EOF\n");
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

			double scale = image.width/innerWidth;
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
						"  /Times-Roman findfont\n" + "7 scalefont setfont b1 " + (fiducialTotalWidth - 10) +
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

	private void printHeader( String documentTitle , double widthCM) {
		out.println("%!PS-Adobe-3.0 EPSF-3.0\n" +
				"%%Creator: BoofCV\n" +
				"%%Title: "+documentTitle+" w="+ widthCM +" "+unit.abbreviation+"\n" +
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

	/**
	 * Draws the grid in light grey on the document
	 */
	private void printGrid() {
		out.println("% grid lines");

		out.print(" /drawRow { moveto "+pageWidth+" 0 rlineto 1 setlinewidth stroke} def\n");
		out.print(" /drawColumn { moveto 0 "+pageHeight+" rlineto 1 setlinewidth stroke} def\n");
		out.print(" 0.75 setgray\n");
		for (int i = 0; i <= numCols; i++) {
			double x = offsetX + i*fiducialTotalWidth;
			out.printf(" newpath %f 0 drawColumn\n",x);
		}
		for (int i = 0; i <= numRows; i++) {
			double y = offsetY + i*fiducialTotalWidth;
			out.printf(" newpath 0 %f drawRow\n",y);
		}
	}

	private void insertFiducial(int row, int col ) {
		out.print(
				"  /originX " + (offsetX + col * fiducialTotalWidth) + " def\n" +
				"  /originY " + (offsetY + row * fiducialTotalWidth) + " def\n" +
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
