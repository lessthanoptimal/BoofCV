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

package boofcv.factory.feature.describe;

import boofcv.abst.feature.describe.DescribePoint;
import boofcv.abst.feature.orientation.ConfigOrientation2;
import boofcv.struct.Configuration;

/**
 * Configuration for creating {@link DescribePoint}
 *
 * @author Peter Abeles
 */
public class ConfigDescribePoint implements Configuration {
	/** Specifies how the descriptors are computed */
	public final ConfigDescribeRegion descriptors = new ConfigDescribeRegion();

	/** Specifies how the orientation is found for orientation invariant descriptors */
	public final ConfigOrientation2 orientation = new ConfigOrientation2();

	/**
	 * If the descriptor is scale invariant you need to specify the region it will cover.
	 * If &le; 0 it will use the descriptor's default
	 */
	public double radius = -1;

	/** Convert the descriptor into a different format */
	public ConfigConvertTupleDesc convert = new ConfigConvertTupleDesc();

	@Override
	public void checkValidity() {
		descriptors.checkValidity();
		orientation.checkValidity();
		convert.checkValidity();
	}

	public ConfigDescribePoint setTo( ConfigDescribePoint src ) {
		this.radius = src.radius;
		this.descriptors.setTo(src.descriptors);
		this.orientation.setTo(orientation);
		this.convert.setTo(src.convert);
		return this;
	}
}
