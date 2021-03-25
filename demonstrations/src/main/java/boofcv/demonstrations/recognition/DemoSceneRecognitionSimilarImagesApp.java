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

package boofcv.demonstrations.recognition;

import boofcv.alg.scene.SceneRecognitionSimilarImages;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static boofcv.io.UtilIO.systemToUnix;

/**
 * @author Peter Abeles
 */
public class DemoSceneRecognitionSimilarImagesApp<Gray extends ImageGray<Gray>, TD extends TupleDesc<TD>>
		extends DemonstrationBase {

	VisualizePanel gui = new VisualizePanel();
	ControlPanel controlPanel = new ControlPanel();

	Class<Gray> grayType;
	ImageType<Planar<Gray>> colorType;

	protected DemoSceneRecognitionSimilarImagesApp( List<?> exampleInputs, Class<Gray> grayType ) {
		super(true, false, exampleInputs);
		this.grayType = grayType;
		this.colorType = ImageType.pl(3, grayType);

		gui.setPreferredSize(new Dimension(800, 800));

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, gui);
	}

	// Not used as this has it's own pipeline
	@Override public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {}

	/**
	 * Specialized open file which makes a list of files
	 */
	@Override public void openFile( File file, boolean addToRecent ) {
		List<String> foundFiles;

		if (file.isDirectory()) {
			// If a directory open up all images in the directory
			foundFiles = UtilIO.listSmartImages(file.getPath(), true);
		} else if (UtilImageIO.isImage(file)) {
			// A bit silly, but include just this one image if the idiot user tells it to
			foundFiles = new ArrayList<>();
			foundFiles.add(file.getPath());
		} else {
			throw new RuntimeException("Doesn't support videos yet");
		}

		if (foundFiles.isEmpty()) {
			throw new RuntimeException("No files selected");
		}

		// Save the path just incase it wants to reload it later
		inputFilePath = systemToUnix(file.getPath());

		// update recent items menu
		if (addToRecent) {
			String path = inputFilePath;
			BoofSwingUtil.invokeNowOrLater(() -> {
				BoofSwingUtil.addToRecentFiles(DemoSceneRecognitionSimilarImagesApp.this,
						selectRecentFileName(BoofMiscOps.asList(file)), BoofMiscOps.asList(path));
				updateRecentItems();
			});
		}

		processImageFiles(foundFiles);
	}

	/**
	 *
	 */
	void processImageFiles( List<String> foundFiles ) {
		SceneRecognitionSimilarImages<Gray,TD> alg;
	}

	class ControlPanel extends StandardAlgConfigPanel {
		protected JLabel processingTimeLabel = new JLabel();
		protected JLabel imageSizeLabel = new JLabel();

		public void setImageSize( final int width , final int height ) {
			BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width+" x "+height));
		}

		public void setProcessingTimeS( double seconds ) {
			processingTimeLabel.setText(String.format("%7.1f",(seconds*1000)));
		}
	}

	class VisualizePanel extends JPanel {

	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();

		SwingUtilities.invokeLater(() -> {
			var app = new DemoSceneRecognitionSimilarImagesApp<>(examples, GrayU8.class);
			app.openExample(examples.get(0));
			app.display("FeatureSceneRecognition Demo");
		});
	}
}
