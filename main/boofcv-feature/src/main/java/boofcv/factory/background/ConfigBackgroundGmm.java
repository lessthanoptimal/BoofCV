/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.background;

/**
 * Configuration for {@link ConfigBackgroundGmm}.
 *
 * <p><b>Tuning Advice:</b> Background models do a much better job of deciding if pixel doesn't match a model it has.
 * This background model has multiple Gaussians. So you should keep the number of Gaussians small so that when
 * the moving object goes past it doesn't because part of the model right away. If you have too many Gaussians
 * its basically impossible to keep the amount of noise down and have a stable foreground object.</p>
 *
 * @author Peter Abeles
 */
public class ConfigBackgroundGmm extends ConfigBackground {

	/**
	 * Specifies how fast it will adjust to changes in the image. Must be greater than zero.
	 */
	public float learningPeriod = 1000f;

	/**
	 * The initial variance assigned to a new pixel.  Larger values to reduce false positives due to
	 * under sampling.  Don't set to zero since that can cause divided by zero errors.
	 */
	public float initialVariance = 400;

	/**
	 * Determines how quickly a model is forgotten. Smaller values means they last longer. Values equal to
	 * or greater than one might cause new Gaussians to be immediately removed.
	 */
	public float decayCoefient = 0.005f;

	/**
	 * Maximum Mahalanobis a value can be from a Gaussian to be considered a member of the gaussian
	 */
	public float maxDistance = 3;

	/**
	 * Maximum number of gaussians that can be in a single mixture
	 */
	public int numberOfGaussian = 5;

	/**
	 * Once the weight for a Gaussian becomes greater than this amount it is no longer considered part of the
	 * foreground and is the the background model. Strongly influences how long it takes an object that was moving
	 * to fade into the background. Probably one of the first tuning variables you should mess with.
	 */
	public float significantWeight = 0.01f;

	@Override
	public void checkValidity() {
		if( learningPeriod <= 0 )
			throw new IllegalArgumentException("Learning period must be more than zero");
		if( decayCoefient < 0 )
			throw new IllegalArgumentException("Decay coeffient must be more than or equal to zero");
		if( initialVariance == 0 )
			throw new IllegalArgumentException("Don't set initialVariance to zero, set it to Float.MIN_VALUE instead");
		if( initialVariance < 0 )
			throw new IllegalArgumentException("Variance must be set to a value larger than zero");
		if( initialVariance < 0 )
			throw new IllegalArgumentException("minimumDifference must be >= 0");
	}

	@Override
	public String toString() {
		return "ConfigBackgroundGmm{" +
				"learningPeriod=" + learningPeriod +
				", initialVariance=" + initialVariance +
				", decayCoefient=" + decayCoefient +
				", maxDistance=" + maxDistance +
				", numberOfGaussian=" + numberOfGaussian +
				", significantWeight=" + significantWeight +
				", unknownValue=" + unknownValue +
				'}';
	}
}
