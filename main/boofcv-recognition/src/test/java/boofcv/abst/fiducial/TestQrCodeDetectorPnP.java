/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class TestQrCodeDetectorPnP extends GenericFiducialDetectorChecks {

	public TestQrCodeDetectorPnP() {
		types.add( ImageType.single(GrayU8.class));
		types.add( ImageType.single(GrayF32.class));
		pixelAndProjectedTol = 1.0; // should be very close
	}

	@Override
	public FiducialDetector createDetector(ImageType imageType) {
		return FactoryFiducial.qrcode3D(null,imageType.getImageClass());
	}

	@Override
	public GrayF32 renderFiducial() {
		QrCode qr = new QrCodeEncoder().addAutomatic("THE MESSAGE").fixate();
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(6);
		generator.setBorderModule(1); // interpolation gets messed up if it touches the border. plus scale is relative
		generator.render(qr);

		return generator.getGrayF32();
	}
}
