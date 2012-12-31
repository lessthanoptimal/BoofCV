/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.wavelet;

import boofcv.abst.wavelet.WaveletTransform;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.border.BorderType;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.factory.transform.wavelet.GFactoryWavelet;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


/**
 * @author Peter Abeles
 */
public class WaveletVisualizeApp
		<T extends ImageSingleBand, W extends ImageSingleBand, C extends WlCoef>
		extends SelectAlgorithmAndInputPanel
{
	int numLevels = 3;

	T image;
	T imageInv;

	Class<T> imageType;

	ListDisplayPanel panel = new ListDisplayPanel();
	boolean processedImage = false;

	public WaveletVisualizeApp(Class<T> imageType ) {
		super(1);
		this.imageType = imageType;

		addWaveletDesc("Haar",GFactoryWavelet.haar(imageType));
		addWaveletDesc("Daub 4", GFactoryWavelet.daubJ(imageType,4));
		addWaveletDesc("Bi-orthogonal 5",GFactoryWavelet.biorthogoal(imageType,5, BorderType.REFLECT));
		addWaveletDesc("Coiflet 6",GFactoryWavelet.coiflet(imageType,6));

		setMainGUI(panel);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);
		
		image = ConvertBufferedImage.convertFromSingle(input, null, imageType);
		imageInv = (T)image._createNew(image.width,image.height);

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
		setActiveAlgorithm(0,null,cookies[0]);
	}

	private void addWaveletDesc( String name , WaveletDescription desc )
	{
		if( desc != null )
			addAlgorithm(0, name,desc);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( image == null )
			return;

		WaveletDescription<C> desc = (WaveletDescription<C>)cookie;
		WaveletTransform<T,W,C> waveletTran =
				FactoryWaveletTransform.create((Class)image.getClass(),desc,numLevels,0,255);

		panel.reset();

		W imageWavelet = waveletTran.transform(image,null);

		waveletTran.invert(imageWavelet,imageInv);

		// adjust the values inside the wavelet transform to make it easier to see
		UtilWavelet.adjustForDisplay(imageWavelet, waveletTran.getLevels(), 255);
		BufferedImage buffWavelet = VisualizeImageData.grayMagnitude(imageWavelet,null,255);
		BufferedImage buffInv = ConvertBufferedImage.convertTo(imageInv,null);

		panel.addImage(buffWavelet,"Transform");
		panel.addImage(buffInv,"Inverse");
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
		BufferedImage in = UtilImageIO.loadImage("data/standard/lena512.bmp");
		WaveletVisualizeApp app = new WaveletVisualizeApp(ImageFloat32.class);
//		WaveletVisualizeApp app = new WaveletVisualizeApp(ImageUInt8.class);

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

		ShowImages.showWindow(app,"Wavelet Transforms");
	}
}
