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

package boofcv.alg.color;

import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.ProgressMonitorThread;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Shows individual bands in different color models
 *
 * @author Peter Abeles
 */
public class ShowColorModelApp
	extends SelectAlgorithmAndInputPanel
{
	int active = 0;

	ListDisplayPanel gui = new ListDisplayPanel();
	boolean processedImage = false;

	MultiSpectral<ImageFloat32> input;
	MultiSpectral<ImageFloat32> output;


	// tells progress monitor the current progress
	volatile int progress;

	public ShowColorModelApp() {
		super(1);

		addAlgorithm(0,"RGB",0);
		addAlgorithm(0,"HSV",1);
		addAlgorithm(0,"YUV",2);
		addAlgorithm(0,"XYZ",3);
		addAlgorithm(0,"LAB",4);

		input = new MultiSpectral<ImageFloat32>(ImageFloat32.class, 1, 1 , 3);
		output = new MultiSpectral<ImageFloat32>(ImageFloat32.class, 1, 1 , 3);

		setMainGUI(gui);
	}



	public void process( final BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		output.reshape(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFromMulti(image, input,true, ImageFloat32.class);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setInputImage(image);
				doRefreshAll();
			}});
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0, null, cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		active = (Integer) cookie;

		performUpdate();
	}

	private synchronized void performUpdate() {
		if( input == null || output == null )
			return;

		progress = 0;

		ProgressMonitorThread monitor = new MyMonitor(this,"");
		monitor.start();

		final String names[] = new String[3];
		final BufferedImage out[] = new BufferedImage[3];

		for( int i = 0; i < 3; i++ ) {
			out[i] = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
		}

		switch (active) {
			case 0:
				names[0] = "Red";
				names[1] = "Green";
				names[2] = "Blue";
				output.setTo(input);
				ConvertBufferedImage.convertTo(output.getBand(0),out[0]);
				ConvertBufferedImage.convertTo(output.getBand(1),out[1]);
				ConvertBufferedImage.convertTo(output.getBand(2),out[2]);
				break;

			case 1:
				names[0] = "Hue";
				names[1] = "Saturation";
				names[2] = "Value";
				ColorHsv.rgbToHsv_F32(input, output);
				setNaN(output.getBand(0));
				PixelMath.multiply(output.getBand(0), (float) (255.0 / (2 * Math.PI)), output.getBand(0));
				PixelMath.multiply(output.getBand(1), 255.0f, output.getBand(1));
				ConvertBufferedImage.convertTo(output.getBand(0),out[0]);
				ConvertBufferedImage.convertTo(output.getBand(1),out[1]);
				ConvertBufferedImage.convertTo(output.getBand(2),out[2]);
				break;

			case 2:
				names[0] = "Y";
				names[1] = "U";
				names[2] = "V";
				ColorYuv.rgbToYuv_F32(input, output);
				ConvertBufferedImage.convertTo(output.getBand(0),out[0]);
				VisualizeImageData.colorizeSign(output.getBand(1), out[1], -1);
				VisualizeImageData.colorizeSign(output.getBand(2), out[2], -1);
				break;

			case 3:
				names[0] = "X";
				names[1] = "Y";
				names[2] = "Z";
				ColorXyz.rgbToXyz_F32(input, output);
				PixelMath.multiply(output.getBand(1),255,output.getBand(1));
				VisualizeImageData.colorizeSign(output.getBand(0), out[0], -1);
				ConvertBufferedImage.convertTo(output.getBand(1), out[1]);
				VisualizeImageData.colorizeSign(output.getBand(2), out[2], -1);
				break;

			case 4:
				names[0] = "L";
				names[1] = "A";
				names[2] = "B";
				ColorLab.rgbToLab_F32(input, output);
				VisualizeImageData.grayMagnitude(output.getBand(0), out[0], -1);
				VisualizeImageData.colorizeSign(output.getBand(1), out[1], -1);
				VisualizeImageData.colorizeSign(output.getBand(2), out[2], -1);
				break;
		}
		monitor.stopThread();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.reset();
				for( int i = 0; i < 3; i++ ) {
					gui.addImage(out[i],names[i]);
				}
				gui.setPreferredSize(new Dimension(input.width,input.height));
				gui.repaint();
				processedImage = true;
			}
		});
	}

	private void setNaN( ImageFloat32 image ) {
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				float v = image.unsafe_get(x,y);
				if( Float.isNaN(v) || Float.isInfinite(v) ) {
					image.unsafe_set(x,y,0);
				}
			}
		}
	}

	private class MyMonitor extends ProgressMonitorThread {

		protected MyMonitor( Component comp , String message ) {
			super( new ProgressMonitor(comp,
					"Converting color space",
					message, 0, input.getNumBands()));
		}

		@Override
		public void doRun() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					monitor.setProgress(progress);
				}});
		}
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if (image != null) {
			process(image);
		}
	}

	public static void main(String args[]) {

		ShowColorModelApp app = new ShowColorModelApp();

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("sunflowers","../data/evaluation/sunflowers.png"));
		inputs.add(new PathLabel("beach","../data/evaluation/scale/beach02.jpg"));
		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Color Formats");
	}
}
