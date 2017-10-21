/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestQrCodeDataBits {

	private QrCodePatternLocations locations = new QrCodePatternLocations();

	/**
	 * Compare the number of modules available to store data against the specification
	 */
	@Test
	public void dataCapability() {
		dataCapability(1,208);
		dataCapability(2,359);
		dataCapability(3,567);
		dataCapability(4,807);
		dataCapability(5,1079);
		dataCapability(6,1383);
		dataCapability(7,1568);
		dataCapability(9,2336);
		dataCapability(20,8683);
		dataCapability(40,29648);
	}

	private void dataCapability(int version , int expected ) {
		QrCodeDataBits mask = new QrCodeDataBits(locations.size[version],
				locations.alignment[version],version >= QrCodePatternLocations.VERSION_VERSION);

		int countData = 0;
		for (int i = 0; i < mask.getNumElements(); i++) {
			if(!mask.get(i))
				countData++;
		}
		assertEquals(expected,countData);
		assertEquals(expected,mask.dataBits);
	}
}