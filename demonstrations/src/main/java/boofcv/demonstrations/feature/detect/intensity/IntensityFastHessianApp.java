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

package boofcv.demonstrations.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.GIntegralImageFeatureIntensity;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays intensity of features in the SURF feature detector
 *
 * @author Peter Abeles
 */
public class IntensityFastHessianApp<T extends ImageGray<T>>
		extends DemonstrationBase {

	ImageGray integral;
	GrayF32 intensity = new GrayF32(1, 1);

	ListDisplayPanel guiIntensity = new ListDisplayPanel();

	public IntensityFastHessianApp( List<?> exampleInputs, Class<T> imageType ) {
		super(exampleInputs, ImageType.single(imageType));

		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		integral = GeneralizedImageOps.createSingleBand(integralType, 1, 1);
		add(guiIntensity, BorderLayout.CENTER);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);

		SwingUtilities.invokeLater(() -> {
			guiIntensity.setPreferredSize(new Dimension(width, height));
		});
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		GIntegralImageOps.transform((T)input, integral);
		intensity.reshape(integral.width, input.height);

		List<BufferedImage> layers = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		int skip = 0;
		for (int octave = 0; octave < 4; octave++) {
			if (skip == 0)
				skip = 1;
			else
				skip = skip + skip;
			for (int sizeIndex = 0; sizeIndex < 4; sizeIndex++) {
				int block = 1 + skip*2*(sizeIndex + 1);
				int size = 3*block;

				GIntegralImageFeatureIntensity.hessian(integral, 1, size, intensity, null, null, null);
				float maxAbs = ImageStatistics.maxAbs(intensity);
				BufferedImage b = VisualizeImageData.colorizeSign(intensity, null, maxAbs);
				labels.add(String.format("Oct = %2d size %3d", octave + 1, size));
				layers.add(b);
			}
		}

		SwingUtilities.invokeLater(() -> {
			guiIntensity.reset();
			guiIntensity.addImage(buffered, "Original");
			guiIntensity.addImage(input, "Gray");
			for (int i = 0; i < layers.size(); i++) {
				guiIntensity.addImage(layers.get(i), labels.get(i), ScaleOptions.DOWN);
			}
		});
	}

	public static void main( String[] args ) {

		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("outdoors01", UtilIO.pathExample("outdoors01.jpg")));
		examples.add(new PathLabel("particles01", UtilIO.pathExample("particles01.jpg")));
		examples.add(new PathLabel("beach02", UtilIO.pathExample("scale/beach02.jpg")));
		examples.add(new PathLabel("mountain", UtilIO.pathExample("data/scale/mountain_7p1mm.jpg")));
		examples.add(new PathLabel("shapes01", UtilIO.pathExample("fiducial/shapes01.png")));

		SwingUtilities.invokeLater(() -> {
			IntensityFastHessianApp<GrayF32> app = new IntensityFastHessianApp(examples, GrayF32.class);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Feature Intensity");
		});
	}
}
