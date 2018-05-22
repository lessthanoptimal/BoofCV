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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Renders a QR Code inside a gray scale image.
 *
 * @author Peter Abeles
 */
public class QrCodeGeneratorImage extends QrCodeGenerator {

	GrayU8 gray = new GrayU8(1,1);
	int pixelsPerModule;

	// number of module wide the border/quite zone is around the qr code
	int borderModule=2;

	// number of pixels in the border
	private int borderPixels;
	// Width of the marker in pixels
	private int markerPixels;

	public QrCodeGeneratorImage( int pixelsPerModule) {
		super(1.0);
		this.pixelsPerModule = pixelsPerModule;
	}

	@Override
	public void init() {
		borderPixels = borderModule*pixelsPerModule;
		markerPixels = pixelsPerModule*numModules;
		int width = markerPixels +2*borderPixels;
		gray.reshape(width,width);
		ImageMiscOps.fill(gray,255);
	}

	@Override
	public void render(QrCode qr ) {
		super.render(qr);
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

	private void adjustSize(Polygon2D_F64 poly) {
		for (int i = 0; i < poly.size(); i++) {
			Point2D_F64 p = poly.get(i);
			p.x = borderPixels+p.x*markerPixels;
			p.y = borderPixels+p.y*markerPixels;
		}
	}

	@Override
	public void square(double x0, double y0, double width) {
		int pixelX = borderPixels+(int)(x0* markerPixels +0.5);
		int pixelY = borderPixels+(int)(y0* markerPixels +0.5);
		int pixelsWidth = (int)(width* markerPixels +0.5);

		ImageMiscOps.fillRectangle(gray,0,pixelX,pixelY,
				pixelsWidth, pixelsWidth);
	}

	@Override
	public void square(double x0, double y0, double width0, double thickness) {

		int X0 = borderPixels+(int)(x0* markerPixels +0.5);
		int Y0 = borderPixels+(int)(y0* markerPixels +0.5);
		int WIDTH = (int)(width0* markerPixels +0.5);
		int THICKNESS = (int)(thickness * markerPixels +0.5);

		ImageMiscOps.fillRectangle(gray,0,X0,Y0,WIDTH,THICKNESS);
		ImageMiscOps.fillRectangle(gray,0,X0,Y0+WIDTH-THICKNESS,WIDTH,THICKNESS);
		ImageMiscOps.fillRectangle(gray,0,X0,Y0+THICKNESS,THICKNESS,WIDTH-THICKNESS*2);
		ImageMiscOps.fillRectangle(gray,0,X0+WIDTH-THICKNESS,Y0+THICKNESS,THICKNESS,WIDTH-THICKNESS*2);
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
		return gray;
	}
}
