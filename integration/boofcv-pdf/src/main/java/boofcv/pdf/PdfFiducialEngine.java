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

package boofcv.pdf;

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Renders fiducials to PDF documents.
 *
 * @author Peter Abeles
 */
public class PdfFiducialEngine extends FiducialRenderEngine {

	public PDDocument document;
	public PDPageContentStream pcs;
	public double offsetX, offsetY;
	public double markerWidth;
	public double markerHeight;

	public Color markerColor = Color.BLACK;

	public PdfFiducialEngine( PDDocument document, PDPageContentStream pcs,
							  double markerWidth, double markerHeight ) {
		this.document = document;
		this.pcs = pcs;
		this.markerWidth = markerWidth;
		this.markerHeight = markerHeight;
	}

	@Override
	public void init() {

	}

	@Override public void setGray( double value ) {
		if (value == 0.0)
			markerColor = Color.BLACK;
		else if (value == 1.0)
			markerColor = Color.WHITE;
		else {
			markerColor = new Color((float)value, (float)value, (float)value);
		}
	}

	@Override
	public void circle( double cx, double cy, double r ) {
		try {
			pcs.setNonStrokingColor(markerColor);

			final float k = 0.552284749831f;
			pcs.moveTo(adjustX(cx - r), adjustY(cy));
			pcs.curveTo(adjustX(cx - r), adjustY(cy + k*r), adjustX(cx - k*r), adjustY(cy + r), adjustX(cx), adjustY(cy + r));
			pcs.curveTo(adjustX(cx + k*r), adjustY(cy + r), adjustX(cx + r), adjustY(cy + k*r), adjustX(cx + r), adjustY(cy));
			pcs.curveTo(adjustX(cx + r), adjustY(cy - k*r), adjustX(cx + k*r), adjustY(cy - r), adjustX(cx), adjustY(cy - r));
			pcs.curveTo(adjustX(cx - k*r), adjustY(cy - r), adjustX(cx - r), adjustY(cy - k*r), adjustX(cx - r), adjustY(cy));
			pcs.fill();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void square( double x0, double y0, double width0, double thickness ) {
		try {
			pcs.setNonStrokingColor(markerColor);

			pcs.moveTo(adjustX(x0), adjustY(y0));
			pcs.lineTo(adjustX(x0), adjustY(y0 + width0));
			pcs.lineTo(adjustX(x0 + width0), adjustY(y0 + width0));
			pcs.lineTo(adjustX(x0 + width0), adjustY(y0));
			pcs.lineTo(adjustX(x0), adjustY(y0));

			float x = (float)(x0 + thickness);
			float y = (float)(y0 + thickness);
			float w = (float)(width0 - thickness*2);

			pcs.moveTo(adjustX(x), adjustY(y));
			pcs.lineTo(adjustX(x), adjustY(y + w));
			pcs.lineTo(adjustX(x + w), adjustY(y + w));
			pcs.lineTo(adjustX(x + w), adjustY(y));
			pcs.lineTo(adjustX(x), adjustY(y));

			pcs.fillEvenOdd();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void rectangle( double x0, double y0, double x1, double y1 ) {
		try {
			double width = x1-x0;
			double height = y1-y0;

			pcs.setNonStrokingColor(markerColor);
			pcs.addRect(adjustX(x0), adjustY(y0) - (float)height, (float)width, (float)height);
			pcs.fill();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void draw( GrayU8 image, double x0, double y0, double x1, double y1 ) {
		BufferedImage buffered = new BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_GRAY);
		ConvertBufferedImage.convertTo(image, buffered, true);

		try {
			PDImageXObject pdImage = JPEGFactory.createFromImage(document, buffered);
			pcs.drawImage(pdImage, adjustX(x0), (float)(offsetY + y0), (float)(x1 - x0), (float)(y1 - y0));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void inputToDocument( double x, double y, Point2D_F64 document ) {
		document.x = adjustX(x);
		document.y = adjustY(y);
	}

	public float adjustX( double x ) {
		return (float)(offsetX + x);
	}

	public float adjustY( double y ) {
		return (float)(offsetY + markerHeight - y);
	}
}
