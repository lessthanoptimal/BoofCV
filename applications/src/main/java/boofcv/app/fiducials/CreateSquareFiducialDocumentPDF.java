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

import boofcv.alg.fiducial.square.FiducialSquareGenerator;
import boofcv.app.PaperSize;
import boofcv.generate.Unit;
import boofcv.pdf.PdfFiducialEngine;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.DogArray_I64;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateSquareFiducialDocumentPDF extends CreateFiducialDocumentPDF {

	private FiducialSquareGenerator g;

	public float blackBorderFractionalWidth;

	@Nullable DogArray_I64 binaryPatterns;
	int gridWidth;
	@Nullable java.util.List<GrayU8> imagePatterns;

	String markerType = "";

	public CreateSquareFiducialDocumentPDF( String documentName, PaperSize paper, Unit units ) {
		super(documentName, paper, units);
	}

	@Override
	protected String getMarkerType() {
		return markerType;
	}

	public void render( java.util.List<String> names, DogArray_I64 patterns, int gridWidth ) throws IOException {
		this.names = names;
		binaryPatterns = patterns;
		this.gridWidth = gridWidth;
		imagePatterns = null;
		totalMarkers = binaryPatterns.size;
		render();
	}

	public void render( java.util.List<String> names, java.util.List<GrayU8> patterns ) throws IOException {
		this.names = names;
		binaryPatterns = null;
		imagePatterns = patterns;
		totalMarkers = imagePatterns.size();
		render();
	}

	@Override
	protected void configureRenderer( PdfFiducialEngine pdfengine ) {
		g = new FiducialSquareGenerator(pdfengine);
		g.setBlackBorder(blackBorderFractionalWidth);
		g.setMarkerWidth(markerWidth*UNIT_TO_POINTS);
	}

	@Override
	protected void render( int index ) {
		if (binaryPatterns != null) {
			g.generate(binaryPatterns.get(index), gridWidth);
		} else if (imagePatterns != null) {
			g.generate(imagePatterns.get(index));
		} else {
			throw new RuntimeException("BUG");
		}
	}
}
