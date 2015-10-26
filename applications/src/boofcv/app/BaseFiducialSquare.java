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

import boofcv.struct.image.ImageUInt8;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

/**
 * <p>
 * Base class for generating square fiducials postscript documents for printing.  Fiducials are placed in a regular grid.
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
public abstract class BaseFiducialSquare {

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

	// draw an invisible border around the document.
	boolean boundaryHack = true;

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

	//============= These parameters are used to automatically generate some of the above parameters
	// No printing is allowed inside the border and this information is used to adjust offsets with the user request
	double pageBorderX = 1*CM_TO_POINTS;
	double pageBorderY = 1*CM_TO_POINTS;

	// how wide the fiducial's black border is relative to its total width
	public double blackBorderFractionalWidth;

	// name of the output file
	String outputFileName;

	// stream in which the output file is written to
	PrintStream out;

	// file type extension
	String typeExtension = "ps";

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

	public void setBlackBorderFractionalWidth(double blackBorderFractionalWidth) {
		this.blackBorderFractionalWidth = blackBorderFractionalWidth;
	}

	public void setBoundaryHack(boolean boundaryHack) {
		this.boundaryHack = boundaryHack;
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
			outputName = defaultOutputFileName();
		} else {
			outputName = this.outputFileName;
		}

		String imageName = selectDocumentName();

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
		blackBorder = fiducialBoxWidth*blackBorderFractionalWidth;
		innerWidth = fiducialBoxWidth*(1.0-2.0*blackBorderFractionalWidth);
		fiducialTotalWidth = fiducialBoxWidth +whiteBorder*2;

		//  zone in which the target can't be placed unless it will print inside the border, which is not allowed
		double deadZoneX = Math.max(0,pageBorderX-whiteBorder);
		double deadZoneY = Math.max(0,pageBorderY-whiteBorder);

		if( pageWidthUnit <= 0 ) {
			pageWidth = fiducialTotalWidth*numCols + 2*deadZoneX;
		} else {
			pageWidth = pageWidthUnit*UNIT_TO_POINTS;
		}
		if( pageHeightUnit <= 0 ) {
			pageHeight = fiducialTotalWidth*numRows + 2*deadZoneY;
		} else {
			pageHeight = pageHeightUnit*UNIT_TO_POINTS;
		}

		// Center the fiducial inside the page.  Reduce the size of the grid if required to fit it inside the
		// page, including the border
		if( centerRequested ) {
			centerPage();
		}
	}

	/**
	 * Centers the image while taking in account page border where it can't print.  Reduces the number of elements in
	 * the grid if necessary.
	 */
	private void centerPage() {
		double validX0 = pageBorderX;
		double validX1 = pageWidth-pageBorderX;
		double validY0 = pageBorderY;
		double validY1 = pageHeight-pageBorderY;

		boolean allGood = false;
		while( numCols > 0 && numRows > 0 && !allGood) {
			allGood = true;

			// center the current target inside the page
			offsetX = (pageWidth  - fiducialTotalWidth * numCols ) / 2.0;
			offsetY = (pageHeight - fiducialTotalWidth * numRows ) / 2.0;

			// Find the edges which bound the regions where printing is done
			double edgeBlackLeft = offsetX+whiteBorder;
			double edgeBlackRight = offsetX+ fiducialTotalWidth *numCols-whiteBorder;
			double edgeBlackBottom = offsetY+whiteBorder;
			double edgeBlackTop = offsetY+ fiducialTotalWidth *numRows-whiteBorder;

			if( edgeBlackLeft+1e-8 < validX0 ||edgeBlackRight-1e-8 > validX1 ) {
				allGood = false;
				numCols--;
			}

			if( edgeBlackBottom+1e-8 < validY0 ||edgeBlackTop-1e-8 > validY1 ) {
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

		int patternsPerPage = numRows*numCols;
		int numPages = (int)Math.ceil(totalPatterns()/(double)patternsPerPage);

		printHeader(documentTitle, fiducialWidthUnit, numPages);

		for (int i = 0; i < numPages; i++) {
			printPageHeader(i+1);
			if(boundaryHack)
				printInvisibleBoundary();
			int startPattern = i*patternsPerPage;
			// Just put the definitions needed for this page
			printPatternDefinitions(startPattern, patternsPerPage);

			if (printInfo) {
				// Just put the ones on this page that belong on this page
				for (int ii = 0; ii < patternsPerPage; ii++) {
					final int pattern = ii + startPattern;
					String patternName = getPatternName(pattern);
					out.print(" /" + getDisplayDef(pattern) + "\n" +
							"{\n" +
							"  /Times-Roman findfont\n" + "7 scalefont setfont b1 " + (fiducialTotalWidth - 10) +
							" moveto (" + patternName + "   " + fiducialWidthUnit + " " + unit.abbreviation + ") show\n" +
							"} def\n");
				}
			}

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
					insertFiducial(startPattern,row,col);
				}
			}

			if( printGrid )
				printGrid();

			out.print("  showpage\n");
		}
		out.print("%%EOF\n");
	}

	/**
	 * Creates definitions which will render the pattern.  Each pattern's difintion will have the name returned
	 * by {@link #getPatternPrintDef(int)}
	 * @param startPattern
	 * @param numberOfPatterns
	 */
	protected abstract void printPatternDefinitions(final int startPattern, final int numberOfPatterns);

	/**
	 * Returns the total number of unqiue patterns
	 */
	protected abstract int totalPatterns();

	protected abstract void addPattern( String name );

	/**
	 * Human readable pattern name.  Will be printed on document
	 */
	protected abstract String getPatternName( int num );

	protected String getPatternPrintDef(int num) {
		return String.format("drawImage%03d",num);
	}

	protected String getDisplayDef(int num) {
		return String.format("displayInfo%03d",num);
	}

	private void printHeader( String documentTitle , double widthCM, int totalPages) {
		out.println("%!PS-Adobe-3.0\n" +
				"%%Creator: BoofCV\n" +
				"%%DocumentMedia: Plain "+pageWidth+" "+pageHeight+" 75 white ( )\n" +
				"%%Title: "+documentTitle+" w="+ widthCM +" "+unit.abbreviation+"\n" +
				"%%DocumentData: Clean7Bit\n" +
				"%%LanguageLevel: 2\n" +
				"%%EndComments\n" +
				"%%BeginProlog\n" +
				"%%EndProlog\n" +
				"%%Pages: "+totalPages+"\n");
	}

	private void printPageHeader( int pageNumber ) {
		out.println("%%Page: "+pageNumber+" "+pageNumber+"\n" +
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
	 * Prints an invisible boundary around the document to prevent a smart cropping script from cropping the white space
	 */
	private void printInvisibleBoundary() {
		out.println(" 1.0 setgray");
		out.printf(" newpath 0 0 moveto %f 0 rlineto 0 setlinewidth stroke\n", pageWidth);
		out.printf(" newpath 0 0 moveto 0 %f lineto 0 setlinewidth stroke\n", pageHeight);
		out.printf(" newpath %f 0 moveto %f %f lineto 0 setlinewidth stroke\n", pageWidth, pageWidth, pageHeight);
		out.printf(" newpath 0 %f moveto %f %f lineto 0 setlinewidth stroke\n", pageHeight, pageWidth, pageHeight);
		out.println(" 0.0 setgray");
		out.println();
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

	private void insertFiducial(int startPattern, int row, int col ) {
		out.print(
				"  /originX " + (offsetX + col * fiducialTotalWidth) + " def\n" +
				"  /originY " + (offsetY + row * fiducialTotalWidth) + " def\n" +
				"  originX originY translate\n" );
		out.println();
		out.println("  drawBorder");

		int imageNum = (startPattern+(row*numCols+col))%totalPatterns();

		// print out encoding information for convenience
		if(printInfo) {
			out.println("  " + getDisplayDef(imageNum));
		}

		out.println("% Center then draw the image");
		out.println("  b1 b1 translate");
		out.println("  "+getPatternPrintDef(imageNum));
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

	/**
	 * Returns an automatically selected name/path for the output file
	 */
	public abstract String defaultOutputFileName();

	/**
	 * Name of the image which will go into the document.
	 */
	public abstract String selectDocumentName();
}
