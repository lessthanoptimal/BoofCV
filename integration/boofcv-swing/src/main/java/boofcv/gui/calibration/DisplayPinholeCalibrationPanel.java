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

import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.LensDistortionOps_F32;
import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays information images of planar calibration targets.
 *
 * @author Peter Abeles
 */
public class DisplayPinholeCalibrationPanel extends DisplayCalibrationPanel {

	// which image is being displayed
	int selectedImage;
	// for displaying undistorted image
	BufferedImage distorted;
	BufferedImage undistorted;

	// for displaying corrected image
	Planar<GrayF32> origMS = new Planar<>(GrayF32.class, 1, 1, 3);
	Planar<GrayF32> correctedMS = new Planar<>(GrayF32.class, 1, 1, 3);

	ImageDistort<GrayF32, GrayF32> undoRadial;
	Point2Transform2_F32 remove_p_to_p;

	// int horizontal line
	int lineY = -1;

	public DisplayPinholeCalibrationPanel() {
		// navigate using left mouse clicks
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e) {
				panel.requestFocus();
				if( SwingUtilities.isLeftMouseButton(e)) {
					Point2D_F64 p = pixelToPoint(e.getX(), e.getY());
					centerView(p.x,p.y);
				}
			}
		});
	}

	@Override
	public synchronized void setBufferedImageNoChange( BufferedImage image ) {
		this.distorted = image;
		undoRadialDistortion(distorted);
	}

	@Override
	public void paintComponent( Graphics g ) {
		if (showUndistorted) {
			this.img = undistorted;
		} else {
			this.img = distorted;
		}

		super.paintComponent(g);
	}

	@Override
	protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
		if (features != null && features.size() > selectedImage) {
			drawFeatures(g2, scale);
		}

		if (lineY > -1) {
			g2.setColor(Color.RED);
			g2.setStroke(new BasicStroke(3));
			g2.drawLine(0, lineY, getWidth(), lineY);
		}
	}

	private void undoRadialDistortion( BufferedImage image ) {
		if (undoRadial == null || image == null)
			return;

		ConvertBufferedImage.convertFrom(image, origMS, true);
		if (correctedMS.getNumBands() != origMS.getNumBands())
			correctedMS.setNumberOfBands(origMS.getNumBands());
		correctedMS.reshape(origMS.width, origMS.height);

		for (int i = 0; i < origMS.getNumBands(); i++) {
			GrayF32 in = origMS.getBand(i);
			GrayF32 out = correctedMS.getBand(i);

			undoRadial.apply(in, out);
		}
		undistorted = ConvertBufferedImage.checkDeclare(origMS.width, origMS.height, undistorted, image.getType());

		ConvertBufferedImage.convertTo(correctedMS, undistorted, true);
	}

	private void drawFeatures( Graphics2D g2, double scale ) {
		BoofSwingUtil.antialiasing(g2);

		CalibrationObservation set = features;

		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		Point2D_F32 adj = new Point2D_F32();

		if (showOrder) {
			List<PointIndex2D_F64> adjusted;
			if (showUndistorted) {
				adjusted = new ArrayList<>();
				for (PointIndex2D_F64 p : set.points) {
					remove_p_to_p.compute((float)p.p.x, (float)p.p.y, adj);
					adjusted.add(new PointIndex2D_F64(adj.x, adj.y));
				}
			} else {
				adjusted = set.points;
			}
			renderOrder(g2, scale, adjusted);
		}

		if (showPoints) {
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(3));
			for (PointIndex2D_F64 p : set.points) {
				if (showUndistorted) {
					remove_p_to_p.compute((float)p.p.x, (float)p.p.y, adj);
				} else {
					adj.setTo((float)p.p.x, (float)p.p.y);
				}
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
			}
			g2.setStroke(new BasicStroke(1));
			g2.setColor(Color.RED);
			for (PointIndex2D_F64 p : set.points) {
				if (showUndistorted) {
					remove_p_to_p.compute((float)p.p.x, (float)p.p.y, adj);
				} else {
					adj.setTo((float)p.p.x, (float)p.p.y);
				}
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
			}
		}

		if (showAll) {
			for (CalibrationObservation l : allFeatures) {
				for (PointIndex2D_F64 p : l.points) {
					if (showUndistorted) {
						remove_p_to_p.compute((float)p.p.x, (float)p.p.y, adj);
					} else {
						adj.setTo((float)p.p.x, (float)p.p.y);
					}
					VisualizeFeatures.drawPoint(g2, adj.x*scale, adj.y*scale, 3, Color.BLUE, Color.WHITE, ellipse);
				}
			}
		}

		if (showNumbers) {
			if (showUndistorted)
				drawNumbers(g2, set.points, remove_p_to_p, scale);
			else
				drawNumbers(g2, set.points, null, scale);
		}

		if (showErrors && results != null) {


			for (int i = 0; i < set.size(); i++) {
				PointIndex2D_F64 p = set.get(i);

				if (showUndistorted) {
					remove_p_to_p.compute((float)p.p.x, (float)p.p.y, adj);
				} else {
					adj.setTo((float)p.p.x, (float)p.p.y);
				}

				double r = scale*errorScale*results.pointError[i];
				if (r < 1)
					continue;

				g2.setStroke(new BasicStroke(4));
				g2.setColor(Color.BLACK);
				VisualizeFeatures.drawCircle(g2, adj.x*scale, adj.y*scale, r, ellipse);

				g2.setStroke(new BasicStroke(2.5f));
				g2.setColor(Color.ORANGE);
				VisualizeFeatures.drawCircle(g2, adj.x*scale, adj.y*scale, r, ellipse);
			}
		}
	}

	public static void renderOrder( Graphics2D g2, double scale, List<PointIndex2D_F64> points ) {
		g2.setStroke(new BasicStroke(5));

		Line2D.Double l = new Line2D.Double();

		for (int i = 0, j = 1; j < points.size(); i = j, j++) {
			Point2D_F64 p0 = points.get(i).p;
			Point2D_F64 p1 = points.get(j).p;

			double fraction = i/((double)points.size() - 2);
//			fraction = fraction * 0.8 + 0.1;

			int red = (int)(0xFF*fraction) + (int)(0x00*(1 - fraction));
			int green = 0x00;
			int blue = (int)(0x00*fraction) + (int)(0xff*(1 - fraction));

			int lineRGB = red << 16 | green << 8 | blue;

			l.setLine(scale*p0.x, scale*p0.y, scale*p1.x, scale*p1.y);

			g2.setColor(new Color(lineRGB));
			g2.draw(l);
		}
	}

	public void setCalibration( CameraPinholeBrown param ) {
		CameraPinhole undistorted = new CameraPinhole(param);
		this.undoRadial = LensDistortionOps.changeCameraModel(
				AdjustmentType.FULL_VIEW, BorderType.ZERO, param, undistorted, null, ImageType.single(GrayF32.class));
		this.remove_p_to_p = LensDistortionOps_F32.transformChangeModel(AdjustmentType.FULL_VIEW, param, undistorted, false, null);

		undoRadialDistortion(distorted);
	}

	public void setCalibration( CameraPinholeBrown param, DMatrixRMaj rect ) {
		FMatrixRMaj rect_f32 = new FMatrixRMaj(3, 3);
		ConvertMatrixData.convert(rect, rect_f32);

		this.undoRadial = RectifyDistortImageOps.rectifyImage(
				param, rect_f32, BorderType.ZERO, ImageType.single(GrayF32.class));
		this.remove_p_to_p = RectifyImageOps.transformPixelToRect(param, rect_f32);
	}

	public void setLine( int y ) {
		this.lineY = y;
	}

	public static void drawNumbers( Graphics2D g2, List<PointIndex2D_F64> points,
									Point2Transform2_F32 transform,
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
									Point2Transform2_F32 transform,
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
									  Point2Transform2_F32 transform,
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
									Point2Transform2_F32 transform,
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

	@Override public void clearCalibration() {
		undoRadial = null;
	}
}
