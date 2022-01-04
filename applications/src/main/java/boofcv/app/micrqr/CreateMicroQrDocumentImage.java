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

package boofcv.app.micrqr;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.microqr.MicroQrCodeGenerator;
import boofcv.app.fiducials.CreateFiducialDocumentImage;

/**
 * Saves Micro QR Code to disk as an image file
 *
 * @author Peter Abeles
 */
public class CreateMicroQrDocumentImage extends CreateFiducialDocumentImage {
	MicroQrCodeGenerator g;
	FiducialImageEngine render = new FiducialImageEngine();
	int moduleWidthPixels;

	public CreateMicroQrDocumentImage( String documentName, int moduleWidthPixels ) {
		super(documentName);
		this.moduleWidthPixels = moduleWidthPixels;

		g = new MicroQrCodeGenerator();
		g.setRender(render);
	}

	public void render( java.util.List<MicroQrCode> markers ) {
		for (int i = 0; i < markers.size(); i++) {
			int pixels = moduleWidthPixels*markers.get(i).getNumberOfModules();
			render.configure(moduleWidthPixels, pixels);
			g.markerWidth = pixels;
			g.render(markers.get(i));
			save(render.getGray(), "" + i);
		}
	}
}
