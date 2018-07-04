/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.javafx;

import boofcv.visualize.PointCloudViewer;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import javafx.application.Platform;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.util.List;

/**
 * Point cloud viewer which uses JavaFX
 */
public class PointCloudViewerFX implements PointCloudViewer {

	PointCloudViewerPanelFX panel = new PointCloudViewerPanelFX();

	public PointCloudViewerFX() {
		Platform.runLater(() -> panel.initialize());
	}

	@Override
	public void setShowAxis(boolean show) {

	}

	@Override
	public void setTranslationStep(double step) {

	}

	@Override
	public void setDotSize(int pixels) {

	}

	@Override
	public void setClipDistance(double distance) {

	}

	@Override
	public void setFog(boolean active) {

	}

	@Override
	public void setBackgroundColor(int rgb) {

	}

	@Override
	public void addCloud(List<Point3D_F64> cloudXyz, int[] colorsRgb) {

	}

	@Override
	public void addCloud(List<Point3D_F64> cloud) {

	}

	@Override
	public void addCloud(GrowQueue_F32 cloudXYZ, GrowQueue_I32 colorRGB) {

	}

	@Override
	public void addPoint(double x, double y, double z, int rgb) {

	}

	@Override
	public void clearPoints() {

	}

	@Override
	public void setCameraHFov(double radians) {
		Platform.runLater(() -> panel.setHorizontalFieldOfView(radians));
	}

	@Override
	public void setCameraToWorld(Se3_F64 cameraToWorld) {
		Platform.runLater(() -> panel.setCameraToWorld(cameraToWorld));
	}

	@Override
	public JComponent getComponent() {
		return panel;
	}
}
