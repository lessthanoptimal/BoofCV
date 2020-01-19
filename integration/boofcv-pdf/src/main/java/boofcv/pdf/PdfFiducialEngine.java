/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.pdf;

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Renders fiducials to PDF documents.
 *
 * @author Peter Abeles
 */
public class PdfFiducialEngine extends FiducialRenderEngine {

	public PDDocument document;
	public PDPageContentStream pcs;
	public double offsetX,offsetY;
	public double markerWidth;

	public PdfFiducialEngine(PDDocument document, PDPageContentStream pcs, double markerWidth) {
		this.document = document;
		this.pcs = pcs;
		this.markerWidth = markerWidth;
	}

	@Override
	public void init() {

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

	@Override
	public void rectangle(double x0, double y0, double x1, double y1) {
		try {
			double width = x1-x0;
			double height = y1-y0;

			pcs.setNonStrokingColor(Color.BLACK);
			pcs.addRect(adjustX(x0), adjustY(y0)-(float)height, (float)width, (float)height);
			pcs.fill();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void draw(GrayU8 image, double x0, double y0, double x1, double y1) {
		BufferedImage buffered = new BufferedImage(image.width,image.height, BufferedImage.TYPE_BYTE_GRAY);
		ConvertBufferedImage.convertTo(image,buffered,true);

		try {
			PDImageXObject pdImage = JPEGFactory.createFromImage(document,buffered);
			pcs.drawImage(pdImage, adjustX(x0), (float)(offsetY+y0),(float)(x1-x0),(float)(y1-y0));
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