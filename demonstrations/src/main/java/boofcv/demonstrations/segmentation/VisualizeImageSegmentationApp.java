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

package boofcv.demonstrations.segmentation;

import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.alg.segmentation.ImageSegmentationOps;
import boofcv.factory.segmentation.FactoryImageSegmentation;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.feature.VisualizeRegions;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Visualization for {@link boofcv.abst.segmentation.ImageSuperpixels}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeImageSegmentationApp<T extends ImageBase<T>> extends DemonstrationBase {
	BufferedImage inputImage;
	BufferedImage outColor = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	BufferedImage outSegments = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	BufferedImage outBorder = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

	ImageSuperpixels<T> alg;

	T color;
	GrayS32 pixelToRegion = new GrayS32(1, 1);

	SegmentConfigPanel controls = new SegmentConfigPanel(this);
	ImagePanel gui = new ImagePanel();

	public VisualizeImageSegmentationApp( java.util.List<PathLabel> examples, ImageType<T> imageType ) {
		super(true, false, examples, imageType);
		allowVideos = false;
		color = imageType.createImage(1, 1);

		add(gui, BorderLayout.CENTER);
		add(controls, BorderLayout.WEST);

		declareAlgorithm();
	}

	public void declareAlgorithm() {
		ImageType<T> imageType = getImageType(0);
		alg = switch (controls.selectedAlgorithm) {
			case 0 -> FactoryImageSegmentation.fh04(controls.configFh, imageType);
			case 1 -> FactoryImageSegmentation.slic(controls.configSlic, imageType);
			case 2 -> FactoryImageSegmentation.meanShift(controls.configMeanShift, imageType);
			case 3 -> FactoryImageSegmentation.watershed(controls.configWatershed, imageType);
			default -> throw new RuntimeException("BUG!");
		};
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		color.reshape(width, height);
		pixelToRegion.reshape(width, height);

		outColor = new BufferedImage(color.width, color.height, BufferedImage.TYPE_INT_RGB);
		outSegments = new BufferedImage(color.width, color.height, BufferedImage.TYPE_INT_RGB);
		outBorder = new BufferedImage(color.width, color.height, BufferedImage.TYPE_INT_RGB);

		handleGuiChange();

		SwingUtilities.invokeLater(() -> {
			gui.setPreferredSize(new Dimension(width, height));
			controls.setImageSize(width, height);
		});
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {
		this.inputImage = buffered;

		color.setTo((T)input);
		pixelToRegion.reshape(color.width, color.height);
		performSegmentation();

		SwingUtilities.invokeLater(() -> {
			gui.setPreferredSize(new Dimension(color.getWidth(), color.getHeight()));
			gui.revalidate();
		});
	}

	public void handleControlsChanged() {
		declareAlgorithm();
		reprocessImageOnly();
	}

	public void handleGuiChange() {
		switch (controls.selectedVisual) {
			case 0:
				gui.setImage(outColor);
				break;
			case 1:
				gui.setImage(outBorder);
				break;
			case 2:
				gui.setImage(outSegments);
				break;
			case 3:
				gui.setImage(inputImage);
				break;
			default:
				throw new RuntimeException("Unknown mode");
		}
		gui.repaint();
	}

	private void performSegmentation() {
		controls.setComputing(true);
		// local reference to segmentation to avoid alg getting changed in another thread
		ImageSuperpixels<T> alg = this.alg;
		if (alg == null)
			return;
		long before = System.nanoTime();
		alg.segment(color, pixelToRegion);
		long after = System.nanoTime();
		controls.setComputing(false);

		SwingUtilities.invokeLater(() -> controls.setProcessingTimeMS((after - before)*1e-6));

		int numSegments = alg.getTotalSuperpixels();

		// Computes the mean color inside each region
		ImageType<T> type = color.getImageType();
		ComputeRegionMeanColor<T> colorize = FactorySegmentationAlg.regionMeanColor(type);

		int numBands = BoofMiscOps.numBands(color);
		DogArray<float[]> segmentColor = new ColorQueue_F32(numBands);
		segmentColor.resize(numSegments);

		DogArray_I32 regionMemberCount = new DogArray_I32();
		regionMemberCount.resize(numSegments);

		ImageSegmentationOps.countRegionPixels(pixelToRegion, numSegments, regionMemberCount.data);
		colorize.process(color, pixelToRegion, regionMemberCount, segmentColor);

		VisualizeRegions.regionsColor(pixelToRegion, segmentColor, outColor);
		VisualizeRegions.regions(pixelToRegion, segmentColor.size(), outSegments);

		// Make edges appear black
		ConvertBufferedImage.convertTo(color, outBorder, true);
		VisualizeRegions.regionBorders(pixelToRegion, 0x000000, outBorder);

		gui.repaint();
	}

	public static void main( String[] args ) {
		ImageType<Planar<GrayF32>> imageType = ImageType.pl(3, GrayF32.class);
//		ImageType<Planar<GrayU8>> defaultType = ImageType.pl(3,GrayU8.class);
//		ImageType<GrayF32> defaultType = ImageType.single(GrayF32.class);
//		ImageType<GrayU8> defaultType = ImageType.single(GrayU8.class);

		java.util.List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Horses", UtilIO.pathExample("segment/berkeley_horses.jpg")));
		examples.add(new PathLabel("Kangaroo", UtilIO.pathExample("segment/berkeley_kangaroo.jpg")));
		examples.add(new PathLabel("Man", UtilIO.pathExample("segment/berkeley_man.jpg")));
		examples.add(new PathLabel("Pines People", UtilIO.pathExample("segment/mountain_pines_people.jpg")));
		examples.add(new PathLabel("Sunflowers", UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("Shapes", UtilIO.pathExample("shapes/shapes01.png")));

		SwingUtilities.invokeLater(() -> {
			VisualizeImageSegmentationApp app = new VisualizeImageSegmentationApp(examples, imageType);

			app.openExample(examples.get(0));
			app.display("Image Segmentation");
		});
	}
}
