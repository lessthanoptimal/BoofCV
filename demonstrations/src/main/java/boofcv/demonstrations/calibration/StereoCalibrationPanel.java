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

package boofcv.demonstrations.calibration;

import boofcv.gui.calibration.DisplayPinholeCalibrationPanel;
import boofcv.struct.calib.CameraPinholeBrown;
import lombok.Getter;
import org.ejml.data.DMatrixRMaj;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

/**
 * Displays stereo calibration data for {@link CalibrateStereoPlanarApp}.
 *
 * @author Peter Abeles
 */
public class StereoCalibrationPanel extends JPanel {

	// draw a horizontal line with this y-value
	public double selectedLineY = -1;

	@Getter ViewPanel panelLeft = new ViewPanel();
	@Getter ViewPanel panelRight = new ViewPanel();

	public StereoCalibrationPanel() {
		super(new GridLayout(0, 2));

		addLineListener(panelLeft);
		addLineListener(panelRight);

		add(panelLeft);
		add(panelRight);
	}

	/**
	 * Listens for mouse clicks and draws a line to see if the rectification is making y-axis align
	 */
	public void addLineListener(ViewPanel panel) {
		panel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override public void mousePressed( MouseEvent e ) {
				if (e.getButton() != MouseEvent.BUTTON1)
					return;

				// If a line is clicked twice you can remove the line
				double coorY = e.getY()/getScale();
				if (Math.abs(selectedLineY-coorY) <= 1.0)
					selectedLineY = -1;
				else
					selectedLineY = coorY;

				panelLeft.repaint();
				panelRight.repaint();
			}
		});
	}

	public void clearVisuals() {
		panelLeft.clearCalibration();
		panelLeft.clearResults();
		panelRight.clearCalibration();
		panelRight.clearResults();
	}

	public void setRectification( CameraPinholeBrown leftParam, DMatrixRMaj leftRect,
								  CameraPinholeBrown rightParam, DMatrixRMaj rightRect ) {
		panelLeft.setCalibration(leftParam, leftRect);
		panelRight.setCalibration(rightParam, rightRect);
	}

	public void recomputeRectification() {
		panelLeft.recomputeRectification();
		panelRight.recomputeRectification();
	}

	public void setShowPoints( boolean state ) {
		panelLeft.showPoints = state;
		panelRight.showPoints = state;
	}

	public void setShowErrors( boolean state ) {
		panelLeft.showErrors = state;
		panelRight.showErrors = state;
	}

	public void setRectify( boolean state ) {
		panelLeft.showUndistorted = state;
		panelRight.showUndistorted = state;
	}

	public void setShowAll( boolean state ) {
		panelLeft.showAll = state;
		panelRight.showAll = state;
	}

	public void setShowNumbers( boolean state ) {
		panelLeft.showNumbers = state;
		panelRight.showNumbers = state;
	}

	public void setShowOrder( boolean state ) {
		panelLeft.showOrder = state;
		panelRight.showOrder = state;
	}

	public void setShowResiduals( boolean state ) {
		panelLeft.showResiduals = state;
		panelRight.showResiduals = state;
	}

	public void setErrorScale( double errorScale ) {
		panelLeft.errorScale = errorScale;
		panelRight.errorScale = errorScale;
	}

	public void setScale( double scale ) {
		panelLeft.setScale(scale);
		panelRight.setScale(scale);
	}

	public double getScale() {
		return panelLeft.getScale();
	}

	/**
	 * Displays the view for one of the cameras
	 */
	public class ViewPanel extends DisplayPinholeCalibrationPanel {
		Line2D.Double line = new Line2D.Double();
		BasicStroke stroke = new BasicStroke(4);

		@Override public void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			super.paintInPanel(tran, g2);
			if (img == null || selectedLineY < 0)
				return;

			g2.setColor(Color.RED);
			g2.setStroke(stroke);
			line.setLine(0, selectedLineY*scale, img.getWidth()*scale, selectedLineY*scale);
			g2.draw(line);
		}
	}
}
