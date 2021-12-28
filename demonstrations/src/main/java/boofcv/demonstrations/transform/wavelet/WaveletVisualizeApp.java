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

package boofcv.demonstrations.transform.wavelet;

import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.factory.transform.wavelet.GFactoryWavelet;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Visualizes wavelets processing.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class WaveletVisualizeApp<T extends ImageGray<T>, W extends ImageGray<W>, C extends WlCoef>
		extends SelectAlgorithmAndInputPanel {
	int numLevels = 3;

	@Nullable T image;
	T imageInv;

	Class<T> imageType;

	ListDisplayPanel panel = new ListDisplayPanel();
	boolean processedImage = false;

	public WaveletVisualizeApp( Class<T> imageType ) {
		super(1);
		this.imageType = imageType;

		addWaveletDesc("Haar", GFactoryWavelet.haar(imageType));
		addWaveletDesc("Daub 4", GFactoryWavelet.daubJ(imageType, 4));
		addWaveletDesc("Bi-orthogonal 5", GFactoryWavelet.biorthogoal(imageType, 5, BorderType.REFLECT));
		addWaveletDesc("Coiflet 6", GFactoryWavelet.coiflet(imageType, 6));

		setMainGUI(panel);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);

		image = ConvertBufferedImage.convertFromSingle(input, null, imageType);
		imageInv = (T)image.createNew(image.width, image.height);

		SwingUtilities.invokeLater(() -> {
			if (image == null)
				return;
			setPreferredSize(new Dimension(image.width + 50, image.height + 20));
			processedImage = true;
		});

		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile( String fileName ) {}

	@Override
	public void refreshAll( Object[] cookies ) {
		setActiveAlgorithm(0, "", cookies[0]);
	}

	private void addWaveletDesc( String name, @Nullable WaveletDescription desc ) {
		if (desc != null)
			addAlgorithm(0, name, desc);
	}

	@Override
	public void setActiveAlgorithm( int indexFamily, String name, Object cookie ) {
		if (image == null)
			return;

		WaveletDescription<C> desc = (WaveletDescription<C>)cookie;
		WaveletTransform<T, W, C> waveletTran =
				FactoryWaveletTransform.create((Class)image.getClass(), desc, numLevels, 0, 255);

		panel.reset();

		W imageWavelet = waveletTran.transform(image, null);

		waveletTran.invert(imageWavelet, imageInv);

		// adjust the values inside the wavelet transform to make it easier to see
		UtilWavelet.adjustForDisplay(imageWavelet, waveletTran.getLevels(), 255);
		BufferedImage buffWavelet = VisualizeImageData.grayMagnitude(imageWavelet, null, 255);
		BufferedImage buffInv = ConvertBufferedImage.convertTo(imageInv, null, true);

		panel.addImage(buffWavelet, "Transform");
		panel.addImage(buffInv, "Inverse");
	}

	@Override
	public void changeInput( String name, int index ) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if (image != null) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main( String[] args ) {
//		BufferedImage in = UtilImageIO.loadImage("data/standard/kodim17.bmp");
		WaveletVisualizeApp app = new WaveletVisualizeApp(GrayF32.class);
//		WaveletVisualizeApp app = new WaveletVisualizeApp(GrayU8.class);

		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Human Statue", UtilIO.pathExample("standard/kodim17.jpg")));
		inputs.add(new PathLabel("boat", UtilIO.pathExample("standard/boat.jpg")));
		inputs.add(new PathLabel("fingerprint", UtilIO.pathExample("standard/fingerprint.jpg")));
		inputs.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		inputs.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			BoofMiscOps.sleep(10);
		}

		ShowImages.showWindow(app, "Wavelet Transforms", true);
	}
}
