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
				QrCode.BlockInfo block = info.levels.get(level);

				assertTrue( block.dataCodewords < block.codewords);

				int byteBlockB = block.codewords+1;
				int byteDataB = block.dataCodewords+1;
				int countB = (info.codewords - block.codewords*block.blocks);

				assertTrue( countB >= 0 );
				if( countB > 0 ) {
					assertTrue(countB % byteBlockB == 0 );
					countB /= byteBlockB;

					assertEquals( info.codewords, block.codewords*block.blocks + byteBlockB*countB);
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

	/**
	 * Compare against specification
	 */
	@Test
	public void VersionInfo_totalDataBytes() {
		assertEquals(19, QrCode.VERSION_INFO[1].totalDataBytes(QrCode.ErrorLevel.L));
		assertEquals(16, QrCode.VERSION_INFO[1].totalDataBytes(QrCode.ErrorLevel.M));
		assertEquals(13, QrCode.VERSION_INFO[1].totalDataBytes(QrCode.ErrorLevel.Q));
		assertEquals(9, QrCode.VERSION_INFO[1].totalDataBytes(QrCode.ErrorLevel.H));

		assertEquals(242-48,  QrCode.VERSION_INFO[8].totalDataBytes(QrCode.ErrorLevel.L));
		assertEquals(242-88,  QrCode.VERSION_INFO[8].totalDataBytes(QrCode.ErrorLevel.M));
		assertEquals(242-132, QrCode.VERSION_INFO[8].totalDataBytes(QrCode.ErrorLevel.Q));
		assertEquals(242-156, QrCode.VERSION_INFO[8].totalDataBytes(QrCode.ErrorLevel.H));
	}

	@Test
	public void checkClone() {
		QrCode orig = new QrCode();

		orig.threshDown = 2;
		orig.threshCorner = 3;
		orig.threshRight = 1;

		orig.ppDown.set(0,1,2);
		orig.ppCorner.set(1,3,6);
		orig.ppRight.set(2,5,8);

		orig.failureCause = QrCode.Failure.ALIGNMENT;
		orig.error = QrCode.ErrorLevel.M;
		orig.version = 4;
		orig.bounds.set(1,3,4);

		orig.message = "asdasd";

		QrCode found = orig.clone();

		assertTrue(found != orig);

		assertEquals(2,found.threshDown,1e-8);
		assertEquals(3,found.threshCorner,1e-8);
		assertEquals(1,found.threshRight,1e-8);

		assertTrue(orig.ppDown != found.ppDown);
		assertTrue(orig.ppCorner != found.ppCorner);
		assertTrue(orig.ppRight != found.ppRight);
		assertTrue(found.ppDown.get(0).distance(1,2) <= 1e-8);
		assertTrue(found.ppCorner.get(1).distance(3,6) <= 1e-8);
		assertTrue(found.ppRight.get(2).distance(5,8) <= 1e-8);

		assertTrue(orig.failureCause==found.failureCause);
		assertTrue(orig.error==found.error);
		assertTrue(orig.version==found.version);
		assertTrue(found.bounds.get(1).distance(3,4) <= 1e-8);
		assertTrue(found.bounds != orig.bounds);

		assertTrue(found.message.equals("asdasd"));

	}

}
