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
import boofcv.app.drawing.PdfFiducialEngine;
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

		PDDocumentInformation info = document.getDocumentInformation();
		info.setCreator("BoofCV");
		info.setTitle(documentName);
	}

	public void render(java.util.List<QrCode> markers ) throws IOException {
		if( markerWidth <= 0 || spaceBetween <= 0)
			throw new RuntimeException("Must specify the marker's dimensions. Width and spacing");

		float sizeBox = (markerWidth+spaceBetween)*UNIT_TO_POINTS;

		int numRows = (int)Math.floor(pageHeight/sizeBox);
		int numCols = (int)Math.floor(pageWidth/sizeBox);

		// offset used to center
		float centerX = (pageHeight-sizeBox*numRows)/2f;
		float centerY = (pageWidth-sizeBox*numCols)/2f;

		// see if multiple pages are required
		int markersPerPage = numRows*numCols;
		int numPages = (int)Math.ceil(markers.size()/(double)markersPerPage);

		int markerIndex = 0;
		for (int pageIdx = 0; pageIdx < numPages; pageIdx++) {

			PDRectangle rectangle = new PDRectangle(pageWidth, pageHeight);
			PDPage page = new PDPage(rectangle);
			document.addPage(page);
			PDPageContentStream pcs = new PDPageContentStream(document , page);

			PdfFiducialEngine r = new PdfFiducialEngine(document,pcs,markerWidth*UNIT_TO_POINTS);
			QrCodeGenerator g = new QrCodeGenerator(markerWidth*UNIT_TO_POINTS);
			g.setRender(r);

			if( showInfo ) {
				float offX = Math.min(CM_TO_POINTS,spaceBetween*CM_TO_POINTS/2);
				float offY = Math.min(CM_TO_POINTS,spaceBetween*CM_TO_POINTS/2);

				pcs.beginText();
				pcs.setFont(PDType1Font.TIMES_ROMAN,7);
				pcs.newLineAtOffset( offX, offY );
				pcs.showText(String.format("Created by BoofCV"));
				pcs.endText();
			}

			for (int row = 0; row < numRows; row++) {
				r.offsetY = centerX + row*sizeBox + UNIT_TO_POINTS*spaceBetween/2f;

				for (int col = 0; col < numCols; col++, markerIndex++) {
					if( !gridFill && markerIndex >= markers.size() )
						break;
					QrCode qr = markers.get( markerIndex%markers.size());
					r.offsetX = centerY + col*sizeBox + UNIT_TO_POINTS*spaceBetween/2f;
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
						pcs.newLineAtOffset( (float)r.offsetX, (float)r.offsetY-offset);
						pcs.showText(message);
						pcs.endText();
						pcs.beginText();
						pcs.newLineAtOffset( (float)r.offsetX, (float)r.offsetY+markerWidth*UNIT_TO_POINTS+offset-7);
						pcs.showText(String.format("%4.1f %2s",markerWidth,units.getAbbreviation()));
						pcs.endText();
					}
				}
			}
			pcs.close();
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
