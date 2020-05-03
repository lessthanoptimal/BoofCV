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

package boofcv.struct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestConfigGridUniform extends StandardConfigurationChecks {
	/**
	 * Compare to hand computed solution
	 */
	@Test
	void selectTargetCellSize_hand() {
		// test a few easy cases
		var config = new ConfigGridUniform();
		config.minCellLength = 1;
		config.regionScaleFactor = 1.0;
		assertEquals(1,config.selectTargetCellSize(200,10,20));
		assertEquals(2,config.selectTargetCellSize(200,10,40));
		assertEquals(2,config.selectTargetCellSize(50,10,20));
		assertEquals(5,config.selectTargetCellSize(12,10,20));

		// is the min cell length respected
		config.minCellLength = 10;
		assertEquals(10,config.selectTargetCellSize(200,10,20));
		assertEquals(50,config.selectTargetCellSize(1,50,50));

		// how about scaling
		config.minCellLength = 1;
		config.regionScaleFactor = 2.0;
		assertEquals(2,config.selectTargetCellSize(200,10,20));
		assertEquals(3,config.selectTargetCellSize(200,10,40));
		assertEquals(4,config.selectTargetCellSize(50,10,20));
		assertEquals(9,config.selectTargetCellSize(12,10,20));
	}

	/**
	 * See how close the results are to a uniform distribution if every region contributes once
	 */
	@Test
	void selectTargetCellSize_uniform() {
		checkUniform(200,10,20);
		checkUniform(50,10,20);
		checkUniform(1000,800,600);
		checkUniform(500,800,600);
		checkUniform(500,4000,3000);
	}

	private void checkUniform( int maxSample, int imageWidth, int imageHeight ) {
		var config = new ConfigGridUniform();
		config.minCellLength = 1;
		config.regionScaleFactor = 1.0;
		double length = config.selectTargetCellSize(maxSample,imageWidth,imageHeight);

		double predicted = (imageWidth/length)*(imageHeight/length);
		double ratio = predicted/(double)maxSample;
		assertEquals(1.0,ratio,0.1);
	}
}
