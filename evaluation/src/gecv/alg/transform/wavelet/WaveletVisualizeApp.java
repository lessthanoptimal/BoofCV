/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.transform.wavelet;

import gecv.abst.wavelet.WaveletTransform;
import gecv.alg.misc.GPixelMath;
import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.border.BorderType;
import gecv.factory.transform.wavelet.FactoryWaveletTransform;
import gecv.factory.transform.wavelet.GFactoryWavelet;
import gecv.gui.ListDisplayPanel;
import gecv.gui.SelectAlgorithmPanel;
import gecv.gui.image.ShowImages;
import gecv.gui.image.VisualizeImageData;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef;

import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * @author Peter Abeles
 */
public class WaveletVisualizeApp
		<T extends ImageBase, W extends ImageBase, C extends WlCoef>
		extends SelectAlgorithmPanel
{
	int numLevels = 4;

	T image;
	T imageInv;

	Class<T> imageType;
	Class<W> waveletType;

	ListDisplayPanel panel = new ListDisplayPanel();

	public WaveletVisualizeApp(Class<T> imageType, Class<W> waveletType,
							   BufferedImage original ) {
		this.imageType = imageType;
		this.waveletType = waveletType;

		image = ConvertBufferedImage.convertFrom(original,null,imageType);
		imageInv = (T)image._createNew(image.width,image.height);

		addWaveletDesc("Haar",GFactoryWavelet.haar(imageType));
		addWaveletDesc("Daub 4", GFactoryWavelet.daubJ(imageType,4));
		addWaveletDesc("Bi-orthogonal 5",GFactoryWavelet.biorthogoal(imageType,5, BorderType.REFLECT));
		addWaveletDesc("Coiflet 6",GFactoryWavelet.coiflet(imageType,6));

		add(panel, BorderLayout.CENTER);
		setPreferredSize(new Dimension(image.width+50,image.height+20));
	}

	private void addWaveletDesc( String name , WaveletDescription desc )
	{
		if( desc != null )
			addAlgorithm(name,desc);
	}


	@Override
	public void setActiveAlgorithm(String name, Object cookie) {
		WaveletDescription<C> desc = (WaveletDescription<C>)cookie;
		WaveletTransform<T,W,C> waveletTran = FactoryWaveletTransform.create(desc,numLevels);

		panel.reset();

		W imageWavelet = waveletTran.transform(image,null);

		waveletTran.invert(imageWavelet,imageInv);

		float maxValue = (float)GPixelMath.maxAbs(imageWavelet);
		BufferedImage buffWavelet = VisualizeImageData.grayMagnitude(imageWavelet,null,maxValue);
		BufferedImage buffImage = ConvertBufferedImage.convertTo(image,null);
		BufferedImage buffInv = ConvertBufferedImage.convertTo(imageInv,null);

		panel.addImage(buffWavelet,"Transform");
		panel.addImage(buffImage,"Original");
		panel.addImage(buffInv,"Inverse");
	}

	public static void main( String args[] ) {
		BufferedImage in = UtilImageIO.loadImage("evaluation/data/standard/lena512.bmp");
		WaveletVisualizeApp app = new WaveletVisualizeApp(ImageFloat32.class,ImageFloat32.class,in);
//		WaveletVisualizeApp app = new WaveletVisualizeApp(ImageUInt8.class, ImageSInt32.class,in);

		ShowImages.showWindow(app,"Wavelet Transforms");
	}
}
