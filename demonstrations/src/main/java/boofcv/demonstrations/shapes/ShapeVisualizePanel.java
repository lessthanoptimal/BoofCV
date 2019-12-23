/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.gui.image.ImageZoomPanel;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ShapeVisualizePanel extends ImageZoomPanel {
	public ShapeVisualizePanel() {
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				panel.requestFocus();
				if( SwingUtilities.isLeftMouseButton(e)) {
					Point2D_F64 p = pixelToPoint(e.getX(), e.getY());
					centerView(p.x,p.y);
				}
			}
		});

		setWheelScrollingEnabled(false);
		panel.addMouseWheelListener(e->{
			setScale(BoofSwingUtil.mouseWheelImageZoom(scale,e));
		});
	}
}
