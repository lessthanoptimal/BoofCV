/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.struct;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * Checks simple data structures which implement setTo() and reset()
 */
public abstract class StandardStructChecks extends BoofStandardJUnit {

	Class<?> type;
	protected String resetName = "reset";

	protected StandardStructChecks( Class<?> type ) {
		this.type = type;
	}

	protected StandardStructChecks() {type = lookUpClassFromTestName();}

	@Test void setTo() {checkSetTo(type, true);}

	@Test void reset() throws Exception {checkReset(type, resetName);}
}
