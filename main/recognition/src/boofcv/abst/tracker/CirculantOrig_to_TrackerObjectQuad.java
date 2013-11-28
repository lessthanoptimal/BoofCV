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

import boofcv.alg.tracker.circulant.CirculantTrackerOrig;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import georegression.struct.shapes.RectangleCorner2D_F64;

/**
 * Wrapper around {@link boofcv.alg.tracker.circulant.CirculantTrackerOrig} for {@link TrackerObjectQuad}.
 *
 * @author Peter Abeles
 */
public class CirculantOrig_to_TrackerObjectQuad<T extends ImageSingleBand> implements TrackerObjectQuad<T> {

	CirculantTrackerOrig tracker;
	RectangleCorner2D_F64 rect = new RectangleCorner2D_F64();

	ImageType<T> imageType;
	ImageFloat32 tmp;

	public CirculantOrig_to_TrackerObjectQuad(CirculantTrackerOrig tracker, ImageType<T> imageType) {
		this.tracker = tracker;
		this.imageType = imageType;

		if( imageType.getDataType() != ImageDataType.F32 ) {
			tmp = new ImageFloat32(1,1);
		}
	}

	@Override
	public boolean initialize(T image, Quadrilateral_F64 location) {

		if( imageType.getDataType() != ImageDataType.F32 ) {
			tmp.reshape(image.width,image.height);
			GConvertImage.convert(image,tmp);
		} else {
			tmp = (ImageFloat32)image;
		}

		UtilPolygons2D_F64.bounding(location, rect);

		int width = (int)(rect.x1 - rect.x0);
		int height = (int)(rect.y1 - rect.y0);

		tracker.initialize(tmp,(int)rect.x0,(int)rect.y0,width,height);

		return true;
	}

	@Override
	public boolean process(T image, Quadrilateral_F64 location) {

		if( imageType.getDataType() != ImageDataType.F32 ) {
			GConvertImage.convert(image,tmp);
		} else {
			tmp = (ImageFloat32)image;
		}

		tracker.performTracking(tmp);
		Rectangle2D_I32 r = tracker.getTargetLocation();

		int x0 = r.tl_x;
		int y0 = r.tl_y;
		int x1 = r.tl_x + r.width;
		int y1 = r.tl_y + r.height;

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
