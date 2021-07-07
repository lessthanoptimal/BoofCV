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

package boofcv.demonstrations.sfm.d2;

import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.InvertibleTransform;
import georegression.struct.shapes.Quadrilateral_F64;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Attempts to remove camera jitter across multiple video frames by detecting point features inside the image
 * and tracking their motion. Models are then fit to the feature's motion and the inverse transform
 * computer and rendered. RANSAC is used internally to remove noise. Different feature descriptors and motion
 * models can be used. Both the unstabilized input and stabilized output are shown in a window.
 *
 * @param <I> Input image type
 * @param <D> Image derivative type
 * @author Peter Abeles
 */
public class VideoStabilizeSequentialPointApp<I extends ImageGray<I>, D extends ImageGray<D>,
		IT extends InvertibleTransform<IT>>
		extends VideoStitchBaseApp<I, IT> {
	int inputWidth, inputHeight;

	public VideoStabilizeSequentialPointApp( List<?> exampleInputs, Class<I> imageType ) {
		super(exampleInputs, new Stabilize2DPanel(), true, imageType);

		absoluteMinimumTracks = 40;
		respawnTrackFraction = 0.3;
		respawnCoverageFraction = 0.5;
		maxJumpFraction = 0.3;
		inlierThreshold = 4;

		initializeGui();
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int inputWidth, int inputHeight ) {
		this.inputWidth = inputWidth;
		this.inputHeight = inputHeight;
		handleAlgorithmChange();
	}

	@Override
	protected void handleAlgorithmChange() {
		super.handleAlgorithmChange();
		IT fitModel = createFitModelStructure();

		setStitchImageSize(inputWidth, inputHeight);
		((Stabilize2DPanel)gui).setInputSize(inputWidth, inputHeight);
		alg.configure(inputWidth, inputHeight, fitModel);
	}

	@Override
	protected boolean checkLocation( Quadrilateral_F64 corners ) {
		return false;
	}

	public static void main( String[] args ) {
		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Shake", UtilIO.pathExample("shake.mjpeg")));
		examples.add(new PathLabel("Zoom", UtilIO.pathExample("zoom.mjpeg")));
		examples.add(new PathLabel("Rotate", UtilIO.pathExample("rotate.mjpeg")));

		SwingUtilities.invokeLater(() -> {
			var app = new VideoStabilizeSequentialPointApp(examples, GrayF32.class);
			app.openExample(examples.get(0));
			app.display("Video Image Stabilize");
		});
	}
}
