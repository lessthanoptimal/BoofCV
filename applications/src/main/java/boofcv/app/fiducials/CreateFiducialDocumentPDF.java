/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.app.fiducials;

import boofcv.app.PaperSize;
import boofcv.generate.Unit;
import boofcv.pdf.PdfFiducialEngine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;

/**
 * Generates the fiducial PDF Document
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class CreateFiducialDocumentPDF {

	// objects for writing PDF document
	protected PDDocument document;

	protected PaperSize paper;
	protected Unit units;

	public boolean showInfo = true;
	public boolean gridFill = false;
	public boolean drawGrid = false;
	public boolean drawLineBorder = false;

	public float markerWidth;
	// If > 0 then it specifies the height, otherwise a square marker is assumed.
	public float markerHeight = -1.0f;
	public float spaceBetween;

	String documentName;

	public float UNIT_TO_POINTS;
	public static final float CM_TO_POINTS = 72.0f/2.54f;

	float pageWidth, pageHeight;

	protected int totalMarkers;

	// name of each pattern
	protected java.util.List<String> names;

	protected CreateFiducialDocumentPDF( String documentName, PaperSize paper, Unit units ) {
		this.paper = paper;
		this.units = units;
		this.documentName = documentName;

		// ensure that it has the correct suffix
		if (!documentName.toLowerCase().endsWith(".pdf")) {
			this.documentName += ".pdf";
		}

		UNIT_TO_POINTS = (float)units.getUnitToMeter()*100.0f*CM_TO_POINTS;

		document = new PDDocument();
		pageWidth = (float)(paper.convertWidth(units)*UNIT_TO_POINTS);
		pageHeight = (float)(paper.convertHeight(units)*UNIT_TO_POINTS);

		PDDocumentInformation info = document.getDocumentInformation();
		info.setCreator("BoofCV");
		info.setTitle(documentName);
	}

	public void render() throws IOException {
		if (markerWidth <= 0 || spaceBetween <= 0)
			throw new RuntimeException("Must specify the marker's dimensions. Width and spacing");

		float markerHeight = this.markerHeight > 0 ? this.markerHeight : this.markerWidth;

		float sizeBoxX = (markerWidth + spaceBetween)*UNIT_TO_POINTS;
		float sizeBoxY = (markerHeight + spaceBetween)*UNIT_TO_POINTS;

		int numRows = (int)Math.floor(pageHeight/sizeBoxY);
		int numCols = (int)Math.floor(pageWidth/sizeBoxX);

		if (numRows == 0 || numCols == 0) {
			throw new IOException("Marker too big to fit on a single page.");
		}

		// center the marker better if it doesn't fill the entire page
		if (!gridFill && totalMarkers < numCols*numRows) {
			int minRows = totalMarkers/numCols;
			if (totalMarkers%numCols > 0)
				minRows++;

			numCols = Math.min(totalMarkers, numCols);
			numRows = Math.min(minRows, numRows);
		}

		// offset used to center
		float centerX = (pageHeight - sizeBoxY*numRows)/2f;
		float centerY = (pageWidth - sizeBoxX*numCols)/2f;

		// see if multiple pages are required
		int markersPerPage = numRows*numCols;
		int numPages = (int)Math.ceil(totalMarkers/(double)markersPerPage);

		int markerIndex = 0;
		for (int pageIdx = 0; pageIdx < numPages; pageIdx++) {

			PDRectangle rectangle = new PDRectangle(pageWidth, pageHeight);
			PDPage page = new PDPage(rectangle);
			document.addPage(page);
			PDPageContentStream pcs = new PDPageContentStream(document, page);
			PdfFiducialEngine r = new PdfFiducialEngine(document, pcs,
					markerWidth*UNIT_TO_POINTS, markerHeight*UNIT_TO_POINTS);
			configureRenderer(r);

			if (showInfo) {
				// Print info on the right side of the document
				float offX = Math.min(CM_TO_POINTS, spaceBetween*CM_TO_POINTS/2);
				float offY = Math.min(CM_TO_POINTS, spaceBetween*CM_TO_POINTS/2);

				int fontSize = 7;
				String text = String.format("%sCreated by BoofCV", getMarkerType());
				float textWidth = PDType1Font.TIMES_ROMAN.getStringWidth(text)/1000*fontSize;

				pcs.beginText();
				pcs.setFont(PDType1Font.TIMES_ROMAN, fontSize);
				pcs.newLineAtOffset(pageWidth - textWidth - offX, offY);
				pcs.showText(text);
				pcs.endText();
			}

			for (int row = 0; row < numRows; row++) {
				r.offsetY = centerX + row*sizeBoxY + UNIT_TO_POINTS*spaceBetween/2f;

				for (int col = 0; col < numCols; col++, markerIndex++) {
					if (!gridFill && markerIndex >= totalMarkers)
						break;
					r.offsetX = centerY + col*sizeBoxX + UNIT_TO_POINTS*spaceBetween/2f;
					render(markerIndex%totalMarkers);

					if (showInfo) {
						float offset = 12;

						int maxLength = (int)(markerWidth*UNIT_TO_POINTS)/4;
						String message = names.get(markerIndex%totalMarkers);
						if (message.length() > maxLength) {
							message = message.substring(0, maxLength);
						}

						pcs.beginText();
						pcs.setNonStrokingColor(Color.GRAY);
						pcs.setFont(PDType1Font.TIMES_ROMAN, 7);
						pcs.newLineAtOffset((float)r.offsetX, (float)r.offsetY - offset);
						pcs.showText(message);
						pcs.endText();
						pcs.beginText();
						pcs.newLineAtOffset((float)r.offsetX, (float)r.offsetY + markerHeight*UNIT_TO_POINTS + offset - 7);
						pcs.showText(createMarkerSizeString());
						pcs.endText();
					}
				}
			}

			if (drawGrid) {
				printGrid(pcs, centerY, centerX, numRows, numCols, sizeBoxX, sizeBoxY);
			}

			pcs.close();
		}
	}

	protected abstract String getMarkerType();

	protected String createMarkerSizeString() {
		if (this.markerHeight <= 0) {
			return String.format("%4.1f %2s", markerWidth, units.getAbbreviation());
		} else {
			return String.format("%4.1f x %4.1f %2s", markerWidth, markerHeight, units.getAbbreviation());
		}
	}

	protected abstract void configureRenderer( PdfFiducialEngine pdfengine );

	protected abstract void render( int whichFiducial );

	/**
	 * Draws the grid in light grey on the document
	 */
	private void printGrid( PDPageContentStream pcs, float offsetX, float offsetY,
							int numRows, int numCols, float sizeBoxX, float sizeBoxY ) throws IOException {
		float pageWidth = (float)paper.convertWidth(units)*UNIT_TO_POINTS;
		float pageHeight = (float)paper.convertHeight(units)*UNIT_TO_POINTS;

//		pcs.setLineCapStyle(1);
		pcs.setStrokingColor(0.75);

		for (int i = 0; i <= numCols; i++) {
			float x = offsetX + i*sizeBoxX;
			pcs.moveTo(x, 0);
			pcs.lineTo(x, pageHeight);
		}
		for (int i = 0; i <= numRows; i++) {
			float y = offsetY + i*sizeBoxY;
			pcs.moveTo(0, y);
			pcs.lineTo(pageWidth, y);
		}
		pcs.closeAndStroke();
	}

	public void sendToPrinter() {
		try {
			PrinterJob job = PrinterJob.getPrinterJob();
			job.setPageable(new PDFPageable(document));
			if (job.printDialog()) {
				job.print();
			}
			document.close();
		} catch (IOException | PrinterException e) {
			throw new RuntimeException(e);
		}
	}

	public void saveToDisk() throws IOException {
		document.save(documentName);
		document.close();
	}
}
