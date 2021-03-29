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

package boofcv.factory.structure;

import boofcv.factory.geo.ConfigFundamental;
import boofcv.factory.geo.ConfigHomography;
import boofcv.factory.geo.ConfigRansac;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Configuration for implementations of {@link boofcv.alg.structure.EpipolarScore3D}.
 *
 * @author Peter Abeles
 */
public class ConfigEpipolarScore3D implements Configuration {

	/** RANSAC for fundamental matrix */
	public final ConfigRansac ransacF = new ConfigRansac();

	/** Configuration for computing fundamental matrix */
	public final ConfigFundamental fundamental = new ConfigFundamental();

	/** Which algorithm it should use */
	public Type type = Type.MODEL_INLIERS;

	/** Configuration used if {@link Type#MODEL_INLIERS} is selected */
	public final ModelInliers typeInliers = new ModelInliers();

	/** Configuration used if {@link Type#FUNDAMENTAL_ERROR} is selected */
	public final FundamentalError typeErrors = new FundamentalError();

	{
		ransacF.iterations = 500;
		ransacF.inlierThreshold = 1;

		// F computes epipolar error, which isn't as strict as reprojection error for H, so give H a larger error tol
		typeInliers.ransacH.iterations = 500;
		typeInliers.ransacH.inlierThreshold = 2.0;

		fundamental.errorModel = ConfigFundamental.ErrorModel.GEOMETRIC;
		fundamental.numResolve = 1;
	}

	@Override public void checkValidity() {
		ransacF.checkValidity();
		fundamental.checkValidity();
		typeInliers.checkValidity();
		typeErrors.checkValidity();
	}

	public void setTo( ConfigEpipolarScore3D src ) {
		this.ransacF.setTo(src.ransacF);
		this.fundamental.setTo(src.fundamental);
		this.type = src.type;
		this.typeInliers.setTo(src.typeInliers);
		this.typeErrors.setTo(typeErrors);
	}

	/**
	 * Specifies which algorithm to use
	 */
	public enum Type {
		MODEL_INLIERS,
		FUNDAMENTAL_ERROR
	}

	/**
	 * Configuration for {@link boofcv.alg.structure.score3d.ScoreRatioFundamentalHomography}
	 */
	public static class ModelInliers implements Configuration {
		/** RANSAC for fundamental Homography */
		public final ConfigRansac ransacH = new ConfigRansac();

		/** Configuration for computing Homography matrix */
		public final ConfigHomography homography = new ConfigHomography();

		/** The minimum number of inliers for an edge to be accepted. If relative, then relative to pairs.  */
		public final ConfigLength minimumInliers = ConfigLength.fixed(30);

		/**
		 * If number of matches from fundamental divided by homography is more than this then it is
		 * considered a 3D scene
		 */
		public double ratio3D = 1.5;

		/**
		 * Caps how much influence the geometric score can have. The error ratio can sky rocket as the baseline
		 * increased but the benefit doesn't seem to increase after a point.
		 */
		public double maxRatioScore = 5.0;

		@Override public void checkValidity() {
			BoofMiscOps.checkTrue(ratio3D > 0.0);
			BoofMiscOps.checkTrue(maxRatioScore > 0.0);
			ransacH.checkValidity();
			homography.checkValidity();
			minimumInliers.checkValidity();
		}

		public void setTo( ModelInliers src ) {
			this.ratio3D = src.ratio3D;
			this.maxRatioScore = src.maxRatioScore;
			this.ransacH.setTo(src.ransacH);
			this.homography.setTo(src.homography);
			this.minimumInliers.setTo(src.minimumInliers);
		}
	}

	/**
	 * Configuration for {@link boofcv.alg.structure.score3d.ScoreFundamentalReprojectionError}
	 */
	public static class FundamentalError implements Configuration {
		/** Higher values indicate more evidence is needed for a scene to be 3D */
		public double ratio3D = 2.0;

		/** Smoothing parameter and avoid divide by zero. This is typically < 1.0 since error is computed in pixels */
		public double eps = 0.01;

		/**
		 * Caps how much influence the geometric score can have. The error ratio can sky rocket as the baseline
		 * increased but the benefit doesn't seem to increase after a point.
		 */
		public double maxRatioScore = 5.0;

		/** The minimum number of inliers for an edge to be accepted. If relative, then relative to pairs.  */
		public final ConfigLength minimumInliers = ConfigLength.fixed(30);

		@Override public void checkValidity() {
			BoofMiscOps.checkTrue(eps >= 0);
			BoofMiscOps.checkTrue(ratio3D > 0.0);
			BoofMiscOps.checkTrue(maxRatioScore > 0.0);
			minimumInliers.checkValidity();
		}

		public void setTo( FundamentalError src ) {
			this.ratio3D = src.ratio3D;
			this.eps = src.eps;
			this.maxRatioScore = src.maxRatioScore;
			this.minimumInliers.setTo(src.minimumInliers);
		}
	}
}
