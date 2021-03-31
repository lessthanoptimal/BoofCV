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

package boofcv.gui;

import boofcv.gui.controls.ControlPanelPointCloud;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point3D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * For displaying point clouds with controls
 *
 * @author Peter Abeles
 */
public class PointCloudViewerPanel extends JPanel {

	// Control panel for adjusting the visualization
	private final @Getter ControlPanelPointCloud controls = new ControlPanelPointCloud(this::handleControlChange);
	// Where the point cloud is rendered
	private final @Getter PointCloudViewer viewer = VisualizeData.createPointCloudViewer();

	// Basic unit of distance for colorization period
	public double periodBaseline = 1.0;
	// Basic unit of distance for translation control
	public double translateBaseline = 1.0;

	public PointCloudViewerPanel() {
		super(new BorderLayout());
		handleControlChange();

		add(controls,BorderLayout.WEST);
		add(viewer.getComponent(), BorderLayout.CENTER);
	}

	public void clearPoints() {
		BoofSwingUtil.checkGuiThread();

		viewer.clearPoints();
	}

	/**
	 * Selects reasonable values for the baseline distances for translation and visualize period.
	 */
	public void selectPeriods( List<Point3D_F64> cloud ) {
		Point3D_F64 mean = new Point3D_F64();
		UtilPoint3D_F64.mean(cloud,mean);
		double total = 0.0;
		for (int i = 0; i < cloud.size(); i++) {
			total += mean.distance(cloud.get(i));
		}
		total /= cloud.size();
		translateBaseline = total*0.005;
		periodBaseline = total*0.05;
	}

	public void addCloud64(List<Point3D_F64> cloud , @Nullable DogArray_I32 colorRGB )
	{
		BoofSwingUtil.checkGuiThread();

		if( colorRGB == null ) {
			viewer.addCloud(cloud);
		} else {
			assert(cloud.size() == colorRGB.size);
			viewer.addCloud(cloud, colorRGB.data);
		}
	}

	public void handleControlChange() {
		controls.configure(viewer,periodBaseline,translateBaseline);
		viewer.getComponent().repaint();
	}
}
