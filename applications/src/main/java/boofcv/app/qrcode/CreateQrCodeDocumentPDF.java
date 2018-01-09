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

package boofcv.app.qrcode;

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeGenerator;
import boofcv.app.PaperSize;
import boofcv.misc.Unit;
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
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
public class CreateQrCodeDocumentPDF {

	// objects for writing PDF document
	PDDocument document;
	PDPage page;
	PDPageContentStream pcs;

	PaperSize paper;
	Unit units;

	public boolean showInfo = true;
	public boolean gridFill = false;

	public float markerWidth;
	public float spaceBetween;

	String documentName;

	public float UNIT_TO_POINTS;
	public static final float CM_TO_POINTS = 72.0f/2.54f;

	float pageWidth,pageHeight;

	public CreateQrCodeDocumentPDF(String documentName , PaperSize paper, Unit units ) {
		this.paper = paper;
		this.units = units;
		this.documentName = documentName;

		// ensure that it has the correct suffix
		if( !documentName.toLowerCase().endsWith(".pdf")) {
			this.documentName += ".pdf";
		}

		UNIT_TO_POINTS = (float)units.getUnitToMeter()*100.0f*CM_TO_POINTS;

		document = new PDDocument();
		pageWidth = (float)(paper.convertWidth(units)*UNIT_TO_POINTS);
		pageHeight = (float)(paper.convertHeight(units)*UNIT_TO_POINTS);
		PDRectangle rectangle = new PDRectangle(pageWidth, pageHeight);
		page = new PDPage(rectangle);
		document.addPage(page);
		try {
			pcs = new PDPageContentStream(document , page);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void render(java.util.List<QrCode> markers ) throws IOException {
		if( markerWidth <= 0 || spaceBetween <= 0)
			throw new RuntimeException("Must specify the marker's dimensions. Width and spacing");

		printHeader();
		float sizeBox = (markerWidth+spaceBetween)*UNIT_TO_POINTS;

		int numRows = (int)Math.floor(pageHeight/sizeBox);
		int numCols = (int)Math.floor(pageWidth/sizeBox);

		if( !gridFill) {
			numRows = Math.max(1,markers.size()/numCols);
			if( numRows == 1 ) {
				numCols = Math.max(1,markers.size()%numCols);
			}
		}

		// offset used to center
		float centerX = (pageHeight-sizeBox*numRows)/2f;
		float centerY = (pageWidth-sizeBox*numCols)/2f;

		int index = 0;
		Generator g = new Generator(markerWidth*UNIT_TO_POINTS);
		for (int row = 0; row < numRows; row++) {
			g.offsetY = centerX + row*sizeBox + UNIT_TO_POINTS*spaceBetween/2f;

			for (int col = 0; col < numCols; col++, index++) {
				if( !gridFill && index >= markers.size() )
					break;
				QrCode qr = markers.get( index%markers.size());
				g.offsetX = centerY + col*sizeBox + UNIT_TO_POINTS*spaceBetween/2f;
				g.render(qr);

				if( showInfo ) {
					float offset = UNIT_TO_POINTS*spaceBetween/4f;

					int maxLength = (int)(markerWidth*UNIT_TO_POINTS)/4;
					String message = qr.message.toString();
					if( message.length() > maxLength ) {
						message = message.substring(0,maxLength);
					}

					pcs.beginText();
					pcs.setNonStrokingColor(Color.GRAY);
					pcs.setFont(PDType1Font.TIMES_ROMAN,7);
					pcs.newLineAtOffset( (float)g.offsetX, (float)g.offsetY-offset);
					pcs.showText(message);
					pcs.endText();
					pcs.beginText();
					pcs.newLineAtOffset( (float)g.offsetX, (float)g.offsetY+markerWidth*UNIT_TO_POINTS+offset-7);
					pcs.showText(String.format("%4.1f %2s",markerWidth,units.getAbbreviation()));
					pcs.endText();
				}
			}
		}
		pcs.close();
	}

	private class Generator extends QrCodeGenerator {

		public double offsetX,offsetY;

		public Generator(double markerWidth ) {
			super(markerWidth);
		}

		@Override
		public void init() {}

		@Override
		public void square(double x0, double y0, double width) {
			try {
				pcs.setNonStrokingColor(Color.BLACK);
				pcs.addRect(adjustX(x0), adjustY(y0)-(float)width, (float)width, (float)width);
				pcs.fill();
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void square(double x0, double y0, double width0, double thickness) {
			try {
				pcs.setNonStrokingColor(Color.BLACK);

				pcs.moveTo(adjustX(x0), adjustY(y0));
				pcs.lineTo(adjustX(x0), adjustY(y0+width0));
				pcs.lineTo(adjustX(x0+width0), adjustY(y0+width0));
				pcs.lineTo(adjustX(x0+width0), adjustY(y0));
				pcs.lineTo(adjustX(x0), adjustY(y0));

				float x = (float)(x0+thickness);
				float y = (float)(y0+thickness);
				float w = (float)(width0-thickness*2);

				pcs.moveTo(adjustX(x),adjustY(y));
				pcs.lineTo(adjustX(x),adjustY(y+w));
				pcs.lineTo(adjustX(x+w),adjustY(y+w));
				pcs.lineTo(adjustX(x+w),adjustY(y));
				pcs.lineTo(adjustX(x),adjustY(y));

				pcs.fillEvenOdd();
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}

		private float adjustX( double x ) {
			return (float)(offsetX + x);
		}
		private float adjustY( double y ) {
			return (float)(offsetY + markerWidth - y);
		}

	}

	private void printHeader() throws IOException {

		PDDocumentInformation info = document.getDocumentInformation();
		info.setCreator("BoofCV");
		info.setTitle(documentName);

		if( showInfo ) {
			float offX = Math.min(CM_TO_POINTS,spaceBetween*CM_TO_POINTS/2);
			float offY = Math.min(CM_TO_POINTS,spaceBetween*CM_TO_POINTS/2);

			pcs.beginText();
			pcs.setFont(PDType1Font.TIMES_ROMAN,7);
			pcs.newLineAtOffset( offX, offY );
			pcs.showText(String.format("Created by BoofCV"));
			pcs.endText();
		}
	}

	public void sendToPrinter() throws PrinterException, IOException {
		PrinterJob job = PrinterJob.getPrinterJob();
		job.setPageable(new PDFPageable(document));
		if (job.printDialog()) {
			job.print();
		}
		document.close();
	}

	public void saveToDisk() throws IOException {
		document.save(documentName);
		document.close();
	}

	public void setShowInfo(boolean showInfo) {
		this.showInfo = showInfo;
	}
}
