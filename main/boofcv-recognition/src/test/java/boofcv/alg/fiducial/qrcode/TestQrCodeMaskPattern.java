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

import static boofcv.alg.fiducial.qrcode.QrCodeMaskPattern.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestQrCodeMaskPattern {
	/**
	 * Exhaustively check to make sure the output is always 0 or 1 when given an input of 0 or 1
	 */
	@Test
	public void ensureOutputIs0or1() {
		ensureOutputIs0or1(M000);
		ensureOutputIs0or1(M001);
		ensureOutputIs0or1(M010);
		ensureOutputIs0or1(M011);
		ensureOutputIs0or1(M100);
		ensureOutputIs0or1(M101);
		ensureOutputIs0or1(M110);
		ensureOutputIs0or1(M111);
	}

	@Test
	public void checkM000() {
		assertEquals(1,M000.apply(0,0,0));
		assertEquals(0,M000.apply(0,1,0));
		assertEquals(0,M000.apply(1,0,0));
		assertEquals(1,M000.apply(1,1,0));

		assertEquals(0,M000.apply(0,0,1));
	}

	@Test
	public void checkM001() {
		assertEquals(1,M001.apply(0,0,0));
		assertEquals(1,M001.apply(0,1,0));
		assertEquals(0,M001.apply(1,0,0));
		assertEquals(0,M001.apply(1,1,0));

		assertEquals(0,M001.apply(0,0,1));
	}

	@Test
	public void checkM010() {
		assertEquals(1,M010.apply(0,0,0));
		assertEquals(0,M010.apply(0,1,0));
		assertEquals(0,M010.apply(0,2,0));
		assertEquals(1,M010.apply(0,3,0));
		assertEquals(1,M010.apply(1,0,0));

		assertEquals(0,M010.apply(0,0,1));
	}

	@Test
	public void checkM011() {
		assertEquals(1,M011.apply(0,0,0));
		assertEquals(0,M011.apply(0,1,0));
		assertEquals(0,M011.apply(0,2,0));
		assertEquals(1,M011.apply(0,3,0));
		assertEquals(0,M011.apply(1,1,0));
		assertEquals(1,M011.apply(1,2,0));

		assertEquals(0,M011.apply(0,0,1));
	}

	@Test
	public void checkM100() {
		assertEquals(1,M100.apply(0,0,0));
		assertEquals(1,M100.apply(0,1,0));
		assertEquals(1,M100.apply(0,2,0));
		assertEquals(0,M100.apply(0,3,0));
		assertEquals(1,M100.apply(1,0,0));
		assertEquals(0,M100.apply(2,0,0));

		assertEquals(0,M100.apply(0,0,1));
	}

	@Test
	public void checkM101() {
		assertEquals(1,M101.apply(0,0,0));
		assertEquals(1,M101.apply(0,1,0));
		assertEquals(1,M101.apply(0,2,0));
		assertEquals(1,M101.apply(0,3,0));
		assertEquals(1,M101.apply(1,0,0));
		assertEquals(1,M101.apply(2,0,0));
		assertEquals(0,M101.apply(1,1,0));
		assertEquals(0,M101.apply(2,2,0));
		assertEquals(1,M101.apply(2,3,0));

		assertEquals(0,M101.apply(0,0,1));
	}

	@Test
	public void checkM110() {
		assertEquals(1,M110.apply(0,0,0));
		assertEquals(1,M110.apply(0,1,0));
		assertEquals(1,M110.apply(0,2,0));
		assertEquals(1,M110.apply(0,3,0));
		assertEquals(1,M110.apply(1,0,0));
		assertEquals(1,M110.apply(2,0,0));
		assertEquals(1,M110.apply(1,1,0));
		assertEquals(0,M110.apply(2,2,0));
		assertEquals(1,M110.apply(1,2,0));

		assertEquals(0,M110.apply(0,0,1));
	}

	@Test
	public void checkM111() {
		assertEquals(1,M111.apply(0,0,0));
		assertEquals(0,M111.apply(0,1,0));
		assertEquals(1,M111.apply(0,2,0));
		assertEquals(0,M111.apply(0,3,0));
		assertEquals(0,M111.apply(1,0,0));
		assertEquals(1,M111.apply(2,0,0));
		assertEquals(0,M111.apply(1,1,0));
		assertEquals(0,M111.apply(2,2,0));
		assertEquals(0,M111.apply(1,2,0));
		assertEquals(1,M111.apply(1,3,0));

		assertEquals(0,M111.apply(0,0,1));
	}

	public void ensureOutputIs0or1( QrCodeMaskPattern pattern ) {
		int found;
		for (int i = 0; i < 30; i++) {
			for (int j = 0; j < 30; j++) {
				found = pattern.apply(i,j,1);
				assertTrue(found==0||found==1);
				found = pattern.apply(i,j,0);
				assertTrue(found==0||found==1);
			}
		}
	}
}