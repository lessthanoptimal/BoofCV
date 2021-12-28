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

package boofcv.demonstrations.transform.fft;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.misc.GPixelMath;
import boofcv.alg.transform.fft.GDiscreteFourierTransformOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Visualizes an FFT
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class FourierVisualizeApp<T extends GrayF<T>, W extends ImageInterleaved<W>>
		extends DemonstrationBase {
	DiscreteFourierTransform<T, W> fft;

	W transform;
	T magnitude;
	T phase;

	ImagePanel gui = new ImagePanel();
	FourierControls controls = new FourierControls();

	BufferedImage buffOriginal;
	BufferedImage buffVisualized;

	public FourierVisualizeApp( java.util.List<PathLabel> examples, ImageDataType imageType ) {
		super(examples, ImageType.single(imageType));

		add(gui, BorderLayout.CENTER);
		add(controls, BorderLayout.WEST);

		transform = GeneralizedImageOps.createInterleaved(imageType, 1, 1, 2);
		magnitude = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		phase = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		fft = GDiscreteFourierTransformOps.createTransform(imageType);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		transform.reshape(width, height);
		magnitude.reshape(width, height);
		phase.reshape(width, height);

		gui.setPreferredSize(new Dimension(width, height));
	}

	@Override
	protected void configureVideo( int which, SimpleImageSequence sequence ) {
		sequence.setLoop(true);
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase gray ) {
		T image = (T)gray;

		fft.forward(image, transform);

		GDiscreteFourierTransformOps.shiftZeroFrequency(transform, true);

		GDiscreteFourierTransformOps.magnitude(transform, magnitude);
		GDiscreteFourierTransformOps.phase(transform, phase);

		// Convert it to a log scale for visibility
		GPixelMath.log(magnitude, 1.0, magnitude);

		this.buffOriginal = buffered;

		updateImage();
	}

	private void updateImage() {
		SwingUtilities.invokeLater(() -> {
			BufferedImage b = null;
			switch (controls.view) {
				case Original:
					b = buffOriginal;
					break;

				case MAGNITUDE:
					buffVisualized = ConvertBufferedImage.checkDeclare(magnitude.width, magnitude.height,
							buffVisualized, BufferedImage.TYPE_INT_RGB);
					b = VisualizeImageData.grayMagnitude(magnitude, buffVisualized, -1);
					break;

				case PHASE:
					buffVisualized = ConvertBufferedImage.checkDeclare(magnitude.width, magnitude.height,
							buffVisualized, BufferedImage.TYPE_INT_RGB);
					b = VisualizeImageData.colorizeSign(phase, buffVisualized, Math.PI);
					break;
			}
			gui.setImageUI(b);
			gui.repaint();
		});
	}

	class FourierControls extends StandardAlgConfigPanel implements ListSelectionListener {
		JList listPanel;
		FftView view = FftView.MAGNITUDE;

		public FourierControls() {
			listPanel = new JList(FftView.values());

			listPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			listPanel.setSelectedIndex(0);
			listPanel.addListSelectionListener(this);

			addAlignCenter(listPanel);
		}

		@Override
		public void valueChanged( ListSelectionEvent e ) {
			view = FftView.values()[listPanel.getSelectedIndex()];
			updateImage();
		}
	}

	enum FftView {
		MAGNITUDE,
		PHASE,
		Original
	}

	public static void main( String[] args ) {

		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Human Statue", UtilIO.pathExample("standard/kodim17.jpg")));
		inputs.add(new PathLabel("boat", UtilIO.pathExample("standard/boat.jpg")));
		inputs.add(new PathLabel("fingerprint", UtilIO.pathExample("standard/fingerprint.jpg")));
		inputs.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		inputs.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));

		SwingUtilities.invokeLater(() -> {
			FourierVisualizeApp app = new FourierVisualizeApp(inputs, ImageDataType.F32);
//			FourierVisualizeApp app = new FourierVisualizeApp(inputs,ImageTypeInfo.F64);

			app.openExample(inputs.get(0));
			app.display("Discrete Fourier Transform");
		});
	}
}
