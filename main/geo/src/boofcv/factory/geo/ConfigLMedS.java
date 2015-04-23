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

package boofcv.factory.geo;

import boofcv.struct.Configuration;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;

/**
 * Standard configuration parameters for {@link LeastMedianOfSquares}
 *
 * @author Peter Abeles
 */
public class ConfigLMedS implements Configuration {
	/**
	 * Random seed that's used internally
	 */
	public long randSeed;
	/**
	 * Number of cycles it will perform when minimizing the median error
	 */
	public int totalCycles;

	public ConfigLMedS() {
	}

	public ConfigLMedS(long randSeed, int totalCycles) {
		this.randSeed = randSeed;
		this.totalCycles = totalCycles;


	}

	@Override
	public void checkValidity() {

	}
}
