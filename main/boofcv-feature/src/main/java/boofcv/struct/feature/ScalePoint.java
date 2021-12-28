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

package boofcv.struct.feature;

import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;

/**
 * Where a point of interest was detected and at what scale.
 *
 * @author Peter Abeles
 */
public class ScalePoint {
	/** location of interest point */
	public final Point2D_F64 pixel = new Point2D_F64();
	/** The scale the feature was detected at. Exact meaning of "scale" is implementation dependent */
	public @Getter @Setter double scale;
	/** does the blob correspond to a black or white region */
	public @Getter @Setter boolean white;
	/** Feature intensity where it was selected */
	public @Getter @Setter float intensity = Float.NaN;

	public ScalePoint( double x, double y, double scale ) {
		this.pixel.setTo(x, y);
		this.scale = scale;
	}

	public ScalePoint( double x, double y, double scale, boolean white ) {
		setTo(x, y, scale, white);
	}

	public ScalePoint() {}

	public void setTo( double x, double y, double scale ) {
		this.pixel.setTo(x, y);
		this.scale = scale;
		this.intensity = Float.NaN;
	}

	public void setTo( double x, double y, double scale, boolean white ) {
		this.pixel.setTo(x, y);
		this.scale = scale;
		this.white = white;
		this.intensity = Float.NaN;
	}

	public void setTo( double x, double y, double scale, boolean white, float intensity ) {
		this.pixel.setTo(x, y);
		this.scale = scale;
		this.white = white;
		this.intensity = intensity;
	}

	public ScalePoint copy() {
		var ret = new ScalePoint();
		ret.setTo(this);
		return ret;
	}

	public void setTo( ScalePoint p ) {
		this.scale = p.scale;
		this.pixel.setTo(p.pixel);
		this.white = p.white;
		this.intensity = p.intensity;
	}

	@Override
	public String toString() {
		return "ScalePoint{" +
				"pixel=" + pixel +
				", scale=" + scale +
				", white=" + white +
				", intensity=" + intensity +
				'}';
	}
}
