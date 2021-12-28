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

package boofcv.demonstrations.imageprocessing;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static boofcv.factory.filter.derivative.FactoryDerivative.*;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ShowImageDerivativeApp<T extends ImageGray<T>, D extends ImageGray<D>> extends DemonstrationBase {
	Class<T> imageType;
	Class<D> derivType;

	ImagePanel gui = new ImagePanel();
	DerivControls controls = new DerivControls();

	int whichAlg;
	int whichDeriv;

	D derivX;
	D derivY;
	D derivXX;
	D derivYY;
	D derivXY;

	BufferedImage renderedImage;

	ImageGradient<T, D> gradient;
	ImageHessian<D> hessian;

	public ShowImageDerivativeApp( java.util.List<PathLabel> examples, Class<T> imageType ) {
		super(examples, ImageType.single(imageType));
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		derivX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivXX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivYY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivXY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);

		add(gui, BorderLayout.CENTER);
		add(controls, BorderLayout.WEST);

		declareAlgorithm();
	}

	protected void declareAlgorithm() {
		switch (whichAlg) {
			case 0 -> gradient = prewitt(imageType, derivType);
			case 1 -> gradient = sobel(imageType, derivType);
			case 2 -> gradient = scharr(imageType, derivType);
			case 3 -> gradient = three(imageType, derivType);
			case 4 -> gradient = two0(imageType, derivType);
			case 5 -> gradient = two1(imageType, derivType);
			case 6 -> gradient = gaussian(-1, 3, imageType, derivType);
		}
		hessian = hessianThree(derivType);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		derivX.reshape(width, height);
		derivY.reshape(width, height);
		derivXX.reshape(width, height);
		derivYY.reshape(width, height);
		derivXY.reshape(width, height);

		renderedImage = ConvertBufferedImage.checkDeclare(width, height, renderedImage, BufferedImage.TYPE_INT_RGB);

		gui.setPreferredSize(new Dimension(width, height));
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {
		T image = (T)input;

		gradient.process(image, derivX, derivY);
		hessian.process(derivX, derivY, derivXX, derivYY, derivXY);

		updateVisuals();
	}

	public void updateVisuals() {
		new Thread(() -> {
			double max;
			switch (whichDeriv) {
				case 0 -> VisualizeImageData.colorizeGradient(derivX, derivY, -1, renderedImage);
				case 1 -> {
					max = GImageStatistics.maxAbs(derivX);
					VisualizeImageData.colorizeSign(derivX, renderedImage, max);
				}
				case 2 -> {
					max = GImageStatistics.maxAbs(derivY);
					VisualizeImageData.colorizeSign(derivY, renderedImage, max);
				}
				case 3 -> {
					max = GImageStatistics.maxAbs(derivXX);
					VisualizeImageData.colorizeSign(derivXX, renderedImage, max);
				}
				case 4 -> {
					max = GImageStatistics.maxAbs(derivYY);
					VisualizeImageData.colorizeSign(derivYY, renderedImage, max);
				}
				case 5 -> {
					max = GImageStatistics.maxAbs(derivXY);
					VisualizeImageData.colorizeSign(derivXY, renderedImage, max);
				}
			}
			SwingUtilities.invokeLater(() -> {
				gui.setImageRepaint(renderedImage);
			});
		}).start();
	}

	class DerivControls extends StandardAlgConfigPanel implements ActionListener {
		JComboBox<String> comboAlg;
		JComboBox<String> comboWhich;

		public DerivControls() {
			comboAlg = combo(0, "Prewitt", "Sobel", "Scharr", "Three", "Two0", "Two1", "Gaussian");
			comboWhich = combo(0, "Gradient", "Deriv X", "Deriv Y", "Deriv XX", "Deriv YY", "Deriv XY");

			addLabeled(comboAlg, "Operation");
			addLabeled(comboWhich, "Derivative");
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (comboAlg == e.getSource()) {
				whichAlg = comboAlg.getSelectedIndex();
				declareAlgorithm();
				reprocessImageOnly();
			} else if (comboWhich == e.getSource()) {
				whichDeriv = comboWhich.getSelectedIndex();
				updateVisuals();
			}
		}
	}

	public static void main( String[] args ) {
		var examples = new ArrayList<PathLabel>();
		examples.add(new PathLabel("Horses", UtilIO.pathExample("segment/berkeley_horses.jpg")));
		examples.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("xray", UtilIO.pathExample("xray01.jpg")));
		examples.add(new PathLabel("beach", UtilIO.pathExample("scale/beach02.jpg")));

		SwingUtilities.invokeLater(() -> {
			var app = new ShowImageDerivativeApp<>(examples, GrayF32.class);

			app.openExample(examples.get(0));
			app.display("Image Derivatives");
		});
	}
}
