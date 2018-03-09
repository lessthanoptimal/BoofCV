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

package boofcv.alg.background.stationary;

import boofcv.struct.image.ImageType;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public abstract class GenericBackgroundStationaryGmmChecks extends GenericBackgroundModelStationaryChecks {

	float initialVariance;

	@Before
	public void init() {
		initialVariance = 12;
	}

	@Test
	public void initialVariance() {
		for( ImageType type : imageTypes ) {
//			initialVariance(type);
		}
	}


	@Test
	public void learnRate() {
		for( ImageType type : imageTypes ) {
//			checkLearnRate_slow(type);
//			checkLearnRate_fast(type);
		}
	}
}
