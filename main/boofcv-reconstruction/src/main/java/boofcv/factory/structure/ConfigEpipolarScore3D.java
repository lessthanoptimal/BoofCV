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

import boofcv.alg.structure.score3d.ScoreFundamentalHomographyCompatibility;
import boofcv.alg.structure.score3d.ScoreFundamentalVsRotation;
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
	public Type type = Type.FUNDAMENTAL_ROTATION;

	/** Configuration used if {@link Type#MODEL_INLIERS} is selected */
	public final ModelInliers typeInliers = new ModelInliers();

	/** Configuration used if {@link Type#FUNDAMENTAL_COMPATIBLE} is selected */
	public final FundamentalCompatible typeCompatible = new FundamentalCompatible();

	/** Configuration used if {@link Type#FUNDAMENTAL_ROTATION} is selected */
	public final FundamentalRotation typeRotation = new FundamentalRotation();

	{
		ransacF.iterations = 500;
		ransacF.inlierThreshold = 2.0;

		// F computes epipolar error, which isn't as strict as reprojection error for H, so give H a larger error tol
		typeInliers.ransacH.iterations = 500;
		typeInliers.ransacH.inlierThreshold = 4.0;

		fundamental.errorModel = ConfigFundamental.ErrorModel.GEOMETRIC;
		fundamental.numResolve = 1;
	}

	@Override public void checkValidity() {
		ransacF.checkValidity();
		fundamental.checkValidity();
		typeInliers.checkValidity();
		typeCompatible.checkValidity();
	}

	public ConfigEpipolarScore3D setTo( ConfigEpipolarScore3D src ) {
		this.ransacF.setTo(src.ransacF);
		this.fundamental.setTo(src.fundamental);
		this.type = src.type;
		this.typeInliers.setTo(src.typeInliers);
		this.typeCompatible.setTo(src.typeCompatible);
		this.typeRotation.setTo(src.typeRotation);
		return this;
	}

	/**
	 * Specifies which algorithm to use
	 */
	public enum Type {
		MODEL_INLIERS,
		FUNDAMENTAL_COMPATIBLE,
		FUNDAMENTAL_ROTATION
	}

	/**
	 * Configuration for {@link boofcv.alg.structure.score3d.ScoreRatioFundamentalHomography}
	 */
	public static class ModelInliers implements Configuration {
		/** RANSAC for fundamental Homography */
		public final ConfigRansac ransacH = new ConfigRansac();

		/** Configuration for computing Homography matrix */
		public final ConfigHomography homography = new ConfigHomography();

		/** The minimum number of inliers for an edge to be accepted. If relative, then relative to pairs. */
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

		public ModelInliers setTo( ModelInliers src ) {
			this.ratio3D = src.ratio3D;
			this.maxRatioScore = src.maxRatioScore;
			this.ransacH.setTo(src.ransacH);
			this.homography.setTo(src.homography);
			this.minimumInliers.setTo(src.minimumInliers);
			return this;
		}
	}

	/**
	 * Configuration for {@link ScoreFundamentalHomographyCompatibility}
	 */
	public static class FundamentalCompatible implements Configuration {
		/** {@link ScoreFundamentalHomographyCompatibility#ratio3D} */
		public double ratio3D = 1.2;

		/** {@link ScoreFundamentalHomographyCompatibility#inlierErrorTol} */
		public double inlierErrorTol = 2.0;

		/** {@link ScoreFundamentalHomographyCompatibility#maxRatioScore} */
		public double maxRatioScore = 10.0;

		/** The minimum number of inliers for an edge to be accepted. If relative, then relative to pairs. */
		public final ConfigLength minimumInliers = ConfigLength.relative(0.2, 40);

		@Override public void checkValidity() {
			BoofMiscOps.checkTrue(inlierErrorTol >= 0);
			BoofMiscOps.checkTrue(ratio3D > 0.0);
			BoofMiscOps.checkTrue(maxRatioScore > 0.0);
			minimumInliers.checkValidity();
		}

		public FundamentalCompatible setTo( FundamentalCompatible src ) {
			this.ratio3D = src.ratio3D;
			this.inlierErrorTol = src.inlierErrorTol;
			this.maxRatioScore = src.maxRatioScore;
			this.minimumInliers.setTo(src.minimumInliers);
			return this;
		}
	}

	/**
	 * Configuration for {@link ScoreFundamentalVsRotation}
	 */
	public static class FundamentalRotation implements Configuration {
		/** {@link ScoreFundamentalVsRotation#ratio3D} */
		public double ratio3D = 1.2;

		/** {@link ScoreFundamentalVsRotation#inlierErrorTol} */
		public double inlierErrorTol = 1.5;

		/** {@link ScoreFundamentalVsRotation#maxRatioScore} */
		public double maxRatioScore = 10.0;

		/** The minimum number of inliers for an edge to be accepted. If relative, then relative to pairs. */
		public final ConfigLength minimumInliers = ConfigLength.relative(0.2, 40);

		@Override public void checkValidity() {
			BoofMiscOps.checkTrue(inlierErrorTol >= 0);
			BoofMiscOps.checkTrue(ratio3D > 0.0);
			BoofMiscOps.checkTrue(maxRatioScore > 0.0);
			minimumInliers.checkValidity();
		}

		public FundamentalRotation setTo( FundamentalRotation src ) {
			this.ratio3D = src.ratio3D;
			this.inlierErrorTol = src.inlierErrorTol;
			this.maxRatioScore = src.maxRatioScore;
			this.minimumInliers.setTo(src.minimumInliers);
			return this;
		}
	}
}
