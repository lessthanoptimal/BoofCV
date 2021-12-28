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
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Displays information images of planar calibration targets.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DisplayPinholeCalibrationPanel extends DisplayCalibrationPanel {

	// which image is being displayed
	int selectedImage;
	// for displaying undistorted image
	@Nullable BufferedImage distorted;
	BufferedImage undistorted;

	// for displaying corrected image
	Planar<GrayF32> origMS = new Planar<>(GrayF32.class, 1, 1, 3);
	Planar<GrayF32> correctedMS = new Planar<>(GrayF32.class, 1, 1, 3);

	@Nullable ImageDistort<GrayF32, GrayF32> undoRadial;

	// int horizontal line
	int lineY = -1;

	// Transform that does nothing. Used when viewing distorted pixels
	protected Point2Transform2_F32 doNothing = new DoNothing2Transform2_F32();
	// Transform that removes lens distortion
	protected Point2Transform2_F32 remove_p_to_p = doNothing;

	public DisplayPinholeCalibrationPanel() {
		// navigate using left mouse clicks
		panel.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed( MouseEvent e ) {
				panel.requestFocus();
				if (!SwingUtilities.isLeftMouseButton(e))
					return;
				Point2D_F64 p = pixelToPoint(e.getX(), e.getY());
				centerView(p.x, p.y);
			}
		});
	}

	@Override public synchronized void setBufferedImageNoChange( @Nullable BufferedImage image ) {
		this.distorted = image;
		undoRadialDistortion(distorted);
	}

	@Override public void paintComponent( Graphics g ) {
		if (showUndistorted) {
			this.img = undistorted;
			super.pixelTransform = remove_p_to_p;
		} else {
			this.img = distorted;
			this.pixelTransform = doNothing;
		}

		super.paintComponent(g);
	}

	@Override protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
		if (observation != null && observation.size() > selectedImage) {
			drawFeatures(g2, scale);
		}

		if (lineY > -1) {
			g2.setColor(Color.RED);
			g2.setStroke(new BasicStroke(3));
			g2.drawLine(0, lineY, getWidth(), lineY);
		}
	}

	/**
	 * Recomputes the rectified image from the current distorted image and tells the panel to repaint
	 */
	public void recomputeRectification() {
		undoRadialDistortion(distorted);
		repaint();
	}

	private void undoRadialDistortion( @Nullable BufferedImage image ) {
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

	@Override public void clearCalibration() {
		undoRadial = null;
	}
}
