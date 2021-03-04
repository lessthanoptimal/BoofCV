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
import boofcv.alg.fiducial.square.FiducialSquareGenerator;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.DogArray_I64;

import java.util.List;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
public class CreateSquareFiducialDocumentImage extends CreateFiducialDocumentImage {

	FiducialSquareGenerator generator;
	FiducialImageEngine render = new FiducialImageEngine();
	int whiteBorderPixels;

	double blackBorderFractionalWidth;

	public CreateSquareFiducialDocumentImage( String documentName ) {
		super(documentName);
		this.generator = new FiducialSquareGenerator(render);
	}

	public FiducialSquareGenerator getGenerator() {
		return generator;
	}

	public void render( java.util.List<String> names, DogArray_I64 patterns, int gridWidth ) {
		if (markerHeight > 0)
			throw new IllegalArgumentException("markerHeight must be < 0 since only square is supported");
		generator.setMarkerWidth(markerWidth);
		generator.setBlackBorder(blackBorderFractionalWidth);
		render.configure(whiteBorderPixels, (int)generator.getMarkerWidth());
		for (int i = 0; i < patterns.size; i++) {
			generator.generate(patterns.get(i), gridWidth);
			save(render.getGray(), names.get(i));
		}
	}

	public void render( java.util.List<String> names, List<GrayU8> patterns ) {
		if (markerHeight > 0)
			throw new IllegalArgumentException("markerHeight must be < 0 since only square is supported");
		generator.setMarkerWidth(markerWidth);
		render.configure(whiteBorderPixels, (int)generator.getMarkerWidth());
		for (int i = 0; i < patterns.size(); i++) {
			generator.generate(patterns.get(i));
			save(render.getGray(), names.get(i));
		}
	}

	public void setWhiteBorder( int pixels ) {
		whiteBorderPixels = pixels;
	}

	public void setBlackBorderFractionalWidth( double blackBorderFractionalWidth ) {
		this.blackBorderFractionalWidth = blackBorderFractionalWidth;
	}
}
