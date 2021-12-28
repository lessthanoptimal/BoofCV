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

package boofcv.app.calib;

import boofcv.alg.fiducial.calib.hamminggrids.HammingGridGenerator;
import boofcv.app.PaperSize;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.generate.Unit;
import boofcv.pdf.PdfFiducialEngine;
import lombok.Getter;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.ejml.UtilEjml;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateHammingGridDocumentPDF extends CreateFiducialDocumentPDF {

	@Getter HammingGridGenerator g;

	PdfFiducialEngine renderer;

	// width of a square
	public double squareWidth;
	public ConfigHammingGrid config;

	public CreateHammingGridDocumentPDF( String documentName, PaperSize paper, Unit units ) {
		super(documentName, paper, units);
	}

	@Override
	protected String getMarkerType() {
		return "Hamming Grid ";
	}

	@Override
	protected void configureRenderer( PdfFiducialEngine renderer ) {
		if (markerHeight < 0)
			throw new IllegalArgumentException("Must specify marker height even if square");
		this.renderer = renderer;
		config.squareSize = squareWidth*UNIT_TO_POINTS;
		g = new HammingGridGenerator(config);
		g.setRender(renderer);
	}

	@Override
	protected void render( int markerIndex ) {
		g.render();

		// draw optional black box around the marker
		if (drawLineBorder) {
			try {
				drawFiducialBorder();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void drawFiducialBorder() throws IOException {
		final PDPageContentStream pcs = renderer.pcs;

		pcs.setStrokingColor(0.0f);
		float w = markerWidth*UNIT_TO_POINTS;

		float xx = (float)renderer.offsetX;
		float yy = (float)renderer.offsetY;

		pcs.moveTo(xx, yy);
		pcs.lineTo(xx, yy + w);
		pcs.lineTo(xx + w, yy + w);
		pcs.lineTo(xx + w, yy);
		pcs.lineTo(xx, yy);
		pcs.closeAndStroke();
	}

	public void render( ConfigHammingGrid config ) throws IOException {
		this.config = config;

		totalMarkers = 1;
		names = new ArrayList<>();
		names.add(String.format("rows=%d, cols=%d, dict=%s, offset=%d",
				config.numRows, config.numCols, config.markers.dictionary, config.markerOffset));

		render();
	}

	@Override
	protected String createMarkerSizeString() {
		var format = new DecimalFormat("#");
		String strSquare = UtilEjml.fancyString(squareWidth, format, 5, 2);
		String strSpace = UtilEjml.fancyString(squareWidth*config.spaceToSquare, format, 5, 2);

		return String.format("square: %s %2s, space: %s %2s", strSquare, units.getAbbreviation(), strSpace, units.getAbbreviation());
	}
}
