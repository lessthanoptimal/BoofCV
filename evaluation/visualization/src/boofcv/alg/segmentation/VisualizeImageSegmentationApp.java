/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation;

import boofcv.abst.segmentation.ImageSegmentation;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.segmentation.ConfigSlic;
import boofcv.factory.segmentation.FactoryImageSegmentation;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.*;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

/**
 * Visualization for {@link ImageSegmentation}.
 *
 * @author Peter Abeles
 */
// TODO Add please be patient dialog during calculation
// TODO Selectable parameters for each type of segmentation
public class VisualizeImageSegmentationApp <T extends ImageBase>
		extends SelectAlgorithmAndInputPanel
{
	ImageType<T> imageType;

	BufferedImage inputImage;
	BufferedImage outColor = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
	BufferedImage outSegments = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
	BufferedImage outBorder = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);

	ImageSegmentation<T> alg = null;

	int activeDisplay = 0;

	T color;
	ImageSInt32 pixelToRegion = new ImageSInt32(1,1);

	SegmentConfigPanel leftPanel;
	ImagePanel gui = new ImagePanel();

	boolean processImage = false;

	public VisualizeImageSegmentationApp(ImageType<T> imageType ) {
		super(1);
		this.imageType = imageType;


		addAlgorithm(0, "FH04",0);
		addAlgorithm(0, "SLIC Superpixel", 1);
		addAlgorithm(0, "Mean-Shift", 2);

		color = imageType.createImage(1,1);

		declareAlgorithm(0);

		JPanel viewArea = new JPanel(new BorderLayout());
		leftPanel = new SegmentConfigPanel(this);
		viewArea.add(leftPanel, BorderLayout.WEST);
		viewArea.add(gui, BorderLayout.CENTER);

		setMainGUI(viewArea);
	}

	public void process(BufferedImage input) {
		setInputImage(input);
		this.inputImage = input;

		color.reshape(input.getWidth(),input.getHeight());
		pixelToRegion.reshape(color.width, color.height);
		ConvertBufferedImage.convertFrom(input, color,true);

		if( input.getWidth() != outColor.getWidth() || input.getHeight() != outColor.getHeight()) {
			outColor = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);
			outSegments = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);
			outBorder = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);
		}

		gui.setPreferredSize(new Dimension(color.getWidth(), color.getHeight()));
		updateActiveDisplay(activeDisplay);
		doRefreshAll();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				revalidate();
				processImage = true;
			}
		});
	}

	@Override
	public void loadConfigurationFile(String fileName) {
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0, null, cookies[0]);
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if (inputImage == null)
			return;

		declareAlgorithm((Integer)cookie);
		performSegmentation();

		gui.repaint();
	}

	private void declareAlgorithm(int which) {
		alg = null;
		switch( which ) {
			case 0: alg = FactoryImageSegmentation.fh04(null, imageType); break;
			case 1: alg = FactoryImageSegmentation.slic(new ConfigSlic(800), imageType); break;
			case 2: alg = FactoryImageSegmentation.meanShift(null, imageType); break;
			default: throw new RuntimeException("BUG!");
		}
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if (image != null) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processImage;
	}

	public void updateActiveDisplay( int value ) {
		this.activeDisplay = value;
		if( activeDisplay == 0 ) {
			gui.setBufferedImage(outColor);
		} else if( activeDisplay == 1 ) {
			gui.setBufferedImage(outBorder);
		} else if( activeDisplay == 2 ) {
			gui.setBufferedImage(outSegments);
		}
		gui.repaint();
	}

	private void performSegmentation() {
		alg.segment(color, pixelToRegion);
		int numSegments = alg.getTotalSegments();

		// Computes the mean color inside each region
		ImageType<T> type = color.getImageType();
		ComputeRegionMeanColor<T> colorize = FactorySegmentationAlg.regionMeanColor(type);

		FastQueue<float[]> segmentColor = new ColorQueue_F32(type.getNumBands());
		segmentColor.resize(numSegments);

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.resize(numSegments);

		ImageSegmentationOps.countRegionPixels(pixelToRegion, numSegments, regionMemberCount.data);
		colorize.process(color,pixelToRegion,regionMemberCount,segmentColor);

		// Select random colors for each region
		Random rand = new Random(234);
		int colors[] = new int[ segmentColor.size ];
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt();
		}

		// Assign colors to each pixel depending on their region for mean color image and random segment color image
		for( int y = 0; y < color.height; y++ ) {
			for( int x = 0; x < color.width; x++ ) {
				int index = pixelToRegion.unsafe_get(x,y);
				float []cv = segmentColor.get(index);

				int r,g,b;

				if( cv.length == 3 ) {
					r = (int)cv[0];
					g = (int)cv[1];
					b = (int)cv[2];
				} else {
					r = g = b = (int)cv[0];
				}

				int rgb = r << 16 | g << 8 | b;

				outColor.setRGB(x, y, rgb);
				outSegments.setRGB(x, y, colors[index]);
			}
		}

		// Make edges appear black
		ConvertBufferedImage.convertTo(color,outBorder,true);
		ImageUInt8 binary = new ImageUInt8(pixelToRegion.width,pixelToRegion.height);
		ImageSegmentationOps.markRegionBorders(pixelToRegion,binary);
		for( int y = 0; y < binary.height; y++ ) {
			for( int x = 0; x < binary.width; x++ ) {
				if( binary.unsafe_get(x,y) == 1 )  {
					outBorder.setRGB(x,y,0);
				}
			}
		}
	}

	public static void main(String[] args) {
		ImageType<MultiSpectral<ImageFloat32>> imageType = ImageType.ms(3,ImageFloat32.class);
//		ImageType<MultiSpectral<ImageUInt8>> imageType = ImageType.ms(3,ImageUInt8.class);
//		ImageType<ImageFloat32> imageType = ImageType.single(ImageFloat32.class);
//		ImageType<ImageUInt8> imageType = ImageType.single(ImageUInt8.class);

		VisualizeImageSegmentationApp app = new VisualizeImageSegmentationApp(imageType);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("sunflowers", "../data/evaluation/sunflowers.png"));
		inputs.add(new PathLabel("shapes", "../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("amoeba","../data/evaluation/amoeba_shapes.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Image Segmentation");
	}
}
