/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.disparity;

import boofcv.testing.StandardConfigurationChecks;
import org.junit.jupiter.api.Test;

public class TestConfigDisparitySGM extends StandardConfigurationChecks {
	/** A negative minimum disparity is valid, e.g. depth from dual pixels. */
	@Test void negativeDisparityMinIsAllowed() {
		var config = new ConfigDisparitySGM();
		config.disparityMin = -10;
		config.disparityRange = 20;
		config.checkValidity();
	}
}
