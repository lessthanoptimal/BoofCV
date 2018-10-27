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

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Renders a QR Code inside a gray scale image.
 *
 * @author Peter Abeles
 */
public class QrCodeGeneratorImage extends QrCodeGenerator {

	int pixelsPerModule;

	// number of module wide the border/quite zone is around the qr code
	int borderModule=2;

	FiducialImageEngine renderer = new FiducialImageEngine();
	int borderPixels,markerPixels;

	public QrCodeGeneratorImage( int pixelsPerModule) {
		super(1.0);
		this.pixelsPerModule = pixelsPerModule;
		setRender(renderer);
	}

	@Override
	public void render(QrCode qr ) {
		super.render(qr);

		borderPixels = renderer.getBorderPixels();
		markerPixels = renderer.getMarkerPixels();

		// adjust the location to match what's in the image
		adjustSize(qr.ppRight);
		adjustSize(qr.ppCorner);
		adjustSize(qr.ppDown);
		adjustSize(qr.bounds);

		for (int i = 0; i < qr.alignment.size(); i++) {
			QrCode.Alignment a = qr.alignment.get(i);
			a.pixel.x = borderPixels + a.pixel.x* markerPixels;
			a.pixel.y = borderPixels + a.pixel.y* markerPixels;
			a.threshold = 125;
		}

		qr.threshRight = 125;
		qr.threshCorner = 125;
		qr.threshDown = 125;
	}

	@Override
	protected void initialize(QrCode qr) {
		super.initialize(qr);

		renderer.configure( borderModule*pixelsPerModule, pixelsPerModule*numModules);
	}

	private void adjustSize(Polygon2D_F64 poly) {
		for (int i = 0; i < poly.size(); i++) {
			Point2D_F64 p = poly.get(i);
			p.x = borderPixels+p.x*markerPixels;
			p.y = borderPixels+p.y*markerPixels;
		}
	}
	public int getBorderModule() {
		return borderModule;
	}

	/**
	 * Used to change the white border's size.
	 * @param borderModule Border size in units of modules
	 */
	public void setBorderModule(int borderModule) {
		this.borderModule = borderModule;
	}

	public GrayU8 getGray() {
		return renderer.getGray();
	}
}
