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

package boofcv.demonstrations.feature.detect.line;

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DemoLineDetectionApp<T extends ImageGray<T>> extends DemonstrationBase {
	Class<T> imageClass;

	DetectLine<T> detector;

	final Object featureLock = new Object();

	public DemoLineDetectionApp(List<?> exampleInputs, Class<T> imageClass ) {
		super(exampleInputs, ImageType.single(imageClass));

		this.imageClass = imageClass;
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {

	}

	void addHoughPolar() {
//		addToAlgorithmList("Hough Polar", FactoryDetectLineAlgs.houghPolar(
//				new ConfigHoughPolar(3, 30, 2, Math.PI / 180, edgeThreshold, maxLines), imageType, derivType));
	}

	private void addToAlgorithmList( String name , DetectLine<T> detector ) {
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			synchronized (featureLock) {
			}
		}
	}

	class ControlPanel extends StandardAlgConfigPanel implements ChangeListener {

		public ControlPanel() {
		}

		@Override
		public void stateChanged(ChangeEvent e) {

		}
	}
}
