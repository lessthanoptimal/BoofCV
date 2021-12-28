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

package boofcv.demonstrations.tracker;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.ConfigBackgroundGmm;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImageGridPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration of background removal
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeBackgroundModelApp extends DemonstrationBase
		implements BackgroundControlPanel.Listener {
	GrayU8 segmented = new GrayU8(1, 1);
	BufferedImage visualized;
	BufferedImage original;

	private final Object lockBackground = new Object();
	BackgroundModelStationary<GrayU8> background;

	BackgroundControlPanel controls = new BackgroundControlPanel(this);
	ImageGridPanel imagePanels = new ImageGridPanel(1, 2);

	public VisualizeBackgroundModelApp( List<?> exampleInputs ) {
		super(exampleInputs, ImageType.single(GrayU8.class));
		super.allowImages = false;

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, imagePanels);

		imagePanels.setScaleToFit(true);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		synchronized (lockBackground) {
			background.reset();
		}
		imagePanels.setPreferredSize(new Dimension(width*2, height));
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {

		visualized = ConvertBufferedImage.checkDeclare(buffered, visualized);
		original = ConvertBufferedImage.checkCopy(buffered, original);

		segmented.reshape(input.width, input.height);

		final double fps;
		synchronized (lockBackground) {
			long timeBefore = System.nanoTime();
			background.updateBackground((GrayU8)input, segmented);
			long timeAfter = System.nanoTime();
			fps = 1000.0/((timeAfter - timeBefore)*1e-6);
		}

		VisualizeBinaryData.renderBinary(segmented, false, visualized);

		BoofSwingUtil.invokeNowOrLater(() -> {
			imagePanels.setImage(0, 0, visualized);
			imagePanels.setImage(0, 1, original);
			imagePanels.repaint();
			controls.setFPS(fps);
		});
	}

	@Override
	public void modelChanged( ConfigBackgroundGaussian config, boolean stationary ) {
		synchronized (lockBackground) {
			background = FactoryBackgroundModel.stationaryGaussian(config, ImageType.single(GrayU8.class));
		}
	}

	@Override
	public void modelChanged( ConfigBackgroundBasic config, boolean stationary ) {
		synchronized (lockBackground) {
			background = FactoryBackgroundModel.stationaryBasic(config, ImageType.single(GrayU8.class));
		}
	}

	@Override
	public void modelChanged( ConfigBackgroundGmm config, boolean stationary ) {
		synchronized (lockBackground) {
			background = FactoryBackgroundModel.stationaryGmm(config, ImageType.single(GrayU8.class));
		}
	}

	public static void main( String[] args ) {

		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("background/street_intersection.mp4"));
		examples.add(UtilIO.pathExample("background/rubixfire.mp4"));
		examples.add(UtilIO.pathExample("background/horse_jitter.mp4"));
		examples.add(UtilIO.pathExample("tracking/chipmunk.mjpeg"));

		VisualizeBackgroundModelApp app = new VisualizeBackgroundModelApp(examples);

		app.openFile(new File(examples.get(0)));
		app.waitUntilInputSizeIsKnown();
		app.display("Background Model Stationary");
	}
}
