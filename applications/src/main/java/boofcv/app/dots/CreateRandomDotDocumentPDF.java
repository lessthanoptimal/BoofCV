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

package boofcv.app.dots;

import boofcv.alg.fiducial.dots.RandomDotMarkerGenerator;
import boofcv.app.PaperSize;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.generate.Unit;
import boofcv.pdf.PdfFiducialEngine;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateRandomDotDocumentPDF extends CreateFiducialDocumentPDF {

	List<List<Point2D_F64>> markers;
	@Getter RandomDotMarkerGenerator g;

	PdfFiducialEngine renderer;

	public double dotDiameter;

	public CreateRandomDotDocumentPDF( String documentName, PaperSize paper, Unit units ) {
		super(documentName, paper, units);
	}

	@Override
	protected String getMarkerType() {
		return "Uchiya Markers ";
	}

	@Override
	protected void configureRenderer( PdfFiducialEngine renderer ) {
		if (markerHeight < 0)
			throw new IllegalArgumentException("Must specify marker height even if square");
		this.renderer = renderer;
		g = new RandomDotMarkerGenerator();
		g.setRadius(UNIT_TO_POINTS*dotDiameter/2.0);
		g.getDocumentRegion().setWidth(markerWidth*UNIT_TO_POINTS);
		g.getDocumentRegion().setHeight(markerHeight*UNIT_TO_POINTS);
		g.setRender(renderer);
	}

	@Override
	protected void render( int markerIndex ) {
		List<Point2D_F64> marker = markers.get(markerIndex%markers.size());

		//  Convert the marker into document units
		List<Point2D_F64> scaled = new ArrayList<>();
		for (var p : marker) {
			p = p.copy();
			p.x *= UNIT_TO_POINTS;
			p.y *= UNIT_TO_POINTS;
			scaled.add(p);
		}
		g.render(scaled, markerWidth*UNIT_TO_POINTS, markerHeight*UNIT_TO_POINTS);

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

	public void render( List<List<Point2D_F64>> markers, int maxDots, long seed ) throws IOException {
		this.markers = markers;

		totalMarkers = markers.size();
		names = new ArrayList<>();
		for (int i = 0; i < markers.size(); i++) {
			names.add(String.format("ID: %d N: %d Seed: 0x%4X", i, maxDots, seed));
		}

		render();
	}

	@Override
	protected String createMarkerSizeString() {
		return String.format("mw: %4.1f  dd: %4.1f %2s", markerWidth,
				dotDiameter, units.getAbbreviation());
	}
}
