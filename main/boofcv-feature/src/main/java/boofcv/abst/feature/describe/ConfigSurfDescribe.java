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

package boofcv.abst.feature.describe;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Abstract base class for SURF implementations. Use child classes to specify a specific implementation.
 *
 * @author Peter Abeles
 * @see boofcv.alg.feature.describe.DescribePointSurf
 */
public abstract class ConfigSurfDescribe implements Configuration {
	/** Number of sub-regions wide the large grid is. Typically 4 */
	public int widthLargeGrid = 4;

	/** Number of sample points wide a sub-region is. Typically 5 */
	public int widthSubRegion = 5;

	/** The width of a sample point in pixels. Used to scale the descriptor sample region's width. Typically 3 */
	public double widthSample = 3;

	/** If true the Haar wavelet will be used. If false means image gradient. */
	public boolean useHaar = false;

	/**
	 * Configuration for SURF implementation that has been designed for speed at the cost of some
	 * stability.
	 *
	 * @see boofcv.alg.feature.describe.DescribePointSurf
	 */
	public static class Fast extends ConfigSurfDescribe {
		/** Weighting factor's sigma. Try 4.5 */
		public double weightSigma = 4.5;

		@Override public void checkValidity() {
			super.checkValidity();
			BoofMiscOps.checkTrue(weightSigma > 0);
		}

		public Fast setTo( Fast src ) {
			super.setTo(src);
			this.weightSigma = src.weightSigma;
			return this;
		}
	}

	/**
	 * Configuration for SURF implementation that has been designed for stability.
	 *
	 * @see boofcv.alg.feature.describe.DescribePointSurfMod
	 */
	public static class Stability extends ConfigSurfDescribe {
		/** Number of sample points sub-regions overlap, Typically 2. */
		public int overLap = 2;

		/** Sigma used to weight points in the sub-region grid. Typically 2.5 */
		public double sigmaLargeGrid = 2.5;

		/** Sigma used to weight points in the large grid. Typically 2.5 */
		public double sigmaSubRegion = 2.5;

		@Override public void checkValidity() {
			super.checkValidity();
			BoofMiscOps.checkTrue(sigmaLargeGrid > 0);
			BoofMiscOps.checkTrue(sigmaSubRegion > 0);
		}

		public Stability setTo( Stability src ) {
			super.setTo(src);
			this.overLap = src.overLap;
			this.sigmaLargeGrid = src.sigmaLargeGrid;
			this.sigmaSubRegion = src.sigmaSubRegion;
			return this;
		}
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(widthLargeGrid > 0);
		BoofMiscOps.checkTrue(widthSubRegion > 0);
		BoofMiscOps.checkTrue(widthSample > 0);
	}

	public ConfigSurfDescribe setTo( ConfigSurfDescribe src ) {
		this.widthLargeGrid = src.widthLargeGrid;
		this.widthSubRegion = src.widthSubRegion;
		this.widthSample = src.widthSample;
		this.useHaar = src.useHaar;
		return this;
	}
}
