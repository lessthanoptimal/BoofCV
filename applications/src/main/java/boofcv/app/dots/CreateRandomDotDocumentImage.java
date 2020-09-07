/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.dots.RandomDotMarkerGeneratorImage;
import boofcv.app.fiducials.CreateFiducialDocumentImage;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;

import java.util.List;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
public class CreateRandomDotDocumentImage extends CreateFiducialDocumentImage {

	@Getter RandomDotMarkerGeneratorImage g = new RandomDotMarkerGeneratorImage();

	public double dotDiameter;

	public CreateRandomDotDocumentImage( String documentName ) {
		super(documentName);
	}

	public void render( List<List<Point2D_F64>> markers ) {
		int numDigits = BoofMiscOps.numDigits(markers.size());
		int markerHeight = this.markerHeight > 0 ? this.markerHeight : this.markerWidth;
		g.configure(markerWidth, markerHeight, (int)dotDiameter);
		g.setRadius(dotDiameter/2.0);
		if (markers.size() > 1) {
			for (int i = 0; i < markers.size(); i++) {
				g.render(markers.get(i), markerWidth, markerHeight);
				save(g.getImage(), String.format("%0" + numDigits + "d", i));
			}
		} else if (markers.size() == 1) {
			// Don't append a number if only one image is created
			g.render(markers.get(0), markerWidth, markerHeight);
			save(g.getImage(), "");
		}
	}
}
