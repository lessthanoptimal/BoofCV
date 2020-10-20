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

package boofcv.struct.pyramid;

import boofcv.struct.StandardConfigurationChecks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestConfigDiscreteLevels extends StandardConfigurationChecks {
	@Test
	void handleSelected() {
		// well defined cases
		assertEquals(3,new ConfigDiscreteLevels(-1,10,-1).computeLayers(40,80));
		assertEquals(3,new ConfigDiscreteLevels(-1,-1,20).computeLayers(40,80));
		assertEquals(3,new ConfigDiscreteLevels(3,-1,-1).computeLayers(40,80));

		// ambiguous
		assertEquals(2,new ConfigDiscreteLevels(-1,11,-1).computeLayers(40,80));
		assertEquals(2,new ConfigDiscreteLevels(-1,-1,21).computeLayers(40,80));
	}
}
