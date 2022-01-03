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

package boofcv.abst.fiducial;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.microqr.MicroQrCodeEncoder;
import boofcv.alg.fiducial.microqr.MicroQrCodeGenerator;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

public class TestMicroQrCodeDetectorPnP extends GenericFiducialDetectorChecks {
	public TestMicroQrCodeDetectorPnP() {
		types.add(ImageType.single(GrayU8.class));
		types.add(ImageType.single(GrayF32.class));
		pixelAndProjectedTol = 1.0; // should be very close
		stabilityShrink = 0.3;
	}

	@Override public FiducialDetector createDetector( ImageType imageType ) {
		return FactoryFiducial.microqr3D(null, imageType.getImageClass());
	}

	@Override public GrayF32 renderFiducial() {
		MicroQrCode qr = new MicroQrCodeEncoder().addAutomatic("THE MESSAGE").fixate();

		int width = MicroQrCode.totalModules(qr.version)*6;
		var engine = new FiducialImageEngine();
		// interpolation gets messed up if it touches the border. plus scale is relative
		engine.configure(1, width);
		var generator = new MicroQrCodeGenerator();
		generator.markerWidth = width;
		generator.setRender(engine);
		generator.render(qr);

		return engine.getGrayF32();
	}
}
