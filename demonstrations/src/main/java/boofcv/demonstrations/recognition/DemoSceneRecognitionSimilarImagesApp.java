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

import boofcv.alg.similar.ConfigSimilarImagesSceneRecognition;
import boofcv.alg.similar.SimilarImagesSceneRecognition;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.dialogs.JSpringPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static boofcv.io.UtilIO.systemToUnix;

/**
 * Visualizes similar images using scene recognition. Shows images features, words, and allows interaction.
 *
 * @author Peter Abeles
 */
public class DemoSceneRecognitionSimilarImagesApp<Gray extends ImageGray<Gray>, TD extends TupleDesc<TD>>
		extends DemonstrationBase {

	// TODO Info panel next to image with useful statistics
	// TODO Add a tuning panel
	// TODO Load full resolution image for the selected target
	// TODO progress bar + status when doing math
	// TODO save / load DB
	// TODO Use prebuilt vocab

	public static final int PREVIEW_PIXELS = 400*300;

	VisualizePanel gui = new VisualizePanel();
	ViewControlPanel viewControlPanel = new ViewControlPanel();

	Class<Gray> grayType;
	ImageType<Planar<Gray>> colorType;

	SimilarImagesSceneRecognition<Gray, TD> sceneSimilar;

	// List of which images are similar to each other
	final Object imageLock = new Object();
	DogArray<DogArray_I32> imagesSimilar = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);
	DogArray<DogArray_I32> imagesSimilarCounts = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);
	DogArray<DogArray_I32> imagesWords = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);
	List<String> imagePaths = new ArrayList<>();
	List<BufferedImage> imagePreviews = new ArrayList<>();

	// If true it's done doing all computations
	boolean computingFinished;

	protected DemoSceneRecognitionSimilarImagesApp( List<?> exampleInputs, Class<Gray> grayType ) {
		super(true, false, exampleInputs);
		this.grayType = grayType;
		this.colorType = ImageType.pl(3, grayType);

		gui.setPreferredSize(new Dimension(800, 800));

		add(BorderLayout.WEST, viewControlPanel);
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
			imagesSimilarCounts.reset();
			imagesWords.reset();
		}
		SwingUtilities.invokeLater(() -> viewControlPanel.updateImagePaths());

		var config = new ConfigSimilarImagesSceneRecognition();
		config.features.detectFastHessian.extract.radius = 5;
		config.features.detectFastHessian.maxFeaturesAll = 500;
		config.limitMatchesConsider = 15;
		config.minimumSimilar.setRelative(0.3, 100);
		config.recognizeNister2006.learningMinimumPointsForChildren.setRelative(0.001, 100);
//		config.associate.greedy.scoreRatioThreshold = 0.9;
		config.recognizeNister2006.minimumDepthFromRoot = 1;
		config.recognizeNister2006.featureSingleWordHops = 5;
