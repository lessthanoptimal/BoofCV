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

package boofcv.alg.sfm.d3.direct;

import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import boofcv.struct.pyramid.ImagePyramid;
import georegression.struct.se.Se3_F32;
import lombok.Getter;

/**
 * <p>Adds a pyramidal implementation on top of {@link VisOdomDirectColorDepth} to enable it to handle larger motions
 * which its local approach couldn't handle in a single layer. Highest layers (lowest resolution) are processed
 * first. Their estimated motion is then passed into the next layers for its initial guess.</p>
 *
 * <p>Selection of keyframes is a critical problem. A global keyframe is used for all pixels. if Keyframes are selected
 * too often then performance will be degraded and not often enough can cause it to fail completely. A new keyframe
 * is selected when the number of trackable pixels drops below a threshold or the it's spatial diversity has dropped
 * too low</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PyramidDirectColorDepth<T extends ImageGray<T>> {

	private final ImageType<Planar<T>> imageType;

	private final ImagePyramid<Planar<T>> pyramid;
	private VisOdomDirectColorDepth<T, ?>[] layersOdom;

	// When the diversity of the image drops below this fraction of the key frame create a new keyframe
	private double diversityThreshold = 0.75;
	private double keyframeDiversity; // diversity of the keyframe
	private double diversity; // diversity of the current frame

	// number of pixels which were valid in the last layer processed
	private double fractionInBounds = 0;

	private final LayerTo3D layerTo3D;

	private final Se3_F32 worldToKey = new Se3_F32();
	private final Se3_F32 keyToCurrent = new Se3_F32();
	private final Se3_F32 work = new Se3_F32();
	private final Se3_F32 worldToCurrent = new Se3_F32();

	/** Unique ID for each frame in the sequence it has processed */
	private @Getter long frameID = -1;

	public PyramidDirectColorDepth( ImagePyramid<Planar<T>> pyramid ) {
		this.pyramid = pyramid;
		imageType = this.pyramid.getImageType();

		layerTo3D = new LayerTo3D();
	}

	public void setCameraParameters( float fx, float fy,
									 float cx, float cy,
									 int width, int height ) {
		pyramid.initialize(width, height);
		layersOdom = new VisOdomDirectColorDepth[pyramid.getNumLayers()];
		for (int i = 0; i < layersOdom.length; i++) {
			ImageType derivType = GImageDerivativeOps.getDerivativeType(imageType);
			layersOdom[i] = new VisOdomDirectColorDepth(imageType.getNumBands(), imageType.getImageClass(), derivType.getImageClass());
		}
		for (int layer = 0; layer < layersOdom.length; layer++) {
			VisOdomDirectColorDepth o = layersOdom[layer];

			int layerWidth = pyramid.getWidth(layer);
			int layerHeight = pyramid.getHeight(layer);

			float scale = (float)pyramid.getScale(layer);

			o.setCameraParameters(fx/scale, fy/scale, cx/scale, cy/scale,
					layerWidth, layerHeight);
		}
	}

	public boolean process( Planar<T> input, ImagePixelTo3D inputDepth ) {
		pyramid.process(input);

		frameID++;
		if (fractionInBounds == 0) {
			setKeyFrame(inputDepth);
			fractionInBounds = 1.0;
		} else {
			if (estimateMotion()) {
				boolean keyframeTriggered = false;

//				System.out.printf("   d %6.2f  f %6.2f\n",UtilAngle.degree(diversity),fractionInBounds);

//				System.out.println("  spartial density "+(diversity*fractionInBounds));
				if (diversity < keyframeDiversity*diversityThreshold) {
//					System.out.println("  triggerd by diversity "+ UtilAngle.degree(diversity)+" deg");
					keyframeTriggered = true;
				}

				if (fractionInBounds < 0.5) {
//					System.out.println("  triggerd by fraction "+ fractionInBounds);
					keyframeTriggered = true;
				}

				if (keyframeTriggered) {
					setKeyFrame(inputDepth);
				}
			} else {
				return false;
			}
		}

		return true;
	}

	protected void setKeyFrame( ImagePixelTo3D inputDepth ) {
		layerTo3D.wrap(inputDepth);

		for (int layer = 0; layer < layersOdom.length; layer++) {
			Planar<T> layerImage = pyramid.getLayer(layer);
			layerTo3D.scale = pyramid.getScale(layer);
			layersOdom[layer].setKeyFrame(layerImage, layerTo3D);
		}
		worldToKey.concat(keyToCurrent, work);
		worldToKey.setTo(work);
		keyToCurrent.reset();

		keyframeDiversity = layersOdom[layersOdom.length - 1].computeFeatureDiversity(keyToCurrent);
	}

	protected boolean estimateMotion() {
		work.setTo(keyToCurrent);

		boolean oneLayerWorked = false;
		for (int layer = layersOdom.length - 1; layer >= 0; layer--) {
//			System.out.println("Layer   "+layer);
			Planar<T> layerImage = pyramid.getLayer(layer);
			VisOdomDirectColorDepth<T, ?> o = layersOdom[layer];
			if (o.estimateMotion(layerImage, work)) {
				oneLayerWorked = true;
				work.setTo(o.getKeyToCurrent());
//				work.print();

				fractionInBounds = o.getInboundsPixels()/(double)o.getKeyframePixels();
//				System.out.println("   fraction in bounds "+fractionInBounds);
			} else {
//				System.out.println("   failed");
				break;
			}
		}

		if (oneLayerWorked) {
			keyToCurrent.setTo(work);
			worldToKey.concat(keyToCurrent, worldToCurrent);

			// compute diversity in the smallest image. Should be about the same in all the layers
			diversity = layersOdom[layersOdom.length - 1].computeFeatureDiversity(keyToCurrent);
		}

		return oneLayerWorked;
	}

	public boolean isFatalError() {
		return false;
	}

	public Se3_F32 worldToCurrent() {
		return worldToCurrent;
	}

	public ImageType<Planar<T>> getInputType() {
		return imageType;
	}

	public void reset() {
		fractionInBounds = 0;
		keyToCurrent.reset();
		worldToCurrent.reset();
	}

	public void setDiversityThreshold( double diversityThreshold ) {
		this.diversityThreshold = diversityThreshold;
	}

	public double getFractionInBounds() {
		return fractionInBounds;
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class LayerTo3D implements ImagePixelTo3D {
		ImagePixelTo3D orig;

		public double scale;

		public void wrap( ImagePixelTo3D orig ) {
			this.orig = orig;
		}

		@Override
		public boolean process( double x, double y ) {
			return orig.process((x + 0.5)*scale, (y + 0.5)*scale);
		}

		@Override
		public double getX() {
			return orig.getX();
		}

		@Override
		public double getY() {
			return orig.getY();
		}

		@Override
		public double getZ() {
			return orig.getZ();
		}

		@Override
		public double getW() {
			return orig.getW();
		}
	}
}
