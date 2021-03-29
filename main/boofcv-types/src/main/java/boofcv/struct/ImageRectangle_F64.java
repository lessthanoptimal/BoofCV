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

package boofcv.struct;

/**
 * Specifies an axis aligned rectangle inside an image using lower and upper extents.
 *
 * @author Peter Abeles
 */
public class ImageRectangle_F64 {
	/** Inclusive lower extent (x0,y) and exclusive upper extent (x1,y1) */
	public double x0, y0, x1, y1;

	public ImageRectangle_F64( double x0, double y0, double x1, double y1 ) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}

	public ImageRectangle_F64( ImageRectangle_F64 orig ) {
		this.x0 = orig.x0;
		this.y0 = orig.y0;
		this.x1 = orig.x1;
		this.y1 = orig.y1;
	}

	public ImageRectangle_F64() {}

	public void setTo( ImageRectangle_F64 orig ) {
		this.x0 = orig.x0;
		this.y0 = orig.y0;
		this.x1 = orig.x1;
		this.y1 = orig.y1;
	}

	public void setTo( double x0, double y0, double x1, double y1 ) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}

	/**
	 * Returns the area of the rectangle
	 */
	public double area() {
		return (y1 - y0)*(x1 - x0);
	}
}
