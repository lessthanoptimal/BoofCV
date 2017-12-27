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

package boofcv.gui;

import boofcv.abst.fiducial.calib.RenderCalibrationTargets;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class RenderCalibrationTargetsGraphics2D extends RenderCalibrationTargets {

	int padding;
	double unitsToPixels;

	BufferedImage bufferred;
	Graphics2D g2;

	public RenderCalibrationTargetsGraphics2D(int padding, double unitsToPixels) {
		this.padding = padding;
		this.unitsToPixels = unitsToPixels;
	}

	@Override
	public void specifySize(double width, double height) {
		int w = (int)(unitsToPixels*width+0.5);
		int h = (int)(unitsToPixels*height+0.5);

		bufferred = new BufferedImage(w+2*padding,h+2*padding,BufferedImage.TYPE_INT_RGB);
		g2 = bufferred.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,bufferred.getWidth(),bufferred.getHeight());
		g2.setColor(Color.BLACK);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	@Override
	public void drawSquare(double x, double y, double width) {
		Rectangle2D.Double r = new Rectangle2D.Double();
		r.x = padding + x*unitsToPixels;
		r.y = padding + y*unitsToPixels;
		r.width = width*unitsToPixels;
		r.height = r.width;

		g2.fill(r);
	}

	@Override
	public void drawCircle(double cx, double cy, double diameter) {
		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		ellipse.x = padding + cx*unitsToPixels-diameter/2;
		ellipse.y = padding + cy*unitsToPixels-diameter/2;
		ellipse.width = diameter*unitsToPixels;
		ellipse.height = diameter*unitsToPixels;
		g2.fill(ellipse);
	}

	public GrayU8 getGrayU8() {
		GrayU8 gray = new GrayU8(bufferred.getWidth(),bufferred.getHeight());
		ConvertBufferedImage.convertFrom(bufferred,gray);
		return gray;
	}

	public GrayF32 getGrayF32() {
		GrayF32 gray = new GrayF32(bufferred.getWidth(),bufferred.getHeight());
		ConvertBufferedImage.convertFrom(bufferred,gray);
		return gray;
	}

	public BufferedImage getBufferred() {
		return bufferred;
	}
}
