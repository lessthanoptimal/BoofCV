/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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


import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link SiftScaleSpace}
 *
 * @author Peter Abeles
 */
public class ConfigSiftScaleSpace implements Configuration {

	/**
	 * Amount of blur at the first level in the image pyramid.  Recommend 1.6
	 */
	public float sigma0 = 2.75f;

	/**
	 * Number of scales in each octave.  The amount of Gaussian blur will double this number of images in the
	 * octave.  However, the number of actual images computed will be numScales + 3 and the number of difference
	 * of Guassian images will be numScales + 2.
	 */
	public int numScales = 3;

	/**
	 * Specified the first and last octaves.  Each octave is a factor of 2 smaller or larger
	 * than the input image.  The overall size of an octave relative to the input image is pow(2,-octave)
	 */
	public int firstOctave = -1, lastOctave = 5;

	/**
	 * Creates a configuration similar to how it was originally described in the paper
	 */
	public static ConfigSiftScaleSpace createPaper() {
		ConfigSiftScaleSpace config = new ConfigSiftScaleSpace();
		config.sigma0 = 1.6f;
		config.numScales = 3;
		config.firstOctave = -1;
		config.lastOctave = 5;
		return config;
	}

	@Override
	public void checkValidity() {

	}
}
