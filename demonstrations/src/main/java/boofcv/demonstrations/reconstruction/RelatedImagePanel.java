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

import boofcv.demonstrations.recognition.MouseSelectImageFeatures;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.dialogs.JSpringPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

/**
 * For showing which images are related to each other.
 *
 * @author Peter Abeles
 */
public class RelatedImagePanel extends JPanel {

	public static final int PREVIEW_PIXELS = 500*400;

	@Getter @Setter boolean useCustomColor = false;

	final JTextArea textArea = new JTextArea();

	final GridLayout gridLayout = new GridLayout(0, 4, 4, 4);
	final ScrollableJPanel gridPanel = new ScrollableJPanel(gridLayout);

	VisualizeImage mainImage = new VisualizeImage();
	DogArray<ImageLabeledPanel> relatedImages = new DogArray<ImageLabeledPanel>(ImageLabeledPanel::new);

	JScrollPane gridScrollPanel;

	public int numPreviewColumns = 3;

	public RelatedImagePanel() {
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
		gridLayout.setColumns(numPreviewColumns);
		this.gridPanel.invalidate();
		this.gridPanel.validate();
		this.gridPanel.repaint();
		this.gridScrollPanel.invalidate();
		this.gridScrollPanel.validate();
		this.gridScrollPanel.repaint();
	}

	public void clear() {
		BoofSwingUtil.checkGuiThread();
		gridPanel.removeAll();
		gridPanel.validate();

		mainImage.clear();
		relatedImages.reset();
	}

	public void setMainImage( String label, BufferedImage image, List<Point2D_F64> features ) {
		BoofSwingUtil.checkGuiThread();

		mainImage.setImage(label, scaleToPreview(image),
				new ImageDimension(image.getWidth(), image.getHeight()), features);
	}

	public void setMainText( String text ) {
		BoofSwingUtil.checkGuiThread();
		textArea.setText(text);
	}

	public void addRelatedImage( String label, BufferedImage image, List<Point2D_F64> features ) {
		BoofSwingUtil.checkGuiThread();
	}

	public void setRelatedText( String label, String text ) {
		BoofSwingUtil.checkGuiThread();
	}

	public void revalidateRelated() {
		gridPanel.validate();
		gridPanel.repaint();
	}

	public void setAssociations( String label, List<AssociatedIndex> indexes, boolean mainIsSrc ) {
		BoofSwingUtil.checkGuiThread();
	}

	BufferedImage scaleToPreview( BufferedImage original ) {
		double scaleFactor = Math.sqrt(original.getWidth()*original.getHeight())/(double)PREVIEW_PIXELS;
		BufferedImage scaled = null;
		if (scaleFactor <= 1.0)
			scaled = original;
		else {
			int width = (int)(original.getWidth()/scaleFactor);
			int height = (int)(original.getHeight()/scaleFactor);

			AffineTransform affine = new AffineTransform();
			affine.setToScale(1.0/scaleFactor, 1.0/scaleFactor);

			scaled = new BufferedImage(width, height, original.getType());
			scaled.createGraphics().drawImage(original, affine, null);
		}
		return scaled;
	}

	/**
	 * Draws the image and the image's name below it.
	 */
	static class ImageLabeledPanel extends JSpringPanel {
		VisualizeImage image = new VisualizeImage();
		int heightOfLabels;

		public ImageLabeledPanel() {
		}

		public void setImage() {
//			var labelID = new JLabel(new File(imageID).getName());
//			var labelScore = new JLabel("Match: " + count);
//
//			// Take in account the text when scaling the images so that it's still visible even when small
//			heightOfLabels = labelID.getPreferredSize().height + labelScore.getPreferredSize().height + 10;
//
//			image.setPreferredSize(new Dimension(image.getImage().getWidth(), image.getImage().getHeight()));
//
//			add(image);
//			add(labelID);
//			add(labelScore);
//
//			constrainWestNorthEast(image, null, 0, 0);
//			constrainWestSouthEast(labelScore, null, 2, 4 );
//			constrainWestSouthEast(labelID, labelScore, 2, 4 );
//			layout.putConstraint(SpringLayout.SOUTH, image, 0, SpringLayout.NORTH, labelID);
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

	static class VisualizeImage extends ImagePanel {
		ImageDimension dbShape = new ImageDimension();
		DogArray<Point2D_F64> features = new DogArray<>(Point2D_F64::new);

		String imageID = "";

		MouseSelectImageFeatures featureHandler = new MouseSelectImageFeatures(this);

		public VisualizeImage() {
			super(300, 300);
			setScaling(ScaleOptions.DOWN);

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
		}

		public void clear() {
			dbShape.setTo(0, 0);
			features.reset();
			imageID = "";
		}

		public void setImage( String id, BufferedImage preview, ImageDimension shape, List<Point2D_F64> features ) {
			this.imageID = id;
			this.dbShape.setTo(shape);
			this.features.reset();
			this.features.copyAll(features, ( src, dst ) -> dst.setTo(src));
			setImage(preview);
		}

		public double getImageScale() {
			Objects.requireNonNull(img);
			double previewScale = img.getWidth()/(double)dbShape.width;
			return previewScale*scale;
		}
	}
}
