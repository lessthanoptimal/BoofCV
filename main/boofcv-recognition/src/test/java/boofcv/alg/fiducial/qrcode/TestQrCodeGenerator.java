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

/**
 * @author Peter Abeles
 */
public class TestQrCodeGenerator {
	/**
	 * Compare against example from reference
	 */
	@Test
	public void formatInformationBits() {
		QrCode qr = new QrCode();
		qr.version = 1;
		qr.error = QrCode.ErrorLevel.M;
		qr.mask = QrCodeMaskPattern.M101;

		PackedBits32 found = QrCodeGenerator.formatInformationBits(qr);

		int expected = 0b1000000_11001110;
		assertEquals(expected,found.data[0]);
	}

	@Test
	public void alignmentPosition() {
		QrCode qr = new QrCodeEncoder().setVersion(2).addNumeric("123345").fixate();

		Generator alg = new Generator();

		alg.render(qr);

		// get the location of alignment patterns in terms of modules
		int alignment[] = QrCode.VERSION_INFO[qr.version].alignment;

		QrCode.Alignment a = qr.alignment.get(0);
		int N = qr.getNumberOfModules();

		assertEquals(N-1-alignment[0],a.moduleX);
		assertEquals(N-1-alignment[0],a.moduleY);
		assertEquals((N-1-alignment[0]+0.5)/N,a.pixel.x, 1e-8);
		assertEquals((N-1-alignment[0]+0.5)/N,a.pixel.y, 1e-8);


	}

	public class Generator extends QrCodeGenerator {

		public Generator() {
			super(1.0);
		}

		@Override
		public void init() {

		}

		@Override
		public void square(double x0, double y0, double width) {

		}

		@Override
		public void square(double x0, double y0, double width0, double thickness) {

		}
	}
}