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

package boofcv.alg.transform.fft;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageFloat;
import boofcv.struct.image.ImageInterleaved;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


/**
 * @author Peter Abeles
 */
public class FourierVisualizeApp
		<T extends ImageFloat, W extends ImageInterleaved>
		extends SelectInputPanel
{
	DiscreteFourierTransform<T,W> fft;

	T image;
	W transform;
	T magnitude;
	T phase;

	ImageDataType imageType;

	ListDisplayPanel panel = new ListDisplayPanel();
	boolean processedImage = false;

	public FourierVisualizeApp(ImageDataType imageType) {
		this.imageType = imageType;

		image = GeneralizedImageOps.createSingleBand(imageType,1,1);
		transform = GeneralizedImageOps.createInterleaved(imageType, 1, 1, 2);
		magnitude = GeneralizedImageOps.createSingleBand(imageType,1,1);
		phase = GeneralizedImageOps.createSingleBand(imageType,1,1);
		fft = GDiscreteFourierTransformOps.createTransform(imageType);

		setMainGUI(panel);
	}

	public FourierVisualizeApp( Class<T> imageType ) {
		this(ImageDataType.classToType(imageType));
	}

	public void process( BufferedImage input ) {
		setInputImage(input);

		image.reshape(input.getWidth(),input.getHeight());
		transform.reshape(input.getWidth(),input.getHeight());
		magnitude.reshape(input.getWidth(),input.getHeight());
		phase.reshape(input.getWidth(),input.getHeight());

		ConvertBufferedImage.convertFrom(input, image, true);
		fft.forward(image,transform);

		GDiscreteFourierTransformOps.shiftZeroFrequency(transform,true);

		GDiscreteFourierTransformOps.magnitude(transform, magnitude);
		GDiscreteFourierTransformOps.phase(transform, phase);

		// Convert it to a log scale for visibility
		GPixelMath.log(magnitude, magnitude);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(image.width+50,image.height+20));
				processedImage = true;
			}});

		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		if( image == null )
			return;

		panel.reset();

		BufferedImage buffMag = VisualizeImageData.grayMagnitude(magnitude,null,-1);
		BufferedImage buffPhase = VisualizeImageData.colorizeSign(phase,null,Math.PI);

		panel.addImage(inputImage,"Original");
		panel.addImage(buffMag,"Magnitude");
		panel.addImage(buffPhase,"Phase");
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main( String args[] ) {
		FourierVisualizeApp app = new FourierVisualizeApp(ImageDataType.F32);
//		FourierVisualizeApp app = new FourierVisualizeApp(ImageTypeInfo.F64);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("lena","../data/evaluation/standard/lena512.bmp"));
		inputs.add(new PathLabel("boat","../data/evaluation/standard/boat.png"));
		inputs.add(new PathLabel("fingerprint","../data/evaluation/standard/fingerprint.png"));
		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("sunflowers","../data/evaluation/sunflowers.png"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Discrete Fourier Transform");
	}
}
