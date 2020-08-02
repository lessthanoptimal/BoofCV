/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.geo;

import boofcv.struct.Configuration;

/**
 * Projective to metric self calibration algorithm configuration which lets you select multiple approaches.
 *
 * @author Peter Abeles
 */
public class ConfigSelfCalibration implements Configuration {

	/** Which algorithm to use. The default will change as the recommended best approach is updated. */
	public Type type = Type.DUAL_QUADRATIC;

	/** Configuration for estimating the trifocal tensor */
	public ConfigTrifocal trifocal = new ConfigTrifocal();

	// configurations for specific approaches
	public ConfigSelfCalibDualQuadratic dualQuadratic = new ConfigSelfCalibDualQuadratic();
	public ConfigSelfCalibEssentialGuess essentialGuess = new ConfigSelfCalibEssentialGuess();
	public ConfigSelfCalibPracticalGuess practicalGuess = new ConfigSelfCalibPracticalGuess();

	@Override
	public void checkValidity() {
		dualQuadratic.checkValidity();
		essentialGuess.checkValidity();
		practicalGuess.checkValidity();
	}

	public void setTo( ConfigSelfCalibration src ) {
		this.type = src.type;
		this.trifocal.setTo(src.trifocal);
		this.dualQuadratic.setTo(src.dualQuadratic);
		this.essentialGuess.setTo(src.essentialGuess);
		this.practicalGuess.setTo(src.practicalGuess);
	}

	public enum Type {
		/** @see ConfigSelfCalibDualQuadratic */
		DUAL_QUADRATIC,
		/** @see ConfigSelfCalibEssentialGuess */
		ESSENTIAL_GUESS,
		/** @see ConfigSelfCalibPracticalGuess */
		PRACTICAL_GUESS
	}
}
