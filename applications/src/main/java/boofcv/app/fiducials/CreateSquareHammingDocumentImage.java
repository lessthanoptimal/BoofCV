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

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.square.FiducialSquareHammingGenerator;
import boofcv.factory.fiducial.ConfigHammingMarker;
import org.ddogleg.struct.DogArray_I32;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
public class CreateSquareHammingDocumentImage extends CreateFiducialDocumentImage {
	private FiducialSquareHammingGenerator g;
	FiducialImageEngine render = new FiducialImageEngine();
	public ConfigHammingMarker config = new ConfigHammingMarker();
	int whiteBorderPixels;

	public CreateSquareHammingDocumentImage( String documentName ) {
		super(documentName);
		g = new FiducialSquareHammingGenerator(config);
		g.setRenderer(render);
	}

	public FiducialSquareHammingGenerator getGenerator() {
		return g;
	}

	public void render( DogArray_I32 markerIDs ) {
		if (markerHeight > 0)
			throw new IllegalArgumentException("markerHeight must be < 0 since only square is supported");
		g.setMarkerWidth(markerWidth);
		render.configure(whiteBorderPixels, (int)g.getMarkerWidth());
		for (int i = 0; i < markerIDs.size; i++) {
			g.generate(markerIDs.get(i));
			save(render.getGray(), markerIDs.get(i)+"");
		}
	}

	public void setWhiteBorder( int pixels ) {
		whiteBorderPixels = pixels;
	}
}
