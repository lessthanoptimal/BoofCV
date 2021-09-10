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

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.distort.NarrowToWidePtoP_F32;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.geometry.UtilVector3D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Vector3D_F32;
import georegression.struct.so.Rodrigues_F32;
import org.ejml.data.FMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static boofcv.gui.calibration.DisplayPinholeCalibrationPanel.drawNumbers;

/**
 * Used to display a calibrated fisheye camera. Shows a rendered pinhole camera view at the selected location
 * if configured to do so. Will also show feature location
 *
 * @author Peter Abeles
 */
public class DisplayFisheyeCalibrationPanel extends DisplayCalibrationPanel {

	@Nullable NarrowToWidePtoP_F32 distorter;
	@Nullable LensDistortionWideFOV fisheyeDistort;
	ImageDistort<Planar<GrayF32>, Planar<GrayF32>> distortImage;

	Planar<GrayF32> imageFisheye = new Planar<>(GrayF32.class, 1, 1, 3);
	Planar<GrayF32> imageRendered = new Planar<>(GrayF32.class, 1, 1, 3);
	BufferedImage bufferedRendered;

	CameraPinhole pinholeModel = new CameraPinhole(200, 200, 0, 200, 200, 400, 400);

	// selected center point for undistorted view
	double pixelX, pixelY;

	public DisplayFisheyeCalibrationPanel() {
		MouseAdapter adapter = new MouseAdapter() {
			@Override
			public void mouseDragged( MouseEvent e ) {
				changeFocus(e);
			}

			private void changeFocus( MouseEvent e ) {
				double omniX = e.getX()/scale;
				double omniY = e.getY()/scale;

				if (!imageFisheye.isInBounds((int)omniX, (int)omniY))
					return;
				setPinholeCenter(omniX, omniY);
				renderPinhole();
				repaint();
			}
		};
		panel.addMouseMotionListener(adapter);

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				// don't navigate using clicks when undistorting
				if (showUndistorted)
					return;
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
		super.setBufferedImageNoChange(image);
		ConvertBufferedImage.convertFrom(image, imageFisheye, true);

		if (imageFisheye.getNumBands() != imageRendered.getNumBands()) {
			imageRendered = imageFisheye.createNew(1, 1);
		}
		renderPinhole();
	}

	public void setCalibration( LensDistortionWideFOV fisheyeDistort, int width, int height ) {
		BoofSwingUtil.checkGuiThread();
		this.fisheyeDistort = fisheyeDistort;

		LensDistortionNarrowFOV pinholeDistort = new LensDistortionPinhole(pinholeModel);
		distorter = new NarrowToWidePtoP_F32(pinholeDistort, fisheyeDistort);

		// Create the image distorter which will render the image
		InterpolatePixel<Planar<GrayF32>> interp = FactoryInterpolation.
				createPixel(0, 255, InterpolationType.BILINEAR, BorderType.ZERO, imageFisheye.getImageType());
		distortImage = FactoryDistort.distort(false, interp, imageFisheye.getImageType());

		// Pass in the transform created above
		distortImage.setModel(new PointToPixelTransform_F32(distorter));

		setPinholeCenter(width/2, height/2);

		renderPinhole();
	}

	public void setPinholeCenter( double pixelX, double pixelY ) {
		this.pixelX = pixelX;
		this.pixelY = pixelY;

		Point3D_F32 norm = new Point3D_F32();
		fisheyeDistort.undistortPtoS_F32().compute((float)pixelX, (float)pixelY, norm);

		Rodrigues_F32 rotation = new Rodrigues_F32();

		Vector3D_F32 canonical = new Vector3D_F32(0, 0, 1);
		rotation.theta = UtilVector3D_F32.acute(new Vector3D_F32(norm), canonical);
		GeometryMath_F32.cross(canonical, norm, rotation.unitAxisRotation);
		rotation.unitAxisRotation.normalize();

		FMatrixRMaj R = ConvertRotation3D_F32.rodriguesToMatrix(rotation, null);
		distorter.setRotationWideToNarrow(R);
		distortImage.setModel(new PointToPixelTransform_F32(distorter));
	}

	private void renderPinhole() {
		if (distortImage == null)
			return;

		imageRendered.reshape(pinholeModel.width, pinholeModel.height);
		distortImage.apply(imageFisheye, imageRendered);

		bufferedRendered = ConvertBufferedImage.checkDeclare(
				imageRendered.width, imageRendered.height, bufferedRendered, BufferedImage.TYPE_INT_RGB);

		ConvertBufferedImage.convertTo(imageRendered, bufferedRendered, true);
	}

	AffineTransform renderTran = new AffineTransform();

	@Override
	protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
		drawFeatures(g2, scale);

		if (!showUndistorted || bufferedRendered == null)
			return;

		// Ensure that it is 1/2 the size of the input
		double scaleSize = (img.getWidth()/2)/(double)bufferedRendered.getWidth();

		double offX = (pixelX - scaleSize*bufferedRendered.getWidth()/2)*scale;
		double offY = (pixelY - scaleSize*bufferedRendered.getHeight()/2)*scale;

		renderTran.setTransform(scale*scaleSize, 0, 0, scale*scaleSize, offX, offY);
		g2.drawImage(bufferedRendered, renderTran, null);
	}

	private void drawFeatures( Graphics2D g2, double scale ) {

		if (results == null)
			return;

		BoofSwingUtil.antialiasing(g2);

		CalibrationObservation set = features;

		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		Point2D_F32 adj = new Point2D_F32();

		if (showOrder) {
			renderOrder(g2, scale, set.points);
		}

		if (showPoints) {
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(3));
			for (PointIndex2D_F64 p : set.points) {
				adj.setTo((float)p.p.x, (float)p.p.y);
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
			}
			g2.setStroke(new BasicStroke(1));
			g2.setColor(Color.RED);
			for (PointIndex2D_F64 p : set.points) {
				adj.setTo((float)p.p.x, (float)p.p.y);
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
			}
		}

		if (showAll) {
			for (CalibrationObservation l : allFeatures) {
				for (PointIndex2D_F64 p : l.points) {
					adj.setTo((float)p.p.x, (float)p.p.y);
					VisualizeFeatures.drawPoint(g2, adj.x*scale, adj.y*scale, 3, Color.BLUE, Color.WHITE, ellipse);
				}
			}
		}

		if (showNumbers) {
			drawNumbers(g2, set.points, null, scale);
		}

		if (showErrors) {
			g2.setStroke(new BasicStroke(4));
			g2.setColor(Color.BLACK);
			for (int i = 0; i < set.size(); i++) {
				PointIndex2D_F64 p = set.get(i);
				adj.setTo((float)p.p.x, (float)p.p.y);

				double r = errorScale*results.pointError[i];
				if (r < 1)
					continue;

				VisualizeFeatures.drawCircle(g2, adj.x*scale, adj.y*scale, r, ellipse);
			}

			g2.setStroke(new BasicStroke(2.5f));
			g2.setColor(Color.ORANGE);
			for (int i = 0; i < set.size(); i++) {
				PointIndex2D_F64 p = set.get(i);
				adj.setTo((float)p.p.x, (float)p.p.y);

				double r = errorScale*results.pointError[i];
				if (r < 1)
					continue;


				VisualizeFeatures.drawCircle(g2, adj.x*scale, adj.y*scale, r);
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

	@Override public void clearCalibration() {
		fisheyeDistort = null;
		distorter = null;
	}
}
