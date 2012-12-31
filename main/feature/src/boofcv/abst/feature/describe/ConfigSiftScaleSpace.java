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

package boofcv.abst.feature.describe;

import boofcv.struct.Configuration;

/**
 * Configures the scale-space used by SIFT feature detector/descriptor.
 *
 * @see boofcv.alg.feature.detect.interest.SiftImageScaleSpace
 *
 * @author Peter Abeles
 */
public class ConfigSiftScaleSpace implements Configuration {

	/**
	 * Amount of blur applied to each scale inside an octaves.  Try 1.6
	 */
	public float blurSigma = 1.6f;
	/**
	 * Number of scales per octaves.  Try 5.  Must be >= 3
	 */
	public int numScales = 5;
	/**
	 * Number of octaves to detect.  Try 4
	 */
	public int numOctaves = 4;
	/**
	 * Should the input image be doubled? Try false.
	 */
	public boolean doubleInputImage = false;

	public ConfigSiftScaleSpace(float blurSigma, int numScales, int numOctaves, boolean doubleInputImage) {
		this.blurSigma = blurSigma;
		this.numScales = numScales;
		this.numOctaves = numOctaves;
		this.doubleInputImage = doubleInputImage;
	}

	public ConfigSiftScaleSpace() {
	}

	@Override
	public void checkValidity() {
	}
}
