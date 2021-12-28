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

import boofcv.alg.fiducial.square.FiducialSquareHammingGenerator;
import boofcv.app.PaperSize;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.generate.Unit;
import boofcv.pdf.PdfFiducialEngine;
import org.ddogleg.struct.DogArray_I32;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateSquareHammingDocumentPDF extends CreateFiducialDocumentPDF {

	private FiducialSquareHammingGenerator g;

	public ConfigHammingMarker config = new ConfigHammingMarker();

	DogArray_I32 markerIDs = new DogArray_I32();

	String markerType = "";

	public CreateSquareHammingDocumentPDF( String documentName, PaperSize paper, Unit units ) {
		super(documentName, paper, units);
	}

	@Override
	protected String getMarkerType() {
		return markerType;
	}

	public void render( DogArray_I32 markerIDs ) throws IOException {
		markerType = config.dictionary.name()+" ";
		this.markerIDs.resize(markerIDs.size);
		totalMarkers = markerIDs.size;
		names = new ArrayList<>();
		for (int i = 0; i < markerIDs.size; i++) {
			// make sure the marker ID is valid. This is better than crashing. Should filter before it gets here.
			int id = markerIDs.get(i);
			id = Math.min(config.encoding.size()-1, id);

			// add the marker
			this.markerIDs.set(i, id);
			names.add(id+"");
		}
		render();
	}

	@Override
	protected void configureRenderer( PdfFiducialEngine pdfengine ) {
		g = new FiducialSquareHammingGenerator(config);
		g.setRenderer(pdfengine);
		g.setMarkerWidth(markerWidth*UNIT_TO_POINTS);
	}

	@Override
	protected void render( int index ) {
		g.generate(markerIDs.get(index));
	}
}
