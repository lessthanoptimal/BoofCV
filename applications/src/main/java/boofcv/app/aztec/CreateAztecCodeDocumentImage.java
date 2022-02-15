/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.app.aztec;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.alg.fiducial.aztec.AztecGenerator;
import boofcv.app.fiducials.CreateFiducialDocumentImage;

/**
 * Saves Micro QR Code to disk as an image file
 *
 * @author Peter Abeles
 */
public class CreateAztecCodeDocumentImage extends CreateFiducialDocumentImage {
	AztecGenerator g;
	FiducialImageEngine render = new FiducialImageEngine();
	int squareWidthPixels;

	public CreateAztecCodeDocumentImage( String documentName, int squareWidthPixels ) {
		super(documentName);
		this.squareWidthPixels = squareWidthPixels;

		g = new AztecGenerator();
		g.setRender(render);
	}

	public void render( java.util.List<AztecCode> markers ) {
		for (int i = 0; i < markers.size(); i++) {
			int pixels = squareWidthPixels*markers.get(i).getMarkerWidthSquares();
			render.configure(0, pixels);
			g.markerWidth = pixels;
			g.render(markers.get(i));
			save(render.getGray(), "" + i);
		}
	}
}
