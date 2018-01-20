/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.misc.Unit;
import boofcv.struct.image.GrayU8;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p>
 * Base class for generating square fiducial PDF documents for printing.  Fiducials are placed in a regular grid.
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

	// objects for writing PDF document
	PDDocument document;
	PDPage page;
	PDPageContentStream pcs;

	// threshold for converting to a binary image
	public int threshold = 255/2;
	public float UNIT_TO_POINTS;
	public static final float CM_TO_POINTS = 72.0f/2.54f;
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
	float fiducialBoxWidth;
	// length of the white border surrounding the fiducial
	float whiteBorder;
	// length of the black border
	float blackBorder;
	// length of the inner pattern
	float innerWidth ;
	// Length of the fiducial plus the white border
	float fiducialTotalWidth;
	// offset of the page.  basically changes the origin
	float offsetX, offsetY;

	// requested paper size.  If null it will be automatically selected
	PaperSize paper;

	//============= These parameters are used to automatically generate some of the above parameters
	// No printing is allowed inside the border and this information is used to adjust offsets with the user request
	float pageBorderX = 1*CM_TO_POINTS;
	float pageBorderY = 1*CM_TO_POINTS;

	// how wide the fiducial's black border is relative to its total width
	public float blackBorderFractionalWidth;

	// name of the output file
	String outputFileName;

	// file type extension
	String typeExtension = "pdf";

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

	public void setPageBorder( float borderX , float borderY , Unit units) {
		pageBorderX = (float)units.convert(borderX,Unit.CENTIMETER)*CM_TO_POINTS;
		pageBorderY = (float)units.convert(borderY,Unit.CENTIMETER)*CM_TO_POINTS;
	}

	public void setOffset( float offsetX , float offsetY , Unit units) {
		this.offsetX = (float)units.convert(offsetX,Unit.CENTIMETER)*CM_TO_POINTS;
		this.offsetY = (float)units.convert(offsetY,Unit.CENTIMETER)*CM_TO_POINTS;
	}

	public void setBlackBorderFractionalWidth(float blackBorderFractionalWidth) {
		this.blackBorderFractionalWidth = blackBorderFractionalWidth;
	}

	public void setOutputFileName(String outputFileName)  {
		this.outputFileName = outputFileName;
	}

	public void generateGrid( float fiducialWidth , float whiteBorder , int numCols , int numRows , PaperSize paper )
			throws IOException
	{
		this.paper = paper;
		this.numRows = numRows;
		this.numCols = numCols;

		if( numRows < 1 || numCols < 1 )
			this.autofillGrid = true;

		if( whiteBorder < 0 ) {
			float cm2 = (float)Unit.CENTIMETER.convert(2.0,unit);
			whiteBorder = Math.max(cm2, fiducialWidth / 4.0f);
		}
		generate(fiducialWidth, whiteBorder);
	}

	/**
	 * Configures and saves a PDF of the fiducial.
	 *
	 * @param fiducialWidthUnit Width of the fiducial
	 * @param whiteBorderUnit Thickness of the border around the fiducial
	 * @throws IOException
	 */
	private void generate(float fiducialWidthUnit, float whiteBorderUnit) throws IOException {

		String outputName;
		if( this.outputFileName == null ) {
			outputName = defaultOutputFileName();
		} else {
			outputName = this.outputFileName;
		}

		String documentName = selectDocumentName();

		configureDocument(fiducialWidthUnit, whiteBorderUnit);

		// print out the selected number in binary for debugging purposes
		document = new PDDocument();
		PDDocumentInformation info = document.getDocumentInformation();
		info.setCreator("BoofCV");
		info.setTitle(documentName+" w="+ fiducialWidthUnit +" "+unit.abbreviation);

		generateDocument(fiducialWidthUnit);

		// save and close document
		document.save(outputName);
		document.close();
		System.out.println("Saved to "+new File(outputName).getAbsolutePath());
	}

	/**
	 * Compute how to build the documetn and setup all parameters
	 */
	private void configureDocument(float fiducialWidthUnit, float whiteBorderUnit) throws FileNotFoundException
	{
		UNIT_TO_POINTS = 72.0f/(2.54f* (float)Unit.conversion(Unit.CENTIMETER, unit));

		if( autofillGrid ) {
			if( paper == null )
				throw new IllegalArgumentException("If autofillGrid is turned on then the page size must be specified");

			autoSelectGridSize(fiducialWidthUnit, whiteBorderUnit);
		}

		System.out.println("Fiducial width "+ fiducialWidthUnit +" ("+unit.abbreviation+")");

		fiducialBoxWidth = fiducialWidthUnit* UNIT_TO_POINTS;
		whiteBorder = whiteBorderUnit* UNIT_TO_POINTS;
		blackBorder = fiducialBoxWidth*blackBorderFractionalWidth;
		innerWidth = fiducialBoxWidth*(1.0f-2.0f*blackBorderFractionalWidth);
		fiducialTotalWidth = fiducialBoxWidth +whiteBorder*2;

		//  zone in which the target can't be placed unless it will print inside the border, which is not allowed
		double deadZoneX = Math.max(0,pageBorderX-whiteBorder);
		double deadZoneY = Math.max(0,pageBorderY-whiteBorder);

		if( paper == null ) {
			paper = new PaperSize((fiducialTotalWidth*numCols + 2*deadZoneX)/UNIT_TO_POINTS,
					(fiducialTotalWidth*numRows + 2*deadZoneY)/UNIT_TO_POINTS, unit);
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
		float pageWidth = (float)paper.convertWidth(unit)*UNIT_TO_POINTS;
		float pageHeight = (float)paper.convertHeight(unit)*UNIT_TO_POINTS;

		float validX0 = pageBorderX;
		float validX1 = pageWidth-pageBorderX;
		float validY0 = pageBorderY;
		float validY1 = pageHeight-pageBorderY;

		boolean allGood = false;
		while( numCols > 0 && numRows > 0 && !allGood) {
			allGood = true;

			// center the current target inside the page
			offsetX = (pageWidth  - fiducialTotalWidth * numCols ) / 2.0f;
			offsetY = (pageHeight - fiducialTotalWidth * numRows ) / 2.0f;

			// Find the edges which bound the regions where printing is done
			float edgeBlackLeft = offsetX+whiteBorder;
			float edgeBlackRight = offsetX+ fiducialTotalWidth *numCols-whiteBorder;
			float edgeBlackBottom = offsetY+whiteBorder;
			float edgeBlackTop = offsetY+ fiducialTotalWidth *numRows-whiteBorder;

			if( edgeBlackLeft+1e-4f < validX0 ||edgeBlackRight-1e-4f > validX1 ) {
				allGood = false;
				numCols--;
			}

			if( edgeBlackBottom+1e-4f < validY0 ||edgeBlackTop-1e-4f > validY1 ) {
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
	private void autoSelectGridSize(double fiducialWidthUnit, double whiteBorderUnit) {

		double pageWidthUnit = paper.unit.convert(paper.width,unit);
		double pageHeightUnit = paper.unit.convert(paper.height,unit);

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
	 */
	private void generateDocument( float fiducialWidthUnit ) throws IOException {

		int patternsPerPage = numRows*numCols;
		int numPages = (int)Math.ceil(totalPatterns()/(double)patternsPerPage);

		float pageWidth = (float)(paper.convertWidth(unit)*UNIT_TO_POINTS);
		float pageHeight = (float)(paper.convertHeight(unit)*UNIT_TO_POINTS);
		PDRectangle rectangle = new PDRectangle(pageWidth, pageHeight);

		for (int i = 0; i < numPages; i++) {
			page = new PDPage(rectangle);
			document.addPage(page);
			pcs = new PDPageContentStream(document , page);

			int startPattern = i*patternsPerPage;

			for (int row = 0; row < numRows; row++) {
				for (int col = 0; col < numCols; col++) {
					insertFiducial(startPattern,row,col,fiducialWidthUnit);
				}
			}

			if( printGrid )
				printGrid();

			if (printInfo) {
				pcs.beginText();
				pcs.setFont(PDType1Font.TIMES_ROMAN,7);
				pcs.newLineAtOffset( offsetX+1, offsetY+1 );
				pcs.showText(String.format("Page Size: %4.1f by %4.1f %s",
						paper.width,paper.height,paper.unit.abbreviation));
				pcs.endText();
			}

			pcs.close();
		}
	}

	/**
	 * Returns the total number of unqiue patterns
	 */
	protected abstract int totalPatterns();

	protected abstract void addPattern( String name );

	/**
	 * Human readable pattern name.  Will be printed on document
	 */
	protected abstract String getPatternName( int num );

	protected String getDisplayDef(int num) {
		return String.format("displayInfo%03d",num);
	}


	/**
	 * Draws the grid in light grey on the document
	 */
	private void printGrid() throws IOException {
		float pageWidth = (float)paper.convertWidth(unit)*UNIT_TO_POINTS;
		float pageHeight = (float)paper.convertHeight(unit)*UNIT_TO_POINTS;

//		pcs.setLineCapStyle(1);
		pcs.setStrokingColor(0.75);

		for (int i = 0; i <= numCols; i++) {
			float x = offsetX + i*fiducialTotalWidth;
			pcs.moveTo(x,0);
			pcs.lineTo(x,pageHeight);
		}
		for (int i = 0; i <= numRows; i++) {
			float y = offsetY + i*fiducialTotalWidth;
			pcs.moveTo(0,y);
			pcs.lineTo(pageWidth,y);
		}
		pcs.closeAndStroke();
	}

	private void insertFiducial(int patternCount, int row, int col , float fiducialWidthUnit ) throws IOException {
		// Chance coordinate for this fiducial
		pcs.saveGraphicsState();
		pcs.transform(Matrix.getTranslateInstance(
				offsetX + col * fiducialTotalWidth, offsetY + row * fiducialTotalWidth));

		// Draw the black border around the fiducial
		pcs.setNonStrokingColor(Color.BLACK);
		float ow = innerWidth + 2 * blackBorder;
		float wb = whiteBorder+blackBorder;
		pcs.moveTo(whiteBorder,whiteBorder);
		pcs.lineTo(whiteBorder+ow,whiteBorder);
		pcs.lineTo(whiteBorder+ow,whiteBorder+ow);
		pcs.lineTo(whiteBorder,whiteBorder+ow);
		pcs.moveTo(wb,wb);
		pcs.lineTo(wb,wb+innerWidth);
		pcs.lineTo(wb+innerWidth,wb+innerWidth);
		pcs.lineTo(wb+innerWidth,wb);
		pcs.fillEvenOdd();

		int patternID = (patternCount+(row*numCols+col))%totalPatterns();

		// print out encoding information for convenience

		if (printInfo) {
			pcs.beginText();
				pcs.setFont(PDType1Font.TIMES_ROMAN,7);
			String patternName = getPatternName(patternID);
			pcs.newLineAtOffset( whiteBorder+blackBorder, fiducialTotalWidth-10 );
			pcs.showText(patternName + ",  width: " + fiducialWidthUnit + " " + unit.abbreviation);
			pcs.endText();
		}

		// translate to where the pattern will be drawn
		pcs.transform(Matrix.getTranslateInstance(wb, wb));
		drawPattern(patternID);
		pcs.restoreGraphicsState();
	}

	public static String binaryToHex( GrayU8 binary ) {

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

	protected abstract void drawPattern( int patternID ) throws IOException;

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
