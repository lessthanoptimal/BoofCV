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

import boofcv.alg.scene.ConfigSceneRecognitionSimilarImages;
import boofcv.alg.scene.SceneRecognitionSimilarImages;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.geom.AffineTransform;
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

	public static final int PREVIEW_PIXELS = 400*300;

	VisualizePanel gui = new VisualizePanel();
	ControlPanel controlPanel = new ControlPanel();

	Class<Gray> grayType;
	ImageType<Planar<Gray>> colorType;

	// List of which images are similar to each other
	final Object imageLock = new Object();
	DogArray<DogArray_I32> imagesSimilar = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);
	List<String> imagePaths = new ArrayList<>();
	List<BufferedImage> imagePreviews = new ArrayList<>();

	// If true it's done doing all computations
	boolean computingFinished;

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

	@Override protected void openFileMenuBar() {
		List<BoofSwingUtil.FileTypes> types = new ArrayList<>();
		types.add(BoofSwingUtil.FileTypes.IMAGES);
		types.add(BoofSwingUtil.FileTypes.VIDEOS);
		types.add(BoofSwingUtil.FileTypes.DIRECTORIES);
		BoofSwingUtil.FileTypes[] array = types.toArray(new BoofSwingUtil.FileTypes[0]);

		File file = BoofSwingUtil.openFileChooser(DemoSceneRecognitionSimilarImagesApp.this, array);
		if (file != null) {
			openFile(file, true);
		}
	}

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

		stopAllInputProcessing();
		threadPool.execute(() -> {
			setMenuBarEnabled(false);
			computingFinished = false;
			try {
				processImageFiles(foundFiles);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			computingFinished = true;
			setMenuBarEnabled(true);
		});
	}

	/**
	 *
	 */
	void processImageFiles( List<String> foundFiles ) {
		System.out.println("Processing images.size=" + foundFiles.size());
		// clean up
		synchronized (imageLock) {
			imagePaths.clear();
			imagePreviews.clear();
			imagesSimilar.reset();
		}
		SwingUtilities.invokeLater(() -> controlPanel.updateImagePaths());

		var config = new ConfigSceneRecognitionSimilarImages();
		config.features.detectFastHessian.extract.radius = 5;
		config.features.detectFastHessian.maxFeaturesAll = 500;
		config.limitMatchesConsider = 20;
		config.minimumRatio = 0.3;
		config.recognizeNister2006.learningMinimumPointsForChildren.setRelative(0.001, 100);
//		config.associate.greedy.scoreRatioThreshold = 0.9;
		config.recognizeNister2006.minimumDepthFromRoot = 1;
//		config.recognizeNister2006.featureSingleWordHops = 1;
//		config.recognizeNister2006.learnNodeWeights = false;

		SceneRecognitionSimilarImages<Gray, TD> alg =
				FactorySceneRecognition.createSimilarImages(config, ImageType.single(grayType));

		alg.setVerbose(System.out, null);

		Gray gray = GeneralizedImageOps.createImage(grayType, 1, 1, 0);

		System.out.print("Adding images: ");
		long time0 = System.currentTimeMillis();
		for (String path : foundFiles) {
			BufferedImage buffered = UtilImageIO.loadImage(path);

			// create the preview image
			double scale = Math.sqrt(PREVIEW_PIXELS)/Math.sqrt(buffered.getWidth()*buffered.getHeight());
			scale = Math.min(scale, 1.0);
			BufferedImage preview = new BufferedImage(
					(int)(buffered.getWidth()*scale), (int)(buffered.getHeight()*scale), BufferedImage.TYPE_INT_RGB);
			preview.createGraphics().drawImage(buffered, AffineTransform.getScaleInstance(scale, scale), null);
			synchronized (imageLock) {
				imagePaths.add(path);
				imagePreviews.add(preview);
			}
			SwingUtilities.invokeLater(() -> controlPanel.updateImagePaths());

			// Convert to gray for image processing
			ConvertBufferedImage.convertFrom(buffered, gray, true);
			alg.addImage(path, gray);
		}
		long time1 = System.currentTimeMillis();
		System.out.println((time1 - time0) + " (ms)");

		System.out.print("Fixating: ");
		alg.fixate();
		long time2 = System.currentTimeMillis();
		System.out.println((time2 - time1) + " (ms)");

		System.out.print("Finding Similar: ");
		imagesSimilar.resize(0);
		for (String path : foundFiles) {
			List<String> foundIDs = new ArrayList<>();
			alg.findSimilar(path, foundIDs);

			DogArray_I32 similar = imagesSimilar.grow();
			for (String s : foundIDs) {
				similar.add(foundFiles.indexOf(s));
			}
		}
		long time3 = System.currentTimeMillis();
		System.out.println((time3 - time2) + " (ms)");
	}

	class ControlPanel extends StandardAlgConfigPanel implements ListSelectionListener {
		protected JLabel processingTimeLabel = new JLabel();
		protected JLabel imageSizeLabel = new JLabel();

		JList<String> listImages;

		public ControlPanel() {
			listImages = new JList<>();
			listImages.setModel(new DefaultListModel<>());
			listImages.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			listImages.setLayoutOrientation(JList.VERTICAL);
			listImages.setVisibleRowCount(-1);
			listImages.addListSelectionListener(this);

			addLabeled(processingTimeLabel, "Time");
			addLabeled(imageSizeLabel, "Size");
			addAlignCenter(new JScrollPane(listImages));
			setPreferredSize(new Dimension(250, 500));
		}

		public void updateImagePaths() {
			BoofSwingUtil.checkGuiThread();

			// make a quick copy to avoid locking for long
			List<String> copyPaths;
			synchronized (imageLock) {
				// Just add the file name to keep it short
				copyPaths = new ArrayList<>(imagePaths.size());
				for (String path : imagePaths) {
					copyPaths.add(new File(path).getName());
				}
			}

			this.listImages.removeListSelectionListener(this);
			DefaultListModel<String> model = (DefaultListModel<String>)listImages.getModel();
			model.clear();

			model.addAll(copyPaths);

			if (listImages.getSelectedIndex() >= copyPaths.size())
				listImages.setSelectedIndex(0);
			listImages.invalidate();
			listImages.repaint();
			this.listImages.addListSelectionListener(this);
		}

		public void setImageSize( final int width, final int height ) {
			BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width + " x " + height));
		}

		public void setProcessingTimeS( double seconds ) {
			processingTimeLabel.setText(String.format("%7.1f", (seconds*1000)));
		}

		@Override public void valueChanged( ListSelectionEvent e ) {
			int selected = listImages.getSelectedIndex();
			if (selected != -1) {
				gui.handleSelectedChanged();
			}
		}
	}

	class VisualizePanel extends JPanel {
		ImagePanel mainImage = new ImagePanel();

		JPanel gridPanel = new JPanel(new GridLayout(0, 4, 10, 10));

		public VisualizePanel() {
			setLayout(new BorderLayout());
			mainImage.setScaling(ScaleOptions.DOWN);

			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,mainImage, gridPanel);
			splitPane.setDividerLocation(150);
			splitPane.setPreferredSize(new Dimension(200, 0));

			add(splitPane, BorderLayout.CENTER);
		}

		public void handleSelectedChanged() {
			BoofSwingUtil.checkGuiThread();

			int selected = controlPanel.listImages.getSelectedIndex();
			BufferedImage preview = null;
			synchronized (imageLock) {
				if (selected >= 0 && selected < imagePreviews.size()) {
					preview = imagePreviews.get(selected);
				}
			}

			// Nothing is selected so remove any images being displayed
			if (preview == null) {
				mainImage.setImageRepaint(null);
				gridPanel.removeAll();
				gridPanel.validate();
				return;
			}

			mainImage.setImageRepaint(preview);

			// Wait until it has finished before trying to visualize the results
			if (!computingFinished) {
				gridPanel.removeAll();
				gridPanel.validate();
				return;
			}

			DogArray_I32 similar = imagesSimilar.get(selected);
			gridPanel.removeAll();
			similar.forEach(imageIndex -> {
				gridPanel.add(new ImagePanel(imagePreviews.get(imageIndex), ScaleOptions.DOWN));
			});
			gridPanel.validate();
			gridPanel.repaint();
		}
	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();

		SwingUtilities.invokeLater(() -> {
			var app = new DemoSceneRecognitionSimilarImagesApp<>(examples, GrayU8.class);
//			app.openExample(examples.get(0));
			app.displayImmediate("FeatureSceneRecognition Demo");
		});
	}
}
