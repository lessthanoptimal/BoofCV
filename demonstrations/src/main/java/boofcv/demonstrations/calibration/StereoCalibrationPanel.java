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

/**
 * @author Peter Abeles
 */
public class StereoCalibrationPanel extends JPanel {

	// draw a horizontal line with this y-value
	public int selectedLineY = -1;

	@Getter ViewPanel panelLeft = new ViewPanel();
	@Getter ViewPanel panelRight = new ViewPanel();

	public StereoCalibrationPanel() {
		super(new GridLayout(0,2));
		add(panelLeft);
		add(panelRight);
	}

	public void setRectification( CameraPinholeBrown leftParam, DMatrixRMaj leftRect,
								  CameraPinholeBrown rightParam, DMatrixRMaj rightRect ) {
		panelLeft.setCalibration(leftParam, leftRect);
		panelRight.setCalibration(rightParam, rightRect);
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
		panelLeft.showNumbers = state;
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

	}
}
