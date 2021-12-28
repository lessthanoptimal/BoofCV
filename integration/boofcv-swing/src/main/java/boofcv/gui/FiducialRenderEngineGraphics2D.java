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

package boofcv.gui;

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Implementation of {@link boofcv.alg.drawing.FiducialRenderEngine} for a {@link java.awt.image.BufferedImage}.
 */
@SuppressWarnings({"NullAway.Init"})
public class FiducialRenderEngineGraphics2D extends FiducialRenderEngine {
	@Getter protected BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	protected Graphics2D g2;
	protected Ellipse2D.Double ellipse = new Ellipse2D.Double();
	protected Rectangle2D.Double rect = new Rectangle2D.Double();

	// number of pixels in the border
	@Getter private int borderPixelsX;
	@Getter private int borderPixelsY;

	// value of the two color squares
	private final Color white = Color.WHITE;
	private final Color black = Color.BLACK;

	// What color it will draw shapes with
	@Getter private Color drawColor = black;


	public void configure( int borderPixelsX, int borderPixelsY, int markerWidth, int markerHeight ) {
		this.borderPixelsX = borderPixelsX;
		this.borderPixelsY = borderPixelsY;

		int width = markerWidth + 2*borderPixelsX;
		int height = markerHeight + 2*borderPixelsY;
		image = ConvertBufferedImage.checkDeclare(width, height, image, image.getType());
		g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	@Override
	public void init() {
		g2.setColor(white);
		g2.fillRect(0, 0, image.getWidth(), image.getHeight());
	}

	@Override public void setGray( double value ) {
		int v = (int)(255*value);
		drawColor = new Color(v, v, v);
	}

	@Override
	public void circle( double cx, double cy, double radius ) {
		ellipse.setFrame(borderPixelsX + cx - radius, borderPixelsY + cy - radius, radius*2, radius*2);
		g2.setColor(drawColor);
		g2.fill(ellipse);
	}

	@Override
	public void rectangle( double x0, double y0, double x1, double y1 ) {
		rect.setRect(borderPixelsX + x0, borderPixelsY + y0, x1-x0, y1-y0);
		g2.setColor(drawColor);
		g2.fill(rect);
	}

	@Override
	public void square( double x0, double y0, double width, double thickness ) {
		g2.setColor(drawColor);
		x0 += borderPixelsX;
		y0 += borderPixelsY;

		rect.setRect(x0, y0, width, thickness);
		g2.fill(rect);
		rect.setRect(x0, y0+width-thickness, width, thickness);
		g2.fill(rect);
		rect.setRect(x0, y0+thickness, thickness, width-2*thickness);
		g2.fill(rect);
		rect.setRect(x0+width-thickness, y0+thickness, thickness, width-2*thickness);
		g2.fill(rect);
	}

	@Override
	public void draw( GrayU8 image, double x0, double y0, double x1, double y1 ) {
		double scaleX = image.width/(x1-x0);
		double scaleY = image.height/(y1-y0);

		BufferedImage buff = ConvertBufferedImage.convertTo(image, null, true);
		AffineTransform original = g2.getTransform();
		g2.setTransform(AffineTransform.getScaleInstance(scaleX, scaleY));
		g2.drawImage(buff, borderPixelsX + (int)(x0 + 0.5), borderPixelsY + (int)(y0 + 0.5), null);
		g2.setTransform(original);
	}

	@Override
	public void inputToDocument( double x, double y, Point2D_F64 document ) {
		document.x = x + borderPixelsX;
		document.y = y + borderPixelsY;
	}

	public GrayU8 getGrayU8() {
		GrayU8 out = new GrayU8(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, out, true);
		return out;
	}

	public GrayF32 getGrayF32() {
		GrayF32 out = new GrayF32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, out, true);
		return out;
	}
}
