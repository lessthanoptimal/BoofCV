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

import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.alg.disparity.sgm.SgmStereoDisparityHmi;
import boofcv.struct.Configuration;
import boofcv.struct.border.BorderType;

import static boofcv.alg.disparity.sgm.SgmDisparityCost.MAX_COST;

/**
 * Configuration for {@link SgmStereoDisparityHmi Semi Global Matching}
 *
 * @author Peter Abeles
 */
public class ConfigDisparitySGM implements Configuration {
	/** Minimum disparity that it will check. Must be &ge; 0 and &lt; maxDisparity */
	public int disparityMin = 0;
	/** Number of disparity values considered. Must be &gt; 0. Maximum number is 255 for 8-bit disparity images. */
	public int disparityRange = 100;
	/**
	 * Maximum allowed error for a single pixel. Set to a value less than 0 to disable. Has a range from
	 * 0 to {@link SgmDisparityCost#MAX_COST}
	 */
	public int maxError = -1;
	/** Tolerance for how difference the left to right associated values can be. Try 1. Disable with -1 */
	public int validateRtoL = 1;
	/**
	 * Tolerance for how similar optimal region is to other region. Closer to zero is more tolerant.
	 * Try 0.1 for SAD or 0.7 for NCC. Disable with a value &le; 0
	 */
	public double texture = 0.15;
	/**
	 * If subpixel should be used to find disparity or not. If on then output disparity image needs to me GrayF32.
	 * If false then GrayU8.
	 */
	public boolean subpixel = true;
	/**
	 * The penalty applied to a small change in disparity. 0 &le; x &le; {@link SgmDisparityCost#MAX_COST} and
	 * must be less than {@link #penaltyLargeChange}.
	 */
	public int penaltySmallChange = 200;
	/** The penalty applied to a large change in disparity. 0 &le; x &le; {@link SgmDisparityCost#MAX_COST} */
	public int penaltyLargeChange = 2000;
	/** Number of paths it should consider. 4 or 8 is most common. More paths slower it will run. */
	public Paths paths = Paths.P8;
	/** Which error model should it use */
	public DisparitySgmError errorType = DisparitySgmError.CENSUS;
	/** If true it will use block variants of errors. */
	public boolean useBlocks = true;
	/** Used if error type is Census */
	public ConfigDisparityError.Census configCensus = new ConfigDisparityError.Census();
	/** Used if error type is HMI */
	public ConfigDisparityError.HMI configHMI = new ConfigDisparityError.HMI();
	/** The error can be computed using code taken from block matching */
	public ConfigBlockMatchError configBlockMatch = new ConfigBlockMatchError();
	/**
	 * Specifies how the image border is handled. In general you want to avoid an approach which would bias the
	 * error to prefer a region with lots of pixels outside the image border.
	 */
	public BorderType border = BorderType.REFLECT;

	public ConfigDisparitySGM setTo( ConfigDisparitySGM src ) {
		this.disparityMin = src.disparityMin;
		this.disparityRange = src.disparityRange;
		this.maxError = src.maxError;
		this.validateRtoL = src.validateRtoL;
		this.texture = src.texture;
		this.subpixel = src.subpixel;
		this.penaltySmallChange = src.penaltySmallChange;
		this.penaltyLargeChange = src.penaltyLargeChange;
		this.paths = src.paths;
		this.errorType = src.errorType;
		this.useBlocks = src.useBlocks;
		this.configCensus.setTo(src.configCensus);
		this.configHMI.setTo(src.configHMI);
		this.configBlockMatch.setTo(src.configBlockMatch);
		this.border = src.border;
		return this;
	}

	public static class ConfigBlockMatchError implements Configuration {
		/**
		 * Block radius X
		 */
		public int radiusX = 2;
		/**
		 * Block radius Y
		 */
		public int radiusY = 2;

		public BlockMatchingApproach approach = BlockMatchingApproach.BEST5;

		public ConfigBlockMatchError setTo( ConfigBlockMatchError src ) {
			this.radiusX = src.radiusX;
			this.radiusY = src.radiusY;
			return this;
		}

		@Override
		public void checkValidity() {
			if (radiusX <= 0 || radiusY <= 0)
				throw new IllegalArgumentException("Radius must be positive");
		}
	}

	@Override
	public void checkValidity() {
		if (penaltyLargeChange <= penaltySmallChange)
			throw new IllegalArgumentException("large penalty must be larger than small");
		if (penaltySmallChange < 0 || penaltySmallChange > MAX_COST)
			throw new IllegalArgumentException("Invalid value for penaltySmallChange.");
		if (penaltyLargeChange < 0 || penaltyLargeChange > MAX_COST)
			throw new IllegalArgumentException("Invalid value for penaltySmallChange.");
		if (disparityMin < 0)
			throw new IllegalArgumentException("Minimum disparity must be >= 0");
		configBlockMatch.checkValidity();
	}

	/**
	 * Allowed number of paths
	 */
	public enum Paths {
		P1(1), P2(2), P4(4), P8(8), P16(16);
		private final int count;

		Paths( int count ) {
			this.count = count;
		}

		public int getCount() {
			return count;
		}
	}
}
