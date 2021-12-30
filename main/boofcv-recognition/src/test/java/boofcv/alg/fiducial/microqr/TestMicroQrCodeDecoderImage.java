/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.microqr;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.gui.image.ShowImages;
import boofcv.misc.BoofMiscOps;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestMicroQrCodeDecoderImage {
	@Test void basic() {
		var engine = new FiducialImageEngine();
		engine.configure(10, 100);
		var generator = new MicroQrCodeGenerator();
		generator.markerWidth = 100;
		generator.render = engine;

		var qr = new MicroQrCode();
		qr.version = 2;
		qr.error = MicroQrCode.ErrorLevel.L;

		generator.render(qr);

		ShowImages.showWindow(engine.getGray(), "Micro QR");
		BoofMiscOps.sleep(10_000);
	}
}
