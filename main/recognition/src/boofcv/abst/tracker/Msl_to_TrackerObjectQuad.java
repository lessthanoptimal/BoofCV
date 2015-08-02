/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.tracker;

import boofcv.alg.tracker.meanshift.PixelLikelihood;
import boofcv.alg.tracker.meanshift.TrackerMeanShiftLikelihood;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import georegression.struct.shapes.RectangleLength2D_I32;

/**
 * Wrapper around {@link  boofcv.alg.tracker.meanshift.TrackerMeanShiftLikelihood} for {@link TrackerObjectQuad}
 *
 * @author Peter Abeles
 */
public class Msl_to_TrackerObjectQuad <T extends ImageBase> implements TrackerObjectQuad<T> {

	TrackerMeanShiftLikelihood<T> tracker;
	PixelLikelihood<T> likelihood;

	ImageType<T> type;

	Rectangle2D_F64 rect = new Rectangle2D_F64();
	RectangleLength2D_I32 target = new RectangleLength2D_I32();

	public Msl_to_TrackerObjectQuad(TrackerMeanShiftLikelihood<T> tracker,
									PixelLikelihood<T> likelihood , ImageType<T> imageType) {
		this.tracker = tracker;
		this.likelihood = likelihood;

		type = imageType;
	}

	@Override
	public boolean initialize( T image, Quadrilateral_F64 location) {

		UtilPolygons2D_F64.bounding(location, rect);

		target.x0 = (int)rect.p0.x;
		target.y0 = (int)rect.p0.y;
		target.width = (int)rect.getWidth()+1;
		target.height = (int)rect.getHeight()+1;

		likelihood.setImage(image);
		likelihood.createModel(target);
		tracker.initialize(image,target);

		return true;
	}

	@Override
	public boolean process(T image, Quadrilateral_F64 location) {

		if( !tracker.process(image ))
		    return false;

		RectangleLength2D_I32 rect = tracker.getLocation();
		UtilPolygons2D_F64.convert(rect,location);

		return true;
	}

	@Override
	public ImageType<T> getImageType() {
		return type;
	}
}
