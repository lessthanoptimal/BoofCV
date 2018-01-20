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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;

import static georegression.metric.UtilAngle.radian;

/**
 * Generates the actual calibration target.
 *
 * @author Peter Abeles
 */
public class CreateCalibrationTargetGenerator {

	// objects for writing PDF document
	PDDocument document;
	PDPage page;
	PDPageContentStream pcs;

	public boolean sendToPrinter = false;

	PaperSize paper;
	int rows,cols;
	Unit units;

	boolean showInfo = true;

	float patternWidth;
	float patternHeight;

	String documentName;

	public float UNIT_TO_POINTS;
	public static final float CM_TO_POINTS = 72.0f/2.54f;

	public CreateCalibrationTargetGenerator(String documentName , PaperSize paper, int rows , int cols , Unit units ) {
		this.paper = paper;
		this.rows = rows;
		this.cols = cols;
		this.units = units;
		this.documentName = documentName;

		UNIT_TO_POINTS = (float)units.getUnitToMeter()*100.0f*CM_TO_POINTS;

		document = new PDDocument();
		float pageWidth = (float)(paper.convertWidth(units)*UNIT_TO_POINTS);
		float pageHeight = (float)(paper.convertHeight(units)*UNIT_TO_POINTS);
		PDRectangle rectangle = new PDRectangle(pageWidth, pageHeight);
		page = new PDPage(rectangle);
		document.addPage(page);
		try {
			pcs = new PDPageContentStream(document , page);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void chessboard( float squareWidth ) throws IOException {
		float squarePoints = squareWidth*UNIT_TO_POINTS;
		patternWidth = cols*squarePoints;
		patternHeight = rows*squarePoints;

		printHeader("Chessboard "+rows+"x"+cols+", squares "+squareWidth+" "+units.abbreviation);

		pcs.setNonStrokingColor(Color.BLACK);
		for (int col = 0; col < cols; col++) {
			float x = squarePoints*col;
			for (int row = col%2; row < rows; row+=2) {
				float y = squarePoints*row;
				pcs.addRect(x,y,squarePoints,squarePoints);
				pcs.fill();
			}
		}

		close();
	}

	public void squareGrid( float squareWidth , float spacing ) throws IOException {
		float squarePoints = squareWidth*UNIT_TO_POINTS;
		float spacingPoints = spacing*UNIT_TO_POINTS;
		patternWidth = cols*squarePoints +(cols-1)*spacingPoints;
		patternHeight = rows*squarePoints+(rows-1)*spacingPoints;

		printHeader("Square Grid "+rows+"x"+cols+", squares "+squareWidth+", space "+spacing+" "+units.abbreviation);

		pcs.setNonStrokingColor(Color.BLACK);
		for (int col = 0; col < cols; col++) {
			float x = (squarePoints+spacingPoints)*col;
			for (int row = 0; row < rows; row++) {
				float y = (squarePoints+spacingPoints)*row;
				pcs.addRect(x,y,squarePoints,squarePoints);
				pcs.fill();
			}
		}

		close();
	}

	public void binaryGrid( float squareWidth , float spacing ) {
		System.out.println("Binary grid not yet supported because the standard isn't fully defined yet");
		System.exit(0);
	}

	public void circleHexagonal(float diameter , float centerDistance ) throws IOException {
		float diameterPoints = diameter*UNIT_TO_POINTS;

		float centerDistancePoints = centerDistance * UNIT_TO_POINTS;

		float separationPointsW = centerDistancePoints;
		float separationPointsH = 2.0f*centerDistancePoints*(float)Math.sin(radian(60));

		patternWidth = ((cols-1)/2.0f)*separationPointsW + diameterPoints;
		patternHeight = ((rows-1)/2.0f)*separationPointsH + diameterPoints;

		printHeader("Hexagonal Circle "+rows+"x"+cols+", diameter "+diameter+", center distance "+centerDistance+" "+units.abbreviation);

		pcs.setNonStrokingColor(Color.BLACK);
		for (int col = 0; col < cols; col++) {
			float x = diameterPoints/2+(separationPointsW/2)*col;
			for (int row = col%2; row < rows; row+=2) {
				float y = diameterPoints/2+(separationPointsH/2)*row;
				drawCircle(x,y,diameterPoints/2);
			}
		}

		close();
	}

	public void circleGrid( float diameter , float centerDistance ) throws IOException {
		float diameterPoints = diameter*UNIT_TO_POINTS;
		float separationPoints = centerDistance*UNIT_TO_POINTS;
		patternWidth = (cols-1)*separationPoints+diameterPoints;
		patternHeight = (rows-1)*separationPoints+diameterPoints;

		printHeader("Grid Circle "+rows+"x"+cols+", diameter "+diameter+", center distance "+centerDistance+" "+units.abbreviation);

		pcs.setNonStrokingColor(Color.BLACK);
		for (int col = 0; col < cols; col++) {
			float x = diameterPoints/2+(separationPoints)*col;
			for (int row = 0; row < rows; row++) {
				float y = diameterPoints/2+(separationPoints)*row;
				drawCircle(x,y,diameterPoints/2);
			}
		}

		close();
	}
	private void drawCircle( float cx, float cy, float r) throws IOException {
		final float k = 0.552284749831f;
		pcs.moveTo(cx - r, cy);
		pcs.curveTo(cx - r, cy + k * r, cx - k * r, cy + r, cx, cy + r);
		pcs.curveTo(cx + k * r, cy + r, cx + r, cy + k * r, cx + r, cy);
		pcs.curveTo(cx + r, cy - k * r, cx + k * r, cy - r, cx, cy - r);
		pcs.curveTo(cx - k * r, cy - r, cx - r, cy - k * r, cx - r, cy);
		pcs.fill();
	}


	private void printHeader( String documentTitle ) throws IOException {

		PDDocumentInformation info = document.getDocumentInformation();
		info.setCreator("BoofCV");
		info.setTitle(documentTitle);

		float pageWidth = (float)paper.convertWidth(units)*UNIT_TO_POINTS;
		float pageHeight = (float)paper.convertHeight(units)*UNIT_TO_POINTS;

		// center the pattern on the page
		pcs.transform(Matrix.getTranslateInstance((pageWidth-patternWidth)/2.0f, (pageHeight-patternHeight)/2.0f));
		// NOTE: Doing transform first ensure that the info is visible when sent to printer
		//       for some reason

		if( showInfo ) {
			float offX = Math.max(CM_TO_POINTS,(pageWidth-patternWidth)/4);
			float offY = Math.max(CM_TO_POINTS,(pageHeight-patternHeight)/4);

			pcs.beginText();
			pcs.setNonStrokingColor(Color.DARK_GRAY);
			pcs.setFont(PDType1Font.TIMES_ROMAN,7);
			pcs.newLineAtOffset( -offX, -offY );
			pcs.showText(String.format("BoofCV: %s",documentTitle));
			pcs.endText();
		}

	}


	private void close() throws IOException {
		pcs.close();

		try {
			if (sendToPrinter) {
				PrinterJob job = PrinterJob.getPrinterJob();
				job.setPageable(new PDFPageable(document));
				if (job.printDialog()) {
					job.print();
				}
			} else {
				document.save(documentName);
			}
		} catch (PrinterException e) {
			throw new IOException(e);
		} finally {
			document.close();
		}
	}

	public void setShowInfo(boolean showInfo) {
		this.showInfo = showInfo;
	}
}
