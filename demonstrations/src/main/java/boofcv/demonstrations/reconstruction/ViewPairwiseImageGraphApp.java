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

package boofcv.demonstrations.reconstruction;

import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.io.UtilIO;
import boofcv.io.geo.MultiViewIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static boofcv.io.UtilIO.systemToUnix;

/**
 * Used to view a {@link boofcv.alg.structure.PairwiseImageGraph}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ViewPairwiseImageGraphApp extends DemonstrationBase {
	public static final int PREVIEW_PIXELS = 500*400;

	GuiControls controls = new GuiControls();
	RelatedImagePanel gui = new RelatedImagePanel();

	LookUpSimilarImages db;
	PairwiseImageGraph graph;
	String imagePath;
	List<String> imageFiles;

	protected ViewPairwiseImageGraphApp( List<?> exampleInputs ) {
		super(true, false, exampleInputs);

		gui.setPreferredSize(new Dimension(600, 600));

		loadPairwiseIntoGui();
	}

	@Override protected void openFileMenuBar() {
		List<BoofSwingUtil.FileTypes> types = new ArrayList<>();
		types.add(BoofSwingUtil.FileTypes.YAML);
		BoofSwingUtil.FileTypes[] array = types.toArray(new BoofSwingUtil.FileTypes[0]);

		File file = BoofSwingUtil.openFileChooser(ViewPairwiseImageGraphApp.this, array);
		if (file != null) {
			openFile(file, true);
		}
	}

	@Override public void openFile( File file, boolean addToRecent ) {
		// Save the path just in case it wants to reload it later
		inputFilePath = systemToUnix(file.getPath());

		// update recent items menu
		if (addToRecent) {
			String path = inputFilePath;
			BoofSwingUtil.invokeNowOrLater(() -> {
				BoofSwingUtil.addToRecentFiles(ViewPairwiseImageGraphApp.this,
						selectRecentFileName(BoofMiscOps.asList(file)), BoofMiscOps.asList(path));
				updateRecentItems();
			});
		}

		stopAllInputProcessing();
		threadPool.execute(() -> {
			setMenuBarEnabled(false);
			Objects.requireNonNull(inputFilePath);
			System.out.println("Loading pairwise graph at: " + inputFilePath);
			graph = MultiViewIO.load(inputFilePath, (PairwiseImageGraph)null);
			db = MultiViewIO.loadSimilarImages(new File(new File(inputFilePath).getParentFile(),"similar.yaml").getPath());
			smartFindImagePath();
			SwingUtilities.invokeLater(() -> {
				loadPairwiseIntoGui();
				setMenuBarEnabled(true);
			});
		});
	}

	private void changeSelectedView( int viewIdx ) {
		gui.clear();
		if (viewIdx <= -1 || imageFiles == null || viewIdx >= imageFiles.size())
			return;

		PairwiseImageGraph.View pview = graph.nodes.get(viewIdx);

		var features = new DogArray<>(Point2D_F64::new);

		BufferedImage original = UtilIO.load(imageFiles.get(viewIdx));
		db.lookupPixelFeats(pview.id, features);
		gui.setMainImage(pview.id, original, features.toList());
		gui.setMainText(
				"Connections: " + pview.connections.size + "\n" +
				"Features: " + pview.totalObservations + "\n");

		for (int i = 0; i < pview.connections.size; i++) {
			PairwiseImageGraph.Motion m = pview.connections.get(i);
			PairwiseImageGraph.View other = m.other(pview);

			BufferedImage otherImage = UtilIO.load(imageFiles.get(other.index));
			db.lookupPixelFeats(pview.id, features);

			gui.addRelatedImage(other.id, otherImage, features.toList());
			gui.setRelatedText(other.id, "3d=" + m.is3D + " inliers=" + m.inliers.size);
		}
	}

	/**
	 * Sees if it can find the image path by looking at the saved file location
	 */
	protected void smartFindImagePath() {
		Objects.requireNonNull(inputFilePath);

		File parent = new File(inputFilePath).getParentFile();
		if (checkImagePath(parent.getPath()))
			return;

		if (checkImagePath(new File(parent, "images").getPath()))
			return;

		System.out.println("Failed to guess image path");
		imageFiles = new ArrayList<>();
		imagePath = inputFilePath;
	}

	private boolean checkImagePath( String path ) {
		if (!new File(path).exists())
			return false;

		List<String> found = UtilIO.listImages(path, true);
		if (found.size() != graph.nodes.size)
			return false;
		imageFiles = found;
		imagePath = path;
		System.out.println("Using input file path at: " + path);
		return true;
	}

	protected void loadPairwiseIntoGui() {
		controls = new GuiControls();

		removeAll();
		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, gui);
		invalidate();
		validate();
		repaint();
	}

	@Override public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {}

	class GuiControls extends StandardAlgConfigPanel implements ListSelectionListener {
		protected JLabel imageSizeLabel = new JLabel("-----");
		protected JLabel dbSizeLabel = new JLabel("-----");
		protected JLabel totalImagesLabel = new JLabel("-----");

		JList<String> listImages;

		public GuiControls() {
			listImages = new JList<>();
			listImages.setModel(new DefaultListModel<>());
			listImages.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			listImages.setLayoutOrientation(JList.VERTICAL);
			listImages.setVisibleRowCount(-1);
			listImages.addListSelectionListener(this);
			var listScrollPane = new JScrollPane(listImages) {
				// Forces it to fill the entire window
				@Override public Dimension getPreferredSize() {
					return getSize();
				}
			};

			// Add all the nodes to the list
			if (graph != null) {
				DefaultListModel<String> model = (DefaultListModel<String>)listImages.getModel();
				for (int i = 0; i < graph.nodes.size; i++) {
					String id = graph.nodes.get(i).id;
					// truncate the name. Most unique information tends to be at the end
					if (id.length() > 33)
						id = "..." + id.substring(id.length() - 30);
					model.addElement(id);
				}
			}

			addLabeled(imageSizeLabel, "Original Size");
			addLabeled(dbSizeLabel, "DB Size");
			addLabeled(totalImagesLabel, "Total Images");
			addAlignCenter(listScrollPane);
			setPreferredSize(new Dimension(250, 500));
		}

		public void setImageSize( final int width, final int height ) {
			imageSizeLabel.setText(width + " x " + height);
		}

		public void setDbSize( final int width, final int height ) {
			dbSizeLabel.setText(width + " x " + height);
		}

		public void setTotalImages( int count ) {
			totalImagesLabel.setText("" + count);
		}

		@Override public void valueChanged( ListSelectionEvent e ) {
			changeSelectedView(listImages.getSelectedIndex());
		}
	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();

		SwingUtilities.invokeLater(() -> {
			var app = new ViewPairwiseImageGraphApp(examples);
//			app.openExample(examples.get(0));
			app.displayImmediate("Pairwise Image Graph");
		});
	}
}
