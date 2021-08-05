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

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckGenerator;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.app.fiducials.CreateFiducialDocumentImage;
import boofcv.misc.BoofMiscOps;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
public class CreateECoCheckDocumentImage extends CreateFiducialDocumentImage {

	FiducialImageEngine render = new FiducialImageEngine();


	public CreateECoCheckDocumentImage( String documentName) {
		super(documentName);
	}

	public void configure( int width, int height, int border ) {
		render.configure(border, width - 2*border, height - 2*border);
	}

	public void render( ECoCheckUtils utils ) {
		ECoCheckGenerator g = new ECoCheckGenerator(utils);
		g.setRender(render);
		int numMarkers = utils.markers.size();
		int numDigits = BoofMiscOps.numDigits(numMarkers);

		if (numMarkers > 1) {
			for (int i = 0; i < numMarkers; i++) {
				g.render(i);
				save(render.getGray(), String.format("%0" + numDigits + "d", i));
			}
		} else {
			// Don't append a number if only one image is created
			g.render(0);
			save(render.getGray(), "");
		}
	}
}
