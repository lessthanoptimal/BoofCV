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

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckGenerator;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.app.PaperSize;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.generate.Unit;
import boofcv.pdf.PdfFiducialEngine;
import boofcv.struct.GridShape;
import lombok.Getter;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateECoCheckDocumentPDF extends CreateFiducialDocumentPDF {

	@Getter ECoCheckGenerator g;

	PdfFiducialEngine renderer;

	// width of a square
	public double squareWidth;
	public ECoCheckUtils utils;

	public CreateECoCheckDocumentPDF( String documentName, PaperSize paper, Unit units ) {
		super(documentName, paper, units);
	}

	@Override
	protected String getMarkerType() {
		return "ECoCheck ";
	}

	@Override
	protected void configureRenderer( PdfFiducialEngine renderer ) {
		if (markerHeight < 0)
			throw new IllegalArgumentException("Must specify marker height even if square");
		this.renderer = renderer;
		g = new ECoCheckGenerator(utils);
		g.squareWidth = squareWidth*UNIT_TO_POINTS;
		g.setRender(renderer);
	}

	@Override
	protected void render( int markerIndex ) {
		g.render(markerIndex);

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

	public void render( ECoCheckUtils utils ) throws IOException {
		this.utils = utils;
		totalMarkers = utils.markers.size();

		// Create a config to generate shorthand description
		String shorthand;
		{
			GridShape shape = utils.markers.get(0);
			ConfigECoCheckMarkers config = ConfigECoCheckMarkers.singleShape(shape.rows, shape.cols, totalMarkers, 1);
			config.checksumBits = utils.codec.getChecksumBitCount();
			config.errorCorrectionLevel = utils.codec.getErrorCorrectionLevel();
			shorthand = config.compactName();
		}

		names = new ArrayList<>();
		for (int i = 0; i < totalMarkers; i++) {
			names.add(String.format("ID: %d, config=%s", i, shorthand));
		}

		render();
	}

	@Override
	protected String createMarkerSizeString() {
		return String.format("square: %4.1f %2s", squareWidth, units.getAbbreviation());
	}
}
