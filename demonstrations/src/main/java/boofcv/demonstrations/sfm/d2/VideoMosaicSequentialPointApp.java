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
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a mosaic from an image sequence using tracked point features. Each the input window
 * moaes toward the mosaic image's boundary it is automatically reset. When reset the current
 * image is put in the initial position and the mosaic distorted accordingly.
 *
 * @param <I> Input image type
 * @param <D> Image derivative type
 * @author Peter Abeles
 */
public class VideoMosaicSequentialPointApp
		<I extends ImageGray<I>, D extends ImageGray<D>, IT extends InvertibleTransform<IT>>
		extends VideoStitchBaseApp<I, IT> {
	public VideoMosaicSequentialPointApp( List<?> exampleInputs, Class<I> imageType ) {
		super(exampleInputs, new Mosaic2DPanel(), true, imageType);

		absoluteMinimumTracks = 40;
		respawnTrackFraction = 0.4;
		respawnCoverageFraction = 0.8;
		maxJumpFraction = 0.3;
		inlierThreshold = 4;

		initializeGui();
	}

	private IT createInitialTransform() {
		float scale = 0.8f;

		IT fitModel = createFitModelStructure();

		if (fitModel instanceof Affine2D_F64) {
			Affine2D_F64 H = new Affine2D_F64(scale, 0, 0, scale, stitchWidth/4, stitchHeight/4);
			return (IT)H.invert(null);
		} else if (fitModel instanceof Homography2D_F64) {
			Homography2D_F64 H = new Homography2D_F64(scale, 0, stitchWidth/4, 0, scale, stitchHeight/4, 0, 0, 1);
			return (IT)H.invert(null);
		} else {
			throw new RuntimeException("Need to support this model type: " + fitModel.getClass().getSimpleName());
		}
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		handleAlgorithmChange();
	}

	@Override
	protected void handleAlgorithmChange() {
		super.handleAlgorithmChange();
		setStitchImageSize(1000, 600);
		((Mosaic2DPanel)gui).setMosaicSize(stitchWidth, stitchHeight);
		alg.configure(stitchWidth, stitchHeight, createInitialTransform());
	}

	@Override
	protected boolean checkLocation( Quadrilateral_F64 corners ) {
		if (closeToBorder(corners.a))
			return true;
		if (closeToBorder(corners.b))
			return true;
		if (closeToBorder(corners.c))
			return true;
		if (closeToBorder(corners.d))
			return true;

		return false;
	}

	private boolean closeToBorder( Point2D_F64 pt ) {
		if (pt.x < borderTolerance || pt.y < borderTolerance)
			return true;
		return (pt.x >= stitchWidth - borderTolerance || pt.y >= stitchHeight - borderTolerance);
	}

	public static void main( String[] args ) {
		Class type = GrayF32.class;

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Plane 1", UtilIO.pathExample("mosaic/airplane01.mjpeg")));
		examples.add(new PathLabel("Plane 2", UtilIO.pathExample("mosaic/airplane02.mjpeg")));
		examples.add(new PathLabel("Shake", UtilIO.pathExample("shake.mjpeg")));

		SwingUtilities.invokeLater(() -> {

			VideoMosaicSequentialPointApp app = new VideoMosaicSequentialPointApp(examples, type);

			app.openExample(examples.get(0));
			app.display("Video Image Mosaic");
		});
	}
}
