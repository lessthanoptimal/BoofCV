/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_F32;
import georegression.struct.shapes.RectangleCorner2D_F64;

/**
 * Wrapper around {@link CirculantTracker} for {@link TrackerObjectQuad}.
 *
 * @author Peter Abeles
 */
public class Circulant_to_TrackerObjectQuad<T extends ImageSingleBand> implements TrackerObjectQuad<T> {

	CirculantTracker<T> tracker;
	RectangleCorner2D_F64 rect = new RectangleCorner2D_F64();

	ImageType<T> imageType;

	public Circulant_to_TrackerObjectQuad(CirculantTracker<T> tracker , ImageType<T> imageType) {
		this.tracker = tracker;
		this.imageType = imageType;

	}

	@Override
	public boolean initialize(T image, Quadrilateral_F64 location) {


		UtilPolygons2D_F64.bounding(location, rect);

		int width = (int)(rect.x1 - rect.x0);
		int height = (int)(rect.y1 - rect.y0);

		tracker.initialize(image,(int)rect.x0,(int)rect.y0,width,height);

		return true;
	}

	@Override
	public boolean process(T image, Quadrilateral_F64 location) {

		tracker.performTracking(image);
		Rectangle2D_F32 r = tracker.getTargetLocation();

		if( r.tl_x >= image.width || r.tl_y >= image.height )
			return false;
		if( r.tl_x+r.width < 0 || r.tl_y+r.height < 0 )
			return false;

		float x0 = r.tl_x;
		float y0 = r.tl_y;
		float x1 = r.tl_x + r.width;
		float y1 = r.tl_y + r.height;

		location.a.x = x0;
		location.a.y = y0;
		location.b.x = x1;
		location.b.y = y0;
		location.c.x = x1;
		location.c.y = y1;
		location.d.x = x0;
		location.d.y = y1;

		return true;
	}

	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}
}
