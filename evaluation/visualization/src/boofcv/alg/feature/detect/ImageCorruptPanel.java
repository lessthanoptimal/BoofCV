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

package boofcv.alg.feature.detect;

import boofcv.alg.filter.basic.GGrayImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageSingleBand;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Random;

/**
 * Panel for corrupting the image in with gaussian noise and changes in lighting.
 *
 * @author Peter Abeles
 */
public class ImageCorruptPanel extends StandardAlgConfigPanel implements ChangeListener {

	Random rand = new Random(8383);

	// specifies amount of noise to add to the image
	JSpinner noiseLevel;

	// Adjusts image brightness by a scale factor
	JSpinner lightScale;
	// Adds to the image intensity
	JSpinner lightOffset;

	// listener for changes in corruption
	Listener listener;

	// last read values
	double valueNoise = 0;
	double valueScale = 1;
	double valueOffset = 0;

	public ImageCorruptPanel() {

		noiseLevel = new JSpinner(new SpinnerNumberModel(valueNoise,0,100,5));
		noiseLevel.addChangeListener(this);
		noiseLevel.setMaximumSize(noiseLevel.getPreferredSize());

		lightScale = new JSpinner(new SpinnerNumberModel(valueScale,0.5,2,0.1));
		lightScale.addChangeListener(this);
		lightScale.setMaximumSize(lightScale.getPreferredSize());

		lightOffset = new JSpinner(new SpinnerNumberModel(valueOffset,-30,30,5));
		lightOffset.addChangeListener(this);
		lightOffset.setMaximumSize(lightOffset.getPreferredSize());

		addLabeled(noiseLevel,"Noise",this);
		addLabeled(lightScale,"Light Scale",this);
		addLabeled(lightOffset,"Light Offset",this);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		valueNoise = ((Number)noiseLevel.getValue()).doubleValue();
		valueScale = ((Number)lightScale.getValue()).doubleValue();
		valueOffset = ((Number)lightOffset.getValue()).doubleValue();

		if( listener != null )
			listener.corruptImageChange();
	}

	/**
	 * Applies the specified corruption to the image.
	 * @param original Original uncorrupted image.
	 * @param corrupted Corrupted mage.
	 */
	public <T extends ImageSingleBand> void corruptImage( T original , T corrupted )
	{
		GGrayImageOps.stretch(original, valueScale, valueOffset, 255.0, corrupted);
		GImageMiscOps.addGaussian(corrupted, rand, valueNoise, 0, 255);
		GPixelMath.boundImage(corrupted,0,255);
	}

	public static interface Listener
	{
		public void corruptImageChange();
	}
}
