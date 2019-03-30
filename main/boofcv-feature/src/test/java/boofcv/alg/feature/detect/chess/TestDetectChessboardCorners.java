/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author Peter Abeles
 */
class TestDetectChessboardCorners {

	/**
	 * Rotate a chessboard pattern and see if all the corners are detected
	 */
	@Test
	void process_rotate() {
		fail("implement");
	}

	/**
	 * Give it a small chessboard and see if it detects it
	 */
	@Test
	void process_small() {
		// go through features of different radius settings
		fail("implement");
	}

	@Test
	void computefeatures() {
		fail("implement");
	}

	@Test
	void meanShiftLocation() {
		fail("implement");
	}
}
