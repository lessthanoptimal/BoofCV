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
import static org.junit.Assert.assertTrue;

public class TestQrCode {

	/**
	 * Can through version info and see if it passes a few simple checks. It's still possible for wrong entries to
	 * exist.
	 */
	@Test
	public void sanityVersionInfo() {
		for (int version = 1; version <= QrCode.MAX_VERSION; version++) {
//			System.out.println("version "+version);
			QrCode.VersionInfo info = QrCode.VERSION_INFO[version];

			// the number of bits determined by applying a mask to all static features should match the entered
			// number of code words
			int totalBits = new QrCodeCodeWordLocations(version).bits.size();
			assertEquals(totalBits/8,info.codewords);

			// test values found at each error level
			for( QrCode.ErrorLevel level : QrCode.ErrorLevel.values() ) {
				QrCode.ErrorBlock block = info.levels.get(level);

				assertTrue( block.dataCodewords < block.codewords);

				int byteBlockB = block.codewords+1;
				int byteDataB = block.dataCodewords+1;
				int countB = (info.codewords - block.codewords*block.eccBlocks);

				assertTrue( countB >= 0 );
				if( countB > 0 ) {
					assertTrue(countB % byteBlockB == 0 );
					countB /= byteBlockB;

					assertEquals( info.codewords, block.codewords*block.eccBlocks + byteBlockB*countB);
					assertTrue( byteDataB < byteBlockB);
				}


			}
		}
	}

	/**
	 * The last digit in the alignment pattern should always be increasing. Try to catch type-os
	 */
	@Test
	public void versionInfo_alignment_lastNumberIncreasing() {
		for (int version = 3; version <= QrCode.MAX_VERSION; version++) {
//			System.out.println("version "+version);
			QrCode.VersionInfo infoA = QrCode.VERSION_INFO[version-1];
			QrCode.VersionInfo infoB = QrCode.VERSION_INFO[version];

			assertTrue(infoA.alignment[infoA.alignment.length-1] < infoB.alignment[infoB.alignment.length-1]);
		}
	}
}
