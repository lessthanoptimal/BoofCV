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

package boofcv.abst.disparity;

import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Configuration for {@link DisparitySmootherSpeckleFilter}.
 *
 * @author Peter Abeles
 */
public class ConfigSpeckleFilter implements Configuration {
	/** How similar two pixel values need to be for them to be considered connected */
	public float similarTol = 1.0f;

	/** The maximum area (in pixels) for a region to be filtered. If relative, then it's relative to width*height */
	public final ConfigLength maximumArea = ConfigLength.relative(0.002, 0.0);

	@Override public void checkValidity() {
		checkTrue(similarTol >= 0.0f);
		maximumArea.checkValidity();
	}

	public ConfigSpeckleFilter setTo( ConfigSpeckleFilter src ) {
		this.similarTol = src.similarTol;
		this.maximumArea.setTo(src.maximumArea);
		return this;
	}
}
