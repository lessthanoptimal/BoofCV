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

package boofcv.struct;


import georegression.metric.Intersection2D_I32;
import georegression.struct.shapes.Rectangle2D_I32;

/**
 * @author Peter Abeles
 */
public class ImageRectangle extends Rectangle2D_I32 {

	public ImageRectangle(int x0, int y0, int x1, int y1) {
		set(x0,y0,x1,y1);
	}

	public ImageRectangle( ImageRectangle orig ) {
		set(orig);
	}

	public ImageRectangle() {
	}

	public boolean intersection( ImageRectangle b , ImageRectangle result ) {
		return Intersection2D_I32.intersection(this,b,result);
	}
}
