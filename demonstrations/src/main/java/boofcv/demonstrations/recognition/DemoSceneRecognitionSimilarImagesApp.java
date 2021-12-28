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
import boofcv.demonstrations.reconstruction.ScrollableJPanel;
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
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.FactoryFilterLambdas;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;

import static boofcv.io.UtilIO.systemToUnix;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

/**
 * Visualizes similar images using scene recognition. Shows images features, words, and allows interaction.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DemoSceneRecognitionSimilarImagesApp<Gray extends ImageGray<Gray>, TD extends TupleDesc<TD>>
		extends DemonstrationBase {

	// TODO Add a tuning panel
	// TODO Tune input image size
	// TODO select features in one of the similar images
	// TODO Load full resolution image for the selected target
	// TODO progress bar + status when doing math
	// TODO save / load DB
	// TODO Use prebuilt vocab

	public static final int PREVIEW_PIXELS = 500*400;
	public static final int INPUT_PIXELS = 640*480;

	VisualizePanel gui = new VisualizePanel();
	ViewControlPanel viewControlPanel = new ViewControlPanel();

	Class<Gray> grayType;
	ImageType<Planar<Gray>> colorType;

	SimilarImagesSceneRecognition<Gray, TD> dbSimilar;

	// List of which images are similar to each other
	final Object imageLock = new Object();
	Map<String, SimilarInfo> imagesSimilar = new HashMap<>();
	List<String> imagePaths = new ArrayList<>();
	DogArray<ImageDimension> imageShapes = new DogArray<>(ImageDimension::new);
	Map<String, BufferedImage> imagePreviews = new HashMap<>();

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
//		types.add(BoofSwingUtil.FileTypes.VIDEOS);
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
		List<String> foundFiles = new ArrayList<>();

		// See if it's a list of files
		try {
			foundFiles = UtilIO.listAll(file.getPath());
		} catch (RuntimeException ignore) {}

		if (foundFiles.isEmpty() && UtilImageIO.isImage(file)) {
			// A bit silly, but include just this one image if the idiot user tells it to
			foundFiles = new ArrayList<>();
			foundFiles.add(file.getPath());
		}

		if (foundFiles.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Failed to load images");
			return;
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
		List<String> _foundFiles = foundFiles;
		threadPool.execute(() -> {
			setMenuBarEnabled(false);
			computingFinished = false;
			try {
				processImageFiles(_foundFiles);
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
		// Sort to ensure the order is constant
		Collections.sort(foundFiles);

		System.out.println("Processing images.size=" + foundFiles.size());
		// clean up
		synchronized (imageLock) {
			imagePaths.clear();
			imagePreviews.clear();
			imagesSimilar.clear();
			imageShapes.reset();
		}
		SwingUtilities.invokeLater(() -> viewControlPanel.resetImagePaths());

		var config = new ConfigSimilarImagesSceneRecognition();
//		config.features.detectFastHessian.extract.radius = 2;
		config.limitMatchesConsider = 15;
		config.minimumSimilar.setRelative(0.3, 100);
		config.recognizeNister2006.learningMinimumPointsForChildren.setRelative(0.001, 100);
//		config.associate.greedy.scoreRatioThreshold = 0.9;
//		config.recognizeNister2006.minimumDepthFromRoot = 1;
//		config.recognizeNister2006.featureSingleWordHops = 5;
//		config.recognizeNister2006.learnNodeWeights = false;

		dbSimilar = FactorySceneReconstruction.createSimilarImages(config, ImageType.single(grayType));
//		sceneSimilar.setVerbose(System.out, null);

		Gray gray = GeneralizedImageOps.createImage(grayType, 1, 1, 0);
		BoofLambdas.Transform<Gray> transform =
				FactoryFilterLambdas.createDownSampleFilter(INPUT_PIXELS, gray.getImageType());

		System.out.print("Adding images: ");
		long time0 = System.currentTimeMillis();
		for (String path : foundFiles) {
			BufferedImage buffered = UtilImageIO.loadImage(path);

			// There can be non images inside an image directory
			if (buffered == null) {
				System.err.println("Failed to load as image: " + path);
				continue;
			}

			// create the preview image
			double scale = Math.sqrt(PREVIEW_PIXELS)/Math.sqrt(buffered.getWidth()*buffered.getHeight());
			scale = Math.min(scale, 1.0);
			BufferedImage preview = new BufferedImage(
					(int)(buffered.getWidth()*scale), (int)(buffered.getHeight()*scale), BufferedImage.TYPE_INT_RGB);
			preview.createGraphics().drawImage(buffered, AffineTransform.getScaleInstance(scale, scale), null);
			synchronized (imageLock) {
				imagePreviews.put(path, preview);
				imageShapes.grow().setTo(buffered.getWidth(), buffered.getHeight());
			}
			SwingUtilities.invokeLater(() -> viewControlPanel.addImageToPath(path));

			// Convert to gray for image processing
			ConvertBufferedImage.convertFrom(buffered, gray, true);
			dbSimilar.addImage(path, transform.process(gray));
		}
		long time1 = System.currentTimeMillis();
		System.out.println((time1 - time0) + " (ms)");

		System.out.print("Fixating: ");
		dbSimilar.fixate();
		long time2 = System.currentTimeMillis();
		System.out.println((time2 - time1) + " (ms)");

		SwingUtilities.invokeLater(() -> viewControlPanel.setProcessingTimeS((time2 - time0)*1e-3));
	}

	/**
	 * Computes statistics about the selected image
	 */
	private String createInfoText( int imageIndex ) {
		if (!computingFinished)
			return "";

		// Look up all the words in the image
		DogArray_I32 words = new DogArray_I32();
		dbSimilar.lookupImageWords(imagePaths.get(imageIndex), words);

		// Find any features appear in each word
		TIntIntMap wordHistogram = new TIntIntHashMap();
		words.forEach(word -> wordHistogram.put(word, wordHistogram.get(word) + 1));

		// Find number of features in the most common word
		int mostFeaturesInAWord = 0;
		for (int key : wordHistogram.keys()) {
			mostFeaturesInAWord = Math.max(wordHistogram.get(key), mostFeaturesInAWord);
		}

		String text = "";
		text += String.format("Similar Images: %d\n", imagesSimilar.size());
		text += String.format("Total Features: %d\n", words.size());
		text += String.format("Total Words: %d\n", wordHistogram.size());
		text += String.format("Mean Feats/Word: %.1f\n", words.size()/(double)wordHistogram.size());
		text += String.format("Max Feats/Word: %d\n", mostFeaturesInAWord);

		return text;
	}

	class ViewControlPanel extends StandardAlgConfigPanel implements ListSelectionListener {
		public boolean drawFeatures = false;
		public ColorFeatures colorization = ColorFeatures.ASSOCIATION;
		public int numPreviewColumns = 4;

		protected JLabel buildTimeLabel = new JLabel("-----");
		protected JLabel queryTimeLabel = new JLabel("-----");
		protected JLabel imageSizeLabel = new JLabel("-----");
		protected JLabel dbSizeLabel = new JLabel("-----");
		protected JLabel totalImagesLabel = new JLabel("-----");

		JList<String> listImages;

		JCheckBox checkFeatures = checkbox("Features", drawFeatures);
		JComboBox<String> comboColor = combo(colorization.ordinal(), (Object[])ColorFeatures.values());
		JSpinner spinnerNumColumns = spinner(numPreviewColumns, 1, 10, 1);

		public ViewControlPanel() {
			listImages = new JList<>();
			listImages.setModel(new DefaultListModel<>());
			listImages.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			listImages.setLayoutOrientation(JList.VERTICAL);
			listImages.setVisibleRowCount(-1);
			listImages.addListSelectionListener(this);
			var listScrollPane = new JScrollPane(listImages) {
				// Forces it to fill the entire window
				@Override public Dimension getPreferredSize() {
					return ViewControlPanel.this.getSize();
				}
			};

			addLabeled(buildTimeLabel, "Build (s)");
			addLabeled(queryTimeLabel, "Query (ms)");
			addLabeled(imageSizeLabel, "Original Size");
			addLabeled(dbSizeLabel, "DB Size");
			addAlignLeft(checkFeatures);
			addLabeled(comboColor, "Color");
			addLabeled(spinnerNumColumns, "Preview Columns");
			addLabeled(totalImagesLabel, "Total Images");
			addAlignCenter(listScrollPane);
			setPreferredSize(new Dimension(250, 500));
		}

		public void resetImagePaths() {
			BoofSwingUtil.checkGuiThread();
			synchronized (imageLock) {
				imagePaths.clear();
			}

			this.listImages.removeListSelectionListener(this);
			DefaultListModel<String> model = (DefaultListModel<String>)listImages.getModel();
			model.clear();
			listImages.invalidate();
			listImages.repaint();
			this.listImages.addListSelectionListener(this);
		}

		public void addImageToPath( String path ) {
			BoofSwingUtil.checkGuiThread();

			int size;
			synchronized (imageLock) {
				imagePaths.add(path);
				size = imagePaths.size();
			}

			this.listImages.removeListSelectionListener(this);
			DefaultListModel<String> model = (DefaultListModel<String>)listImages.getModel();
			model.addElement(new File(path).getName());

			if (listImages.getSelectedIndex() >= size)
				listImages.setSelectedIndex(0);
			listImages.invalidate();
			listImages.repaint();
			this.listImages.addListSelectionListener(this);
			setTotalImages(size);
		}

		public void setImageSize( final int width, final int height ) {
			imageSizeLabel.setText(width + " x " + height);
		}

		public void setDbSize( final int width, final int height ) {
			dbSizeLabel.setText(width + " x " + height);
		}

		public void setProcessingTimeS( double seconds ) {
			buildTimeLabel.setText(String.format("%7.1f", (seconds)));
		}

		public void setQueryTimeMS( double milliseconds ) {
			queryTimeLabel.setText(String.format("%7.2f", milliseconds));
		}

		public void setTotalImages( int count ) {
			totalImagesLabel.setText("" + count);
		}

		@Override public void valueChanged( ListSelectionEvent e ) {
			int selected = listImages.getSelectedIndex();
			if (selected != -1 && !e.getValueIsAdjusting()) {
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
			} else if (source == spinnerNumColumns) {
				numPreviewColumns = (Integer)spinnerNumColumns.getValue();
				gui.updateGridShape();
			}
		}
	}

	class VisualizePanel extends JPanel {
		final VisualizeImage mainImage = new VisualizeImage(null);

		final JTextArea textArea = new JTextArea();

		final GridLayout gridLayout = new GridLayout(0, 4, 4, 4);
		final ScrollableJPanel gridPanel = new ScrollableJPanel(gridLayout);

		// Selected index of a feature in the "source" image
		int selectedSrcID = -1;
		// The word of the feature in the selected feature
		int selectedWord = -1;

		JScrollPane gridScrollPanel;

		public VisualizePanel() {
			setLayout(new BorderLayout());
			mainImage.requestFocus();

			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			textArea.setMinimumSize(new Dimension(0, 0));

			var mainPanelSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textArea, mainImage);
			mainPanelSplit.setDividerLocation(200);

			gridScrollPanel = new JScrollPane(gridPanel, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER);

			var verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanelSplit, gridScrollPanel);
			verticalSplit.setDividerLocation(300);
			verticalSplit.setPreferredSize(new Dimension(200, 0));

			add(verticalSplit, BorderLayout.CENTER);
		}

		public void updateGridShape() {
			gridLayout.setColumns(viewControlPanel.numPreviewColumns);
			this.gridPanel.invalidate();
			this.gridPanel.validate();
			this.gridPanel.repaint();
			this.gridScrollPanel.invalidate();
			this.gridScrollPanel.validate();
			this.gridScrollPanel.repaint();
		}

		public void handleSelectedChanged() {
			BoofSwingUtil.checkGuiThread();

			// Invalidate previous feature selection, but keep the word so we can watch it
			selectedSrcID = -1;

			int selectedIndex = viewControlPanel.listImages.getSelectedIndex();

			// Nothing is selected so remove any images being displayed
			if (selectedIndex < 0) {
				mainImage.changeImage(null);
				gridPanel.removeAll();
				gridPanel.validate();
				return;
			}

			String imageID = imagePaths.get(selectedIndex);

			mainImage.changeImage(imageID);

			// Update the image shape
			synchronized (imageLock) {
				ImageDimension imageShape = imageShapes.get(selectedIndex);
				viewControlPanel.setImageSize(imageShape.width, imageShape.height);
				mainImage.dbShape.setTo(imageShape);
			}

			// Wait until it has finished before trying to visualize the results
			if (!computingFinished) {
				gridPanel.removeAll();
				gridPanel.validate();
				return;
			}

			lookupSimilarImages(imageID);

			viewControlPanel.setDbSize(mainImage.dbShape.width, mainImage.dbShape.height);

			textArea.setText(createInfoText(selectedIndex));

			gridPanel.removeAll();

			// Sort the similar images from best to worst based on association
			List<SimilarInfo> sortedList = new ArrayList<>();
			for (String viewID : imagesSimilar.keySet()) {
				sortedList.add(imagesSimilar.get(viewID));
			}
			Collections.sort(sortedList, ( a, b ) -> Integer.compare(b.associated.size, a.associated.size));
			for (SimilarInfo match : sortedList) {
				gridPanel.add(new ImageLabeledPanel(match.id, match.associated.size));
			}

			// Tell the GUI to update
			gridPanel.validate();
			gridPanel.repaint();
		}

		// TODO Move outside of GUI to avoid any sort of slow down
		private void lookupSimilarImages( String imageID ) {
			List<String> foundIDs = new ArrayList<>();
			imagesSimilar.clear();
			long time0 = System.nanoTime();
			dbSimilar.findSimilar(imageID, null, foundIDs);
			long time1 = System.nanoTime();

			viewControlPanel.setQueryTimeMS((time1 - time0)*1e-6);
			System.out.printf("query took %.3f (ms) found=%d\n", (time1 - time0)*1e-6, foundIDs.size());

			// Save indexes of similar images
			var info = new DogArray<>(SimilarInfo::new);
			for (String s : foundIDs) {
				SimilarInfo similar = info.grow();
				similar.id = s;
				if (!dbSimilar.lookupAssociated(s, similar.associated))
					System.out.println("BUG! lookupAssociated failed");
				imagesSimilar.put(s, similar);
			}
		}
	}

	/**
	 * Draws the image and the image's name below it.
	 */
	class ImageLabeledPanel extends JSpringPanel {
		VisualizeImage image;
		int heightOfLabels;

		public ImageLabeledPanel( String imageID, int count ) {
			var labelID = new JLabel(new File(imageID).getName());
			var labelScore = new JLabel("Match: " + count);

			// Take in account the text when scaling the images so that it's still visible even when small
			heightOfLabels = labelID.getPreferredSize().height + labelScore.getPreferredSize().height + 10;

			image = new VisualizeImage(imageID);
			image.setPreferredSize(image.getImageDimension());

			add(image);
			add(labelID);
			add(labelScore);

			constrainWestNorthEast(image, null, 0, 0);
			constrainWestSouthEast(labelScore, null, 2, 4);
			constrainWestSouthEast(labelID, labelScore, 2, 4);
			layout.putConstraint(SpringLayout.SOUTH, image, 0, SpringLayout.NORTH, labelID);
		}

		@Override public Dimension getPreferredSize() {
			if (this.image == null)
				return super.getPreferredSize();

			BufferedImage img = this.image.getImage();
			if (img == null)
				return super.getPreferredSize();

			Dimension s = this.getSize();

			double scale = Math.min(img.getWidth(), s.getWidth())/img.getWidth();
			int width = (int)(scale*img.getWidth() + 0.5);
			int height = (int)(scale*img.getHeight() + 0.5) + heightOfLabels;

			return new Dimension(width, height);
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	class VisualizeImage extends ImagePanel {
		ImageDimension dbShape = new ImageDimension();
		DogArray<Point2D_F64> features = new DogArray<>(Point2D_F64::new);
		DogArray_I32 words = new DogArray_I32();
		DogArray_I32 mainFeatureIdx = new DogArray_I32();

		@Nullable String imageID;

		MouseSelectImageFeatures featureHandler = new MouseSelectImageFeatures(this);

		public VisualizeImage( @Nullable String imageID ) {
			super(300, 300);
			setScaling(ScaleOptions.DOWN);
			if (imageID != null)
				changeImage(imageID);

			addMouseListener(featureHandler);
			addMouseMotionListener(featureHandler);

			featureHandler.featureLocation = ( idx, p ) -> p.setTo(features.get(idx));
			featureHandler.screenToImage = ( x, y, image ) -> {
				double scale = getImageScale();
				image.setTo(x/scale, y/scale);
			};
			featureHandler.imageToScreen = ( x, y, screen ) -> {
				double scale = getImageScale();
				screen.setTo(x*scale, y*scale);
			};

			featureHandler.featureSkip = ( idx ) -> {
				boolean filterWords = viewControlPanel.colorization == ColorFeatures.WORD && gui.selectedWord != -1;
				int word = words.get(idx);
				if (filterWords && gui.selectedWord != word)
					return true;

				// Skip over features which are not associated
				if (viewControlPanel.colorization == ColorFeatures.ASSOCIATION) {
					int featureIdx = mainFeatureIdx.get(idx);

					// Skip if there's no corresponding feature in the main view
					if (featureIdx < 0)
						return true;

					// Skip if the user has selected features and this is not one of the selected
					if (gui.mainImage.featureHandler.featuresSelected()) {
						return !gui.mainImage.featureHandler.selectedMask.get(featureIdx);
					}
				}

				return false;
			};
			featureHandler.featureColor = ( idx ) ->
					switch (viewControlPanel.colorization) {
						case ALL -> 0xFF0000;
						case ASSOCIATION -> {
							// If only one feature is selected make sure it's easy to see
							if (gui.mainImage.featureHandler.isSingleSelected()) {
								yield 0xFF9999;
							}
							yield VisualizeFeatures.trackIdToRgb(mainFeatureIdx.get(idx));
						}
						case WORD -> {
							int word = words.get(idx);
							yield VisualizeFeatures.trackIdToRgb(word*100L + (word%100));
						}
					};

			if (imageID == null) {
				// if the user selects features in the master panel repaint everything
				featureHandler.handleSelected = () -> {
					switch (viewControlPanel.colorization) {
						case WORD -> {
							// Select the word based on the selected feature
							if (featureHandler.selected.isEmpty())
								gui.selectedWord = -1;
							else
								gui.selectedWord = words.get(featureHandler.selected.get(0));
							featureHandler.selected.reset();
							gui.repaint();
						}
						default -> {
							// make sure no word is selected then repaint everything
							gui.selectedWord = -1;
							gui.repaint();
						}
					}
				};
			} else {
				// Only the master image can the user select features
				featureHandler.selectRegion = false;
			}
		}

		public void changeImage( @Nullable String imageID ) {
			BoofSwingUtil.checkGuiThread();
			features.reset();
			this.imageID = imageID;
			featureHandler.reset();
			if (imageID == null) {
				setImageRepaint(null);
				return;
			}
			synchronized (imageLock) {
				setImageRepaint(imagePreviews.get(imageID));
			}
		}

		public double getImageScale() {
			Objects.requireNonNull(img);
			double previewScale = img.getWidth()/(double)dbShape.width;
			return previewScale*scale;
		}

		@Override public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			if (imageID == null || !computingFinished || !viewControlPanel.drawFeatures || img == null)
				return;

			if (features.isEmpty()) {
				dbSimilar.lookupPixelFeats(imageID, features);
				dbSimilar.lookupImageWords(imageID, words);
				featureHandler.numFeatures = features.size;

				// look up corresponding features in the mainImage
				mainFeatureIdx.resetResize(words.size, -1);
				SimilarInfo info = imagesSimilar.get(imageID);
				if (info != null) {
					info.associated.forEach(p -> mainFeatureIdx.set(p.dst, p.src));
				} else {
					// This must be displaying the selected image and not one of the similar ones
					// so every feature is a main feature
					for (int i = 0; i < words.size; i++) {
						mainFeatureIdx.set(i, i);
					}
				}
			}
			featureHandler.paint(BoofSwingUtil.antialiasing(g));
		}
	}

	/**
	 * Stores information on a similar image to the query image
	 */
	static class SimilarInfo implements Comparable<SimilarInfo> {
		String id; // image ID
		DogArray<AssociatedIndex> associated = new DogArray<>(AssociatedIndex::new);

		@Override public int compareTo( SimilarInfo o ) {
			return Integer.compare(o.associated.size, associated.size);
		}
	}

	enum ColorFeatures {
		ASSOCIATION, WORD, ALL
	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("recognition/scene"));

		SwingUtilities.invokeLater(() -> {
			var app = new DemoSceneRecognitionSimilarImagesApp<>(examples, GrayU8.class);
			app.openExample(examples.get(0));
			app.displayImmediate("FeatureSceneRecognition Demo");
		});
	}
}
