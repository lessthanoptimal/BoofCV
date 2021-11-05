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

package boofcv.factory.disparity;

import boofcv.alg.disparity.DisparityBlockMatch;
import boofcv.struct.Configuration;
import boofcv.struct.border.BorderType;

/**
 * Configuration for the basic block matching stereo algorithm that employs a greedy winner takes all strategy.
 *
 * @author Peter Abeles
 * @see DisparityBlockMatch
 */
public class ConfigDisparityBM implements Configuration {
	/** Minimum disparity that it will check. Must be &ge; 0 and &lt; maxDisparity */
	public int disparityMin = 0;
	/** Number of disparity values considered. Must be &gt; 0 */
	public int disparityRange = 100;
	/** Radius of the rectangular region along x-axis. */
	public int regionRadiusX = 3;
	/** Radius of the rectangular region along y-axis. */
	public int regionRadiusY = 3;
	/**
	 * Maximum allowed error in a region per pixel. Only used by "error" based measures, e.g. NCC does not
	 * use this value. Set to &lt; 0 to disable.
	 */
	public double maxPerPixelError = 0;
	/** Tolerance for how difference the left to right associated values can be. Try 1. Disable with -1 */
	public int validateRtoL = 1;
	/**
	 * Tolerance for how similar optimal region is to other region. Closer to zero is more tolerant.
	 * Try 0.15 unless NCC then 0.005. Disable with a value &le; 0
	 */
	public double texture = 0.15;
	/**
	 * If subpixel should be used to find disparity or not. If on then output disparity image needs to me GrayF32.
	 * If false then GrayU8.
	 */
	public boolean subpixel = true;
	/** How the error is computed for each block */
	public DisparityError errorType = DisparityError.CENSUS;
	/** Used if error type is Census */
	public ConfigDisparityError.Census configCensus = new ConfigDisparityError.Census();
	/** Used if error type is NCC */
	public ConfigDisparityError.NCC configNCC = new ConfigDisparityError.NCC();
	/**
	 * Specifies how the image border is handled. In general you want to avoid an approach which would bias the
	 * error to prefer a region with lots of pixels outside the image border.
	 */
	public BorderType border = BorderType.REFLECT;

	public ConfigDisparityBM setTo( ConfigDisparityBM src ) {
		this.disparityMin = src.disparityMin;
		this.disparityRange = src.disparityRange;
		this.regionRadiusX = src.regionRadiusX;
		this.regionRadiusY = src.regionRadiusY;
		this.maxPerPixelError = src.maxPerPixelError;
		this.validateRtoL = src.validateRtoL;
		this.texture = src.texture;
		this.subpixel = src.subpixel;
		this.errorType = src.errorType;
		this.configCensus.setTo(src.configCensus);
		this.configNCC.setTo(src.configNCC);
		this.border = src.border;
		return this;
	}

	@Override
	public void checkValidity() {
		if (disparityMin < 0)
			throw new IllegalArgumentException("miDisparity < 0");
		if (disparityRange < 1)
			throw new IllegalArgumentException("rangeDisparity < 1");
		if (border == BorderType.NORMALIZED || border == BorderType.SKIP)
			throw new IllegalArgumentException("Normalized and Skip are not supported");
	}
}
