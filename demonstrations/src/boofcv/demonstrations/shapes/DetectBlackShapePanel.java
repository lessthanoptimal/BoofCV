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

package boofcv.demonstrations.shapes;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;

public abstract class DetectBlackShapePanel extends StandardAlgConfigPanel {

	JSpinner selectZoom;

	int selectedView = 0;

	protected double zoom = 1;
	protected final double minZoom = 0.01;
	protected final double maxZoom = 50;

	public void setZoom( double zoom ) {
		zoom = Math.max(minZoom,zoom);
		zoom = Math.min(maxZoom,zoom);
		this.zoom = zoom;

		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				selectZoom.setValue(DetectBlackShapePanel.this.zoom);
			}
		});
	}
}
