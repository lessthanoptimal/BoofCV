/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.sfm;

import boofcv.factory.geo.ConfigFundamental;
import boofcv.factory.geo.ConfigHomography;
import boofcv.factory.geo.ConfigRansac;
import boofcv.struct.Configuration;

/**
 * Cofiguration for {@link boofcv.alg.sfm.structure.GeneratePairwiseImageGraph}.
 *
 * @author Peter Abeles
 */
public class ConfigGeneratePairwiseImageGraph implements Configuration {
	/** RANSAC for fundamental matrix */
	public final ConfigRansac ransacF = new ConfigRansac();
	/** RANSAC for fundamental Homography */
	public final ConfigRansac ransacH = new ConfigRansac();
	/** Configuration for computing fundamental matrix */
	public final ConfigFundamental fundamental = new ConfigFundamental();
	/** Configuration for computing Homography matrix */
	public final ConfigHomography homography = new ConfigHomography();

	{
		ransacF.iterations = 500;
		ransacF.inlierThreshold = 1;

		// F computes epipolar error, which isn't as strict as reprojection error for H, so give H a larger error tol
		ransacH.iterations = 500;
		ransacH.inlierThreshold = 2.0;

		fundamental.errorModel = ConfigFundamental.ErrorModel.GEOMETRIC;
		fundamental.numResolve = 1;
	}

	@Override public void checkValidity() {
		ransacF.checkValidity();
		ransacH.checkValidity();
		fundamental.checkValidity();
		homography.checkValidity();
	}

	public void setTo( ConfigGeneratePairwiseImageGraph src ) {
		this.ransacF.setTo(src.ransacF);
		this.ransacH.setTo(src.ransacH);
		this.fundamental.setTo(src.fundamental);
		this.homography.setTo(src.homography);
	}
}
