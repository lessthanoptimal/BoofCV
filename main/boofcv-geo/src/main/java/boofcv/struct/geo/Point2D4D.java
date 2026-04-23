/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.geo;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ejml.MapFormattable;
import org.ejml.MapPrintFormat;

/// Observed point feature location on the image plane and its 3D homogeneous position.
public class Point2D4D implements MapFormattable {
	/// Observed location of the feature on the image plane
	@Getter @Setter public Point2D_F64 observation;
	/// 3D location of the feature in homogeneous world coordinates
	@Getter @Setter public Point4D_F64 location;

	public Point2D4D() {
		observation = new Point2D_F64();
		location = new Point4D_F64();
	}

	public Point2D4D( Point2D_F64 observation, Point4D_F64 location ) {
		this.observation = observation;
		this.location = location;
	}

	/// Sets 'this' to be identical to 'src'.
	public Point2D4D setTo( Point2D4D src ) {
		observation.setTo(src.observation);
		location.setTo(src.location);
		return this;
	}

	public void zero() {
		observation.zero();
		location.zero();
	}

	public Point2D4D copy() {
		return new Point2D4D(observation.copy(), location.copy());
	}

	@Override public String formatMap( MapPrintFormat format ) {
		return format.itemPrefix +
				format.pair("observation", observation.formatMap(format), true) +
				format.pair("location", location.formatMap(format), true) +
				format.itemSuffix;
	}

	@Override public String toString() {return MapPrintFormat.DEFAULT.toString(this);}
}
