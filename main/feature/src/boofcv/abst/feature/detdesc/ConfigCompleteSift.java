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

package boofcv.abst.feature.detdesc;

import boofcv.abst.feature.describe.ConfigSiftDescribe;
import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.orientation.ConfigSiftOrientation;
import boofcv.alg.feature.detdesc.CompleteSift;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link CompleteSift}.
 *
 * @author Peter Abeles
 */
public class ConfigCompleteSift implements Configuration{

	public ConfigSiftScaleSpace scaleSpace = new ConfigSiftScaleSpace();
	public ConfigSiftDetector detector = new ConfigSiftDetector();
	public ConfigSiftOrientation orientation = new ConfigSiftOrientation();
	public ConfigSiftDescribe describe = new ConfigSiftDescribe();

	/**
	 * Creates a configuration similar to how it was originally described in the paper
	 */
	public static ConfigCompleteSift createPaper() {
		ConfigCompleteSift config = new ConfigCompleteSift();

		config.scaleSpace = ConfigSiftScaleSpace.createPaper();
		config.detector = ConfigSiftDetector.createPaper();
		config.orientation = ConfigSiftOrientation.createPaper();

		return config;
	}

	/**
	 * Constructor with default parameters for all
	 */
	public ConfigCompleteSift(){}

	/**
	 * Constructor which provides access to a few of the more critical parameters which allow you to control
	 * the number of size of detected features.  If this doesn't result in the desired results try
	 * the default constructor or manipulating other parameters directory.
	 *
	 * @param firstOctave The first octaveo.  Try -1
	 * @param lastOctave The last octave.  Try 5
	 * @param maxFeaturesPerScale Maximum number of features it will detect per scale.   &le; 0 will mean all features
	 */
	public ConfigCompleteSift( int firstOctave , int lastOctave , int maxFeaturesPerScale){
		scaleSpace.firstOctave = firstOctave;
		scaleSpace.lastOctave = lastOctave;
		detector.maxFeaturesPerScale = maxFeaturesPerScale;
	}


	@Override
	public void checkValidity() {
		scaleSpace.checkValidity();
		detector.checkValidity();
		orientation.checkValidity();
		describe.checkValidity();
	}
}
