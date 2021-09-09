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

import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImageZoomPanel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Peter Abeles
 */
public abstract class DisplayCalibrationPanel extends ImageZoomPanel {

	// configures what is displayed or not
	boolean showPoints = true;
	boolean showErrors = true;
	boolean showUndistorted = false;
	boolean showAll = false;
	boolean showNumbers = true;
	boolean showOrder = true;
	double errorScale;

	// observed feature locations
	@Nullable CalibrationObservation features = null;
	// results of calibration
	@Nullable ImageResults results = null;
	@Nullable List<CalibrationObservation> allFeatures;

	public SetScale setScale = (s)->{};

	public DisplayCalibrationPanel() {
		panel.addMouseWheelListener(e -> setScale(BoofSwingUtil.mouseWheelImageZoom(scale, e)));
	}

	public void setResults(CalibrationObservation features , ImageResults results ,
						   List<CalibrationObservation> allFeatures ) {
		BoofSwingUtil.checkGuiThread();

		this.features = features;
		this.results = results;
		this.allFeatures = allFeatures;
	}

	public void clearResults() {
		BoofSwingUtil.checkGuiThread();

		features = null;
		results = null;
		allFeatures = null;
	}

	public void setDisplay( boolean showPoints , boolean showErrors ,
							boolean showUndistorted , boolean showAll , boolean showNumbers ,
							boolean showOrder,
							double errorScale )
	{
		this.showPoints = showPoints;
		this.showErrors = showErrors;
		this.showUndistorted = showUndistorted;
		this.showAll = showAll;
		this.showNumbers = showNumbers;
		this.showOrder = showOrder;
		this.errorScale = errorScale;
	}

	@Override public synchronized void setScale( double scale ) {
		// Avoid endless loops by making sure it's changing
		if (this.scale == scale)
			return;
		super.setScale(scale);
		setScale.setScale(scale);
	}

	/**
	 * Forgets the previously passed in calibration
	 */
	public abstract void clearCalibration();

	@FunctionalInterface public interface SetScale {
		void setScale( double scale );
	}
}
