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

package boofcv.abst.feature.detect.extract;

import boofcv.struct.Configuration;

/**
 * General configuration for {@link NonMaxSuppression}.
 *
 * @author Peter Abeles
 */
public class ConfigExtract implements Configuration {
	/**
	 * Search radius of the non-maximum region.  Most common value is 1 for a 3x3 region. Default is 1.
	 */
	public int radius = 1;
	/**
	 * Minimum feature intensity it will consider when detecting a maximum. For local minimums
	 * it will use a value of -threshold. Defaults to 0.
	 */
	public float threshold = 0;
	/**
	 * Size of border around the image in which pixels are not considered. Default is 0.
	 */
	public int ignoreBorder = 0;
	/**
	 * Is a strict test used to test for local maximums.  If strict the local maximum must be greater than
	 * all its neighbors, otherwise it just needs to be greater than or equal to its neighbors. Default is true.
	 */
	public boolean useStrictRule = true;

	/**
	 * If false then local maximums will be found
	 */
	public boolean detectMinimums = false;

	/**
	 * If true then local maximums will be found
	 */
	public boolean detectMaximums = true;

	public ConfigExtract(int radius, float threshold, int ignoreBorder, boolean useStrictRule,
						 boolean detectMinimums, boolean detectMaximums)
	{
		this.radius = radius;
		this.threshold = threshold;
		this.ignoreBorder = ignoreBorder;
		this.useStrictRule = useStrictRule;
		this.detectMinimums = detectMinimums;
		this.detectMaximums = detectMaximums;
	}

	public ConfigExtract(int radius, float threshold, int ignoreBorder, boolean useStrictRule) {
		this.radius = radius;
		this.threshold = threshold;
		this.ignoreBorder = ignoreBorder;
		this.useStrictRule = useStrictRule;
	}

	/**
	 * Constructor which defaults to an ignore border of 0 and to using a strict rule
	 */
	public ConfigExtract(int radius, float threshold) {
		this(radius,threshold,0,true);
	}

	public ConfigExtract() {
	}

	public void setTo( ConfigExtract orig ) {
		this.radius = orig.radius;
		this.threshold = orig.threshold;
		this.ignoreBorder = orig.ignoreBorder;
		this.useStrictRule = orig.useStrictRule;
		this.detectMinimums = orig.detectMinimums;
		this.detectMaximums = orig.detectMaximums;
	}

	@Override
	public void checkValidity() {
		if( radius <= 0 )
			throw new IllegalArgumentException("Search radius must be >= 1");
		if( ignoreBorder < 0 )
			throw new IllegalArgumentException("Ignore border must be >= 0 ");
	}
}