//		config.recognizeNister2006.learnNodeWeights = false;

		sceneSimilar = FactorySceneReconstruction.createSimilarImages(config, ImageType.single(grayType));
		sceneSimilar.setVerbose(System.out, null);

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
			SwingUtilities.invokeLater(() -> viewControlPanel.updateImagePaths());

			// Convert to gray for image processing
			ConvertBufferedImage.convertFrom(buffered, gray, true);
			sceneSimilar.addImage(path, gray);
		}
		long time1 = System.currentTimeMillis();
		System.out.println((time1 - time0) + " (ms)");

		System.out.print("Fixating: ");
		sceneSimilar.fixate();
		long time2 = System.currentTimeMillis();
		System.out.println((time2 - time1) + " (ms)");

		System.out.print("Finding Similar: ");
		imagesSimilar.reset();
		imagesSimilarCounts.reset();
		imagesWords.reset();
		var info = new DogArray<>(SimilarInfo::new);
		var pairs = new DogArray<>(AssociatedIndex::new);
		for (String path : foundFiles) {
			List<String> foundIDs = new ArrayList<>();
			sceneSimilar.findSimilar(path, foundIDs);

			// Save indexes of similar images
			info.reset();
			for (String s : foundIDs) {
				sceneSimilar.lookupMatches(path, s, pairs);
				info.grow().setTo(s, pairs.size());
			}
			// Sort it best first
			Collections.sort(info.toList());

			// Save the results
			DogArray_I32 similar = imagesSimilar.grow();
			DogArray_I32 matchCount = imagesSimilarCounts.grow();
			info.forEach(n -> {
				similar.add(foundFiles.indexOf(n.id));
				matchCount.add(n.matches);
			});

			// Save which words appeared in the images
			sceneSimilar.lookupImageWords(path, imagesWords.grow());
		}
		long time3 = System.currentTimeMillis();
		System.out.println((time3 - time2) + " (ms)");

		SwingUtilities.invokeLater(() -> viewControlPanel.setProcessingTimeS((time3 - time0)*1e-3));
	}

	/**
	 * Computes statistics about the selected image
	 */
	private String createInfoText( int imageIndex ) {
		if (!computingFinished)
			return "";

		// Find any features appear in each word
		TIntIntMap wordHistogram = new TIntIntHashMap();
		DogArray_I32 words = imagesWords.get(imageIndex);
		words.forEach(word -> wordHistogram.put(word, wordHistogram.get(word) + 1));

		// Find number of features in the most common word
		int mostFeaturesInAWord = 0;
		for (int key : wordHistogram.keys()) {
			mostFeaturesInAWord = Math.max(wordHistogram.get(key), mostFeaturesInAWord);
		}

		String text = "";
		text += String.format("Similar Images: %d\n", imagesSimilar.get(imageIndex).size);
		text += String.format("Total Features: %d\n", words.size());
		text += String.format("Total Words: %d\n", wordHistogram.size());
		text += String.format("Mean Feats/Word: %.1f\n", words.size()/(double)wordHistogram.size());
		text += String.format("Max Feats/Word: %d\n", mostFeaturesInAWord);

		return text;
	}

	class ViewControlPanel extends StandardAlgConfigPanel implements ListSelectionListener {
		public boolean drawFeatures = false;
		public ColorFeatures colorization = ColorFeatures.ASSOCIATION;

		protected JLabel processingTimeLabel = new JLabel("-----");
		protected JLabel imageSizeLabel = new JLabel("-----");

		JList<String> listImages;

		JCheckBox checkFeatures = checkbox("Features", drawFeatures);
		JComboBox<String> comboColor = combo(colorization.ordinal(), (Object[])ColorFeatures.values());

		public ViewControlPanel() {
			listImages = new JList<>();
			listImages.setModel(new DefaultListModel<>());
			listImages.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			listImages.setLayoutOrientation(JList.VERTICAL);
			listImages.setVisibleRowCount(-1);
			listImages.addListSelectionListener(this);
			var listScrollPane = new JScrollPane(listImages) {
				// Forces it to fill the entire window
				public Dimension getPreferredSize() {
					return ViewControlPanel.this.getSize();
				}
			};

			addLabeled(processingTimeLabel, "Time (s)");
			addLabeled(imageSizeLabel, "Image Size");
			addAlignLeft(checkFeatures);
			addLabeled(comboColor, "Color");
			addAlignCenter(listScrollPane);
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

		@Override public void controlChanged( Object source ) {
			if (source == checkFeatures) {
				drawFeatures = checkFeatures.isSelected();
				gui.repaint();
			} else if (source == comboColor) {
				colorization = ColorFeatures.values()[comboColor.getSelectedIndex()];
				gui.repaint();
			}
		}
	}

	class VisualizePanel extends JPanel {
		final VisualizeImage mainImage = new VisualizeImage(-1);

		final JTextArea textArea = new JTextArea();

		final JPanel gridPanel = new JPanel(new GridLayout(0, 4, 4, 4));

		// Selected index of a feature in the "source" image
		int selectedSrcID = -1;
		// The word of the feature in the selected feature
		int selectedWord = -1;

		public VisualizePanel() {
			setLayout(new BorderLayout());

			mainImage.addMouseListener(new MouseAdapter() {
				@Override public void mousePressed( MouseEvent e ) {
					if (mainImage.features.isEmpty())
						return;
					double scale = mainImage.getImageScale();
					double x = e.getX()/scale;
					double y = e.getY()/scale;

					int best = -1;
					double bestDistance = Math.pow(15.0/scale, 2.0);
					for (int i = 0; i < mainImage.features.size; i++) {
						Point2D_F64 pixel = mainImage.features.get(i);
						double d = pixel.distance2(x, y);
						if (d < bestDistance) {
							bestDistance = d;
							best = i;
						}
					}

					if (best == -1) {
						// Only redraw if it deselected
						if (selectedSrcID != best) {
							selectedSrcID = -1;
							selectedWord = -1;
							gui.repaint();
						}
						return;
					}
					selectedSrcID = best;
					selectedWord = mainImage.words.get(best);

					gui.repaint();
				}
			});
			mainImage.requestFocus();

			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			textArea.setMinimumSize(new Dimension(0, 0));

			JSplitPane mainPanelSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textArea, mainImage);
			mainPanelSplit.setDividerLocation(200);

			JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanelSplit, gridPanel);
			verticalSplit.setDividerLocation(300);
			verticalSplit.setPreferredSize(new Dimension(200, 0));

			add(verticalSplit, BorderLayout.CENTER);
		}

		public void handleSelectedChanged() {
			BoofSwingUtil.checkGuiThread();

			// Invalidate previous feature selection, but keep the word so we can watch it
			selectedSrcID = -1;

			int selectedIndex = viewControlPanel.listImages.getSelectedIndex();
			mainImage.changeImage(selectedIndex);

			// Nothing is selected so remove any images being displayed
			if (selectedIndex < 0) {
				mainImage.setImageRepaint(null);
				gridPanel.removeAll();
				gridPanel.validate();
				return;
			}

			// Update the image shape
			ImageDimension shape = mainImage.shape;
			viewControlPanel.setImageSize(shape.width, shape.height);

			// Wait until it has finished before trying to visualize the results
			if (!computingFinished) {
				gridPanel.removeAll();
				gridPanel.validate();
				return;
			}

			textArea.setText(createInfoText(selectedIndex));

			DogArray_I32 matchIds = imagesSimilar.get(selectedIndex);
			DogArray_I32 matchCounts = imagesSimilarCounts.get(selectedIndex);

			gridPanel.removeAll();
			matchIds.forIdx(( idx, imageIndex ) ->
					gridPanel.add(new ImageLabeledPanel(imageIndex, matchCounts.get(idx))));
			gridPanel.validate();
			gridPanel.repaint();
		}
	}

	/**
	 * Draws the image and the image's name below it.
	 */
	class ImageLabeledPanel extends JSpringPanel {
		public ImageLabeledPanel( int imageIndex, int count ) {
			var labelID = new JLabel(new File(imagePaths.get(imageIndex)).getName());
			var labelScore = new JLabel("Match: " + count);

			// Take in account the text when scaling the images so that it's still visible even when small
			int heightOfLabels = labelID.getPreferredSize().height + labelScore.getPreferredSize().height + 4;

			VisualizeImage image = new VisualizeImage(imageIndex) {
				// All of this sizing crap is to keep the box tight around the image
				@Override public Dimension getPreferredSize() {
					if (img == null)
						return super.getPreferredSize();

					Dimension s = ImageLabeledPanel.this.getSize();
					double adjustedHeight = Math.max(0.0, s.getHeight() - heightOfLabels);

					double scale = Math.min(s.getWidth()/img.getWidth(), adjustedHeight/img.getHeight());
					if (scale > 1.0)
						scale = 1.0;
					int width = (int)(scale*img.getWidth() + 0.5);
					int height = (int)(scale*img.getHeight() + 0.5);
					return new Dimension(width, height);
				}
			};

			// This component will stretch out, but there's nothing in it
			JPanel glue = new JPanel();

			add(image);
			add(labelID);
			add(labelScore);
			add(glue);

			constrainWestNorthEast(image, null, 0, 0);
			constrainWestNorthEast(labelID, image, 2, 4);
			constrainWestNorthEast(labelScore, labelID, 2, 4);
			constrainWestNorthEast(glue, labelScore, 0, 0);
			layout.putConstraint(SpringLayout.SOUTH, glue, 0, SpringLayout.SOUTH, this);
		}
	}

	class VisualizeImage extends ImagePanel {
		ImageDimension shape = new ImageDimension();
		DogArray<Point2D_F64> features = new DogArray<>(Point2D_F64::new);
		DogArray_I32 words = new DogArray_I32();
		DogArray_I32 mainFeatureIdx = new DogArray_I32();

		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		String imageID;

		public VisualizeImage( int imageIndex ) {
			super(300, 300);
			setScaling(ScaleOptions.DOWN);
			if (imageIndex >= 0)
				changeImage(imageIndex);
		}

		public void changeImage( int imageIndex ) {
			BoofSwingUtil.checkGuiThread();
			features.reset();
			synchronized (imageLock) {
				if (imageIndex < 0 || imageIndex >= imagePreviews.size()) {
					imageID = null;
					setImageRepaint(null);
					return;
				}
				setImageRepaint(imagePreviews.get(imageIndex));
				imageID = imagePaths.get(imageIndex);
			}
		}

		public double getImageScale() {
			double previewScale = img.getWidth()/(double)shape.width;
			return previewScale*scale;
		}

		@Override public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			if (imageID == null || !computingFinished || !viewControlPanel.drawFeatures || img == null)
				return;

			if (features.isEmpty()) {
				sceneSimilar.lookupPixelFeats(imageID, features);
				sceneSimilar.lookupShape(imageID, shape);
				int imageIndex = imagePaths.indexOf(imageID);
				words.setTo(imagesWords.get(imageIndex));

				// look up corresponding features in the mainImage
				mainFeatureIdx.resize(words.size, -1);
				DogArray<AssociatedIndex> pairs = new DogArray<>(AssociatedIndex::new);
				sceneSimilar.lookupMatches(gui.mainImage.imageID, imageID, pairs);
				pairs.forEach(p -> mainFeatureIdx.set(p.dst, p.src));
			}

			double imageScale = getImageScale();

			Graphics2D g2 = BoofSwingUtil.antialiasing(g);

			// Filter by words if a word has been selected and it's colorizing by words
			boolean filterWords = viewControlPanel.colorization == ColorFeatures.WORD && gui.selectedWord != -1;

			// Filter by feature if showing all features and a feature in the source has been selected
			boolean filterFeature = viewControlPanel.colorization == ColorFeatures.ASSOCIATION && gui.selectedSrcID != -1;

			for (int i = 0; i < features.size; i++) {
				int word = words.get(i);
				Point2D_F64 p = features.get(i);

				if (filterWords && gui.selectedWord != word)
					continue;

				if (filterFeature && gui.selectedSrcID != mainFeatureIdx.get(i))
					continue;

				// Skip over features which are not associated
				if (viewControlPanel.colorization == ColorFeatures.ASSOCIATION && mainFeatureIdx.get(i) < 0)
					continue;

				Color color = switch (viewControlPanel.colorization) {
					case ALL -> Color.RED;
					case ASSOCIATION -> new Color(VisualizeFeatures.trackIdToRgb(mainFeatureIdx.get(i)));
					case WORD -> new Color(VisualizeFeatures.trackIdToRgb(word*100L + (word%100)));
				};

				VisualizeFeatures.drawPoint(g2,
						offsetX + p.x*imageScale, offsetY + p.y*imageScale, 5.0, color, true, ellipse);
			}
		}
	}

	/**
	 * Stores information on a similar image to the query image
	 */
	static class SimilarInfo implements Comparable<SimilarInfo> {
		String id; // image ID
		int matches; // number of features which were matched

		public void setTo( String id, int matches ) {
			this.id = id;
			this.matches = matches;
		}

		@Override public int compareTo( SimilarInfo o ) {
			return Integer.compare(o.matches, matches);
		}
	}

	enum ColorFeatures {
		ASSOCIATION, WORD, ALL
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
