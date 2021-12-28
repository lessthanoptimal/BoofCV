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

package boofcv.demonstrations.shapes;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

@SuppressWarnings({"NullAway.Init"})
public abstract class DetectBlackShapePanel extends StandardAlgConfigPanel {

	protected JSpinner selectZoom;
	protected JLabel processingTimeLabel = new JLabel();
	protected JLabel imageSizeLabel = new JLabel();

	public int selectedView = 0;

	public double zoom = 1;

	public void setZoom( double _zoom ) {
		_zoom = Math.max(MIN_ZOOM, _zoom);
		_zoom = Math.min(MAX_ZOOM, _zoom);
		if (_zoom == zoom)
			return;
		zoom = _zoom;

		BoofSwingUtil.invokeNowOrLater(() -> selectZoom.setValue(zoom));
	}

	public void setImageSize( final int width, final int height ) {
		BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width + " x " + height));
	}

	public void setProcessingTimeS( double seconds ) {
		processingTimeLabel.setText(String.format("%7.1f", (seconds*1000)));
	}

	public void setProcessingTimeMS( double ms ) {
		BoofSwingUtil.checkGuiThread();
		processingTimeLabel.setText(String.format("%7.1f", ms));
	}
}
