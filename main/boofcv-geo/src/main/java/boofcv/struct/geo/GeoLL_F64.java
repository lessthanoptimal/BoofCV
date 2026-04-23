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

import lombok.Getter;
import lombok.Setter;
import org.ejml.MapFormattable;
import org.ejml.MapPrintFormat;

/// Geographic coordinate consisting of latitude (north-south coordinate) and longitude (west-east) .
/// <center>
/// <img src="doc-files/sphere_lat_lon.png">
/// </center>
public class GeoLL_F64 implements MapFormattable {
	/// latitude
	@Getter @Setter public double lat;
	/// longitude
	@Getter @Setter public double lon;

	public GeoLL_F64( double lat, double lon ) {
		this.lat = lat;
		this.lon = lon;
	}

	public GeoLL_F64() {}

	public GeoLL_F64 setTo( GeoLL_F64 src ) {
		this.lat = src.lat;
		this.lon = src.lon;
		return this;
	}

	public GeoLL_F64 setTo( double lat, double lon ) {
		this.lat = lat;
		this.lon = lon;
		return this;
	}

	@Override public String formatMap( MapPrintFormat format ) {
		var builder = new StringBuilder();
		builder.append(format.itemPrefix);
		format.pair(builder, "lat", lat, true);
		format.pair(builder, "lon", lon, false);
		builder.append(format.itemSuffix);
		return builder.toString();
	}

	@Override public String toString() {return MapPrintFormat.DEFAULT.toString(this);}
}
