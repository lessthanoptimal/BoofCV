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

package boofcv.gui.calibration;

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckGenerator;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.alg.fiducial.calib.hammingchess.HammingChessboardGenerator;
import boofcv.alg.fiducial.calib.hamminggrids.HammingGridGenerator;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.gui.FiducialRenderEngineGraphics2D;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Common utility functions for calibration UI.
 *
 * @author Peter Abeles
 */
public class UtilCalibrationGui {
	public static BufferedImage renderTargetBuffered( CalibrationPatterns type, Object config, int squareWidth ) {
		int circle = squareWidth/2;

		if (type == CalibrationPatterns.ECOCHECK) {
			ConfigECoCheckMarkers c = (ConfigECoCheckMarkers)config;

			ECoCheckUtils utils = new ECoCheckUtils();
			utils.codec.setErrorCorrectionLevel(c.errorCorrectionLevel);
			utils.codec.setChecksumBitCount(c.checksumBits);
			c.convertToGridList(utils.markers);
			utils.fixate();
			ConfigECoCheckMarkers.MarkerShape shape = c.markerShapes.get(0);

			int markerWidth = squareWidth*(shape.numCols - 1);
			int markerHeight = squareWidth*(shape.numRows - 1);

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			ECoCheckGenerator generator = new ECoCheckGenerator(utils);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render(0);
			return render.getImage();
		} else if (type == CalibrationPatterns.HAMMING_CHESSBOARD) {
			ConfigHammingChessboard c = (ConfigHammingChessboard)config;

			int markerWidth = squareWidth*c.numCols;
			int markerHeight = squareWidth*c.numRows;

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			HammingChessboardGenerator generator = new HammingChessboardGenerator(c);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render();
			return render.getImage();
		} else if (type == CalibrationPatterns.HAMMING_GRID) {
			ConfigHammingGrid c = (ConfigHammingGrid)config;

			int markerWidth = (int)Math.round(squareWidth*c.getMarkerWidth()/c.squareSize);
			int markerHeight = (int)Math.round(squareWidth*c.getMarkerHeight()/c.squareSize);

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			var generator = new HammingGridGenerator(c);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render();
			return render.getImage();
		}

		final RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(20, 1);

		if (type == CalibrationPatterns.CHESSBOARD) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			renderer.chessboard(c.numRows, c.numCols, squareWidth);
		} else if (type == CalibrationPatterns.SQUARE_GRID) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = squareWidth*c.shapeDistance/c.shapeSize;
			renderer.squareGrid(c.numRows, c.numCols, squareWidth, space);
		} else if (type == CalibrationPatterns.CIRCLE_GRID) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = circle*c.shapeDistance/c.shapeSize;
			renderer.circleRegular(c.numRows, c.numCols, circle, space);
		} else if (type == CalibrationPatterns.CIRCLE_HEXAGONAL) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = circle*c.shapeDistance/c.shapeSize;
			renderer.circleHex(c.numRows, c.numCols, circle, space);
		}
		return renderer.getBuffered();
	}

	private static FiducialRenderEngineGraphics2D configureRenderGraphics2D( int markerWidth, int markerHeight, int border ) {
		// Render the marker. Adjust marker size so that when the border is added it will match the paper size
		var render = new FiducialRenderEngineGraphics2D();
		render.configure(border, border, markerWidth, markerHeight);
		return render;
	}

	public static void renderOrder( Graphics2D g2, @Nullable Point2Transform2_F32 transform,
									double scale, List<PointIndex2D_F64> points ) {
		g2.setStroke(new BasicStroke(5));

		Point2D_F32 adj0 = new Point2D_F32();
		Point2D_F32 adj1 = new Point2D_F32();

		Line2D.Double l = new Line2D.Double();

		for (int i = 0, j = 1; j < points.size(); i = j, j++) {
			double fraction = i/((double)points.size() - 2);
//			fraction = fraction * 0.8 + 0.1;

			int red = (int)(0xFF*fraction) + (int)(0x00*(1 - fraction));
			int green = 0x00;
			int blue = (int)(0x00*fraction) + (int)(0xff*(1 - fraction));

			int lineRGB = red << 16 | green << 8 | blue;

			Point2D_F64 p0 = points.get(i).p;
			Point2D_F64 p1 = points.get(j).p;
			if (transform == null) {
				l.setLine(scale*p0.x, scale*p0.y, scale*p1.x, scale*p1.y);
			} else {
				transform.compute((float)p0.x, (float)p0.y, adj0);
				transform.compute((float)p1.x, (float)p1.y, adj1);
				l.setLine(scale*adj0.x, scale*adj0.y, scale*adj1.x, scale*adj1.y);
			}

			g2.setColor(new Color(lineRGB));
			g2.draw(l);
		}
	}

	public static void drawNumbers( Graphics2D g2, List<PointIndex2D_F64> points,
									@Nullable Point2Transform2_F32 transform,
									double scale ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.get(i).p;
			int gridIndex = points.get(i).index;

			if (transform != null) {
				transform.compute((float)p.x, (float)p.y, adj);
			} else {
				adj.setTo((float)p.x, (float)p.y);
			}

			String text = String.format("%2d", gridIndex);

			int x = (int)(adj.x*scale);
			int y = (int)(adj.y*scale);

			g2.setColor(Color.BLACK);
			g2.drawString(text, x - 1, y);
			g2.drawString(text, x + 1, y);
			g2.drawString(text, x, y - 1);
			g2.drawString(text, x, y + 1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text, x, y);
		}
	}

	public static void drawIndexes( Graphics2D g2, int fontSize, List<Point2D_F64> points,
									@Nullable Point2Transform2_F32 transform,
									double scale ) {

		int numDigits = BoofMiscOps.numDigits(points.size());
		String format = "%" + numDigits + "d";
		Font regular = new Font("Serif", Font.PLAIN, fontSize);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.get(i);

			if (transform != null) {
				transform.compute((float)p.x, (float)p.y, adj);
			} else {
				adj.setTo((float)p.x, (float)p.y);
			}

			String text = String.format(format, i);

			int x = (int)(adj.x*scale);
			int y = (int)(adj.y*scale);

			g2.setColor(Color.BLACK);
			g2.drawString(text, x - 1, y);
			g2.drawString(text, x + 1, y);
			g2.drawString(text, x, y - 1);
			g2.drawString(text, x, y + 1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text, x, y);
		}
	}

	public static void drawFeatureID( Graphics2D g2, int fontSize, List<PointIndex2D_F64> points,
									  @Nullable Point2Transform2_F32 transform,
									  double scale ) {

		int numDigits = BoofMiscOps.numDigits(points.size());
		String format = "%" + numDigits + "d";
		Font regular = new Font("Serif", Font.PLAIN, fontSize);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for (int i = 0; i < points.size(); i++) {
			PointIndex2D_F64 p = points.get(i);

			if (transform != null) {
				transform.compute((float)p.p.x, (float)p.p.y, adj);
			} else {
				adj.setTo((float)p.p.x, (float)p.p.y);
			}

			String text = String.format(format, p.index);

			int x = (int)(adj.x*scale + 0.5);
			int y = (int)(adj.y*scale + 0.5);

			g2.setColor(Color.BLACK);
			g2.drawString(text, x - 1, y);
			g2.drawString(text, x + 1, y);
			g2.drawString(text, x, y - 1);
			g2.drawString(text, x, y + 1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text, x, y);
		}
	}

	public static void drawIndexes( Graphics2D g2, int fontSize, List<ChessboardCorner> points,
									@Nullable Point2Transform2_F32 transform,
									int minLevel,
									double scale ) {

		int numDigits = BoofMiscOps.numDigits(points.size());
		String format = "%" + numDigits + "d";
		Font regular = new Font("Serif", Font.PLAIN, fontSize);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for (int i = 0; i < points.size(); i++) {
			ChessboardCorner p = points.get(i);
			if (p.level2 < minLevel)
				continue;

			if (transform != null) {
				transform.compute((float)p.x, (float)p.y, adj);
			} else {
				adj.setTo((float)p.x, (float)p.y);
			}

			String text = String.format(format, i);

			int x = (int)(adj.x*scale);
			int y = (int)(adj.y*scale);

			g2.setColor(Color.BLACK);
			g2.drawString(text, x - 1, y);
			g2.drawString(text, x + 1, y);
			g2.drawString(text, x, y - 1);
			g2.drawString(text, x, y + 1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text, x, y);
		}
	}
}
