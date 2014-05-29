/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import boofcv.alg.filter.binary.Contour;
import boofcv.struct.image.ImageSInt32;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage for results from blob finding in a binary image
 *
 * @author Peter Abeles
 */
public class ResultsBlob {

	public List<Contour> contour;
	public ImageSInt32 labeled;

	public ResultsBlob(List<Contour> contour, ImageSInt32 labeled) {
		this.contour = contour;
		this.labeled = labeled;
	}

	public SimpleLabeledImage getLabeledImage() {
		return new SimpleLabeledImage(labeled);
	}

	public SimpleContourList getContours() {
		List<SimpleContour> contours = new ArrayList<SimpleContour>();

		for( Contour c : this.contour ) {
			contours.add( new SimpleContour(c));
		}

		return new SimpleContourList(contours,labeled.width,labeled.height);
	}

}
