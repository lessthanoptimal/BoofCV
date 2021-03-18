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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Renders a QR Code inside a gray scale image. To change the color you need to change it in the renderer, which can
 * be accessed with {@link #getRenderer()}.
 *
 * @author Peter Abeles
 */
public class QrCodeGeneratorImage extends QrCodeGenerator {

	int pixelsPerModule;

	// number of module wide the border/quite zone is around the qr code
	int borderModule = 2;

	FiducialImageEngine renderer = new FiducialImageEngine();

	public QrCodeGeneratorImage( int pixelsPerModule ) {
		this.pixelsPerModule = pixelsPerModule;
		setRender(renderer);
	}

	@Override
	public QrCodeGeneratorImage render( QrCode qr ) {
		super.render(qr);

		int borderPixels = renderer.getBorderPixels();

		// adjust the location to match what's in the image
		adjustSize(borderPixels, qr.ppRight);
		adjustSize(borderPixels, qr.ppCorner);
		adjustSize(borderPixels, qr.ppDown);
		adjustSize(borderPixels, qr.bounds);

		for (int i = 0; i < qr.alignment.size(); i++) {
			QrCode.Alignment a = qr.alignment.get(i);
			a.pixel.x += borderPixels;
			a.pixel.y += borderPixels;
			a.threshold = 125;
		}

		qr.threshRight = 125;
		qr.threshCorner = 125;
		qr.threshDown = 125;

		return this;
	}

	@Override
	protected void initialize( QrCode qr ) {
		this.markerWidth = pixelsPerModule*QrCode.totalModules(qr.version);
		;
		super.initialize(qr);
		renderer.configure(borderModule*pixelsPerModule, pixelsPerModule*numModules);
		renderer.init();
	}

	private void adjustSize( int borderPixels, Polygon2D_F64 poly ) {
		for (int i = 0; i < poly.size(); i++) {
			Point2D_F64 p = poly.get(i);
			p.x += borderPixels;
			p.y += borderPixels;
		}
	}

	public int getBorderModule() {
		return borderModule;
	}

	/**
	 * Used to change the white border's size.
	 *
	 * @param borderModule Border size in units of modules
	 */
	public void setBorderModule( int borderModule ) {
		this.borderModule = borderModule;
	}

	public FiducialImageEngine getRenderer() {
		return renderer;
	}

	public GrayU8 getGray() {
		return renderer.getGray();
	}

	public GrayF32 getGrayF32() {
		return renderer.getGrayF32();
	}
}
