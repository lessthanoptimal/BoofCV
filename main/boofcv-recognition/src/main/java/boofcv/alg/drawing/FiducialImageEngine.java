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

package boofcv.alg.drawing;

import boofcv.abst.distort.FDistort;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.ConvertImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class FiducialImageEngine extends FiducialRenderEngine {
	protected GrayU8 gray = new GrayU8(1,1);

	// number of pixels in the border
	private int borderPixels;
	// Width of the marker in pixels
	private int markerPixels;

	/**
	 *
	 * @param borderPixels size of white border around document
	 * @param markerPixels size of workable region inside the document
	 */
	public void configure( int borderPixels , int markerPixels ) {
		this.borderPixels = borderPixels;
		this.markerPixels = markerPixels;

		int width = markerPixels +2*borderPixels;
		gray.reshape(width,width);
	}

	@Override
	public void init() {
		ImageMiscOps.fill(gray,255);
	}

	@Override
	public void rectangle(double x0, double y0, double x1 , double y1) {
		int pixelX0 = borderPixels+(int)(x0* markerPixels +0.5);
		int pixelY0 = borderPixels+(int)(y0* markerPixels +0.5);
		int pixelX1 = borderPixels+(int)(x1* markerPixels +0.5);
		int pixelY1 = borderPixels+(int)(y1* markerPixels +0.5);

		ImageMiscOps.fillRectangle(gray,0,pixelX0,pixelY0,pixelX1-pixelX0,pixelY1-pixelY0);
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

	@Override
	public void draw(GrayU8 image, double x0, double y0, double x1, double y1) {
		int X0 = borderPixels+(int)(x0* markerPixels +0.5);
		int Y0 = borderPixels+(int)(y0* markerPixels +0.5);
		int X1 = borderPixels+(int)(x1* markerPixels +0.5);
		int Y1 = borderPixels+(int)(y1* markerPixels +0.5);

		GrayU8 out = new GrayU8(X1-X0,Y1-Y0);
		new FDistort(image,out).scale().apply();

		gray.subimage(X0,Y0,X1,Y1).setTo(out);
	}

	public int getBorderPixels() {
		return borderPixels;
	}

	public int getMarkerPixels() {
		return markerPixels;
	}

	public GrayU8 getGray() {
		return gray;
	}

	public GrayF32 getGrayF32() {
		GrayF32 out = new GrayF32(gray.width,gray.height);
		ConvertImage.convert(gray,out);
		return out;
	}
}
