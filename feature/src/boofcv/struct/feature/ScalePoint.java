/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.struct.feature;

import georegression.struct.point.Point2D_I32;

/**
 * Where a point of interest was detected and at what scale.
 *
 * @author Peter Abeles
 */
public class ScalePoint extends Point2D_I32 {
	// the scale the feature was detected at
	public double scale;

	public ScalePoint(int x, int y, double scale) {
		super(x, y);
		this.scale = scale;
	}

	public ScalePoint() {
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public ScalePoint copy() {
		return new ScalePoint(x,y,scale);
	}

	public void set(ScalePoint p) {
		this.scale = p.scale;
		this.x = p.x;
		this.y = p.y;
	}
}
