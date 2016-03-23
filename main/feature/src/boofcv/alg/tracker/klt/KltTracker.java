/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.klt;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * A Kanade-Lucas-Tomasi (KLT) [1,2,3,4] point feature tracker for a single layer gray scale image.  It tracks point features
 * across a sequence of images by having each feature individually follow the image's gradient.  Feature locations
 * are estimated to within sub-pixel accuracy.
 * </p>
 *
 * <p>
 * For this particular implementation of KLT, image derivatives is only needed when setDescription() is called.
 * Tracker quality will degrade if features change orientation, but this technique is significantly faster.
 * </p>
 *
 * <p>
 * Citations:<br>
 * <br>
 * [1] Bruce D. Lucas and Takeo Kanade.  An Iterative Image Registration Technique with an
 * Application to Stereo Vision.  International Joint Conference on Artificial Intelligence,
 * pages 674-679, 1981.<br>
 * [2] Carlo Tomasi and Takeo Kanade. Detection and Tracking of Point Features. Carnegie
 * Mellon University Technical Report CMU-CS-91-132, April 1991.<br>
 * [3] Jianbo Shi and Carlo Tomasi. Good Features to Track. IEEE Conference on Computer
 * Vision and Pattern Recognition, pages 593-600, 1994.<br>
 * [4] Stan Birchfield, http://www.ces.clemson.edu/~stb/klt/
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"SuspiciousNameCombination"})
public class KltTracker<InputImage extends ImageGray, DerivativeImage extends ImageGray> {

	// input image
	protected InputImage image;
	// image gradient
	protected DerivativeImage derivX, derivY;

	// Used to interpolate the image and gradient
	protected InterpolateRectangle<InputImage> interpInput;
	protected InterpolateRectangle<DerivativeImage> interpDeriv;

	// tracker configuration
	protected KltConfig config;

	// feature description curvature information
	protected float Gxx, Gyy, Gxy;
	// residual times the gradient
	protected float Ex, Ey;

	// width of the feature
	protected int widthFeature;
	// length of the feature description
	protected int lengthFeature;
	// the feature in the current image
	protected GrayF32 currDesc = new GrayF32(1,1);

	// storage for sub-region used when computing interpolation
	protected GrayF32 subimage = new GrayF32();

	// destination image for current feature data in border case
	int dstX0,dstY0,dstX1,dstY1;
	// top-left corner of feature in input image for border case
	float srcX0 , srcY0;

	// allowed feature bounds
	float allowedLeft;
	float allowedRight;
	float allowedTop;
	float allowedBottom;

	// bounds for checking to see if it is out of the image
	float outsideLeft;
	float outsideRight;
	float outsideTop;
	float outsideBottom;

	// error between template and the current track position in the image
	float error;

	public KltTracker(InterpolateRectangle<InputImage> interpInput,
					  InterpolateRectangle<DerivativeImage> interpDeriv,
					  KltConfig config) {
		this.interpInput = interpInput;
		this.interpDeriv = interpDeriv;
		this.config = config;
	}

	/**
	 * Sets the current image it should be tracking with.
	 *
	 * @param image  Original input image.
	 * @param derivX Image derivative along the x-axis
	 * @param derivY Image derivative along the y-axis
	 */
	public void setImage(InputImage image, DerivativeImage derivX, DerivativeImage derivY) {
		InputSanityCheck.checkSameShape(image, derivX, derivY);

		this.image = image;
		this.interpInput.setImage(image);

		this.derivX = derivX;
		this.derivY = derivY;
	}

	/**
	 * Same as {@link #setImage}, but it doesn't check to see if the images are the same size each time
	 */
	public void unsafe_setImage(InputImage image, DerivativeImage derivX, DerivativeImage derivY) {
		this.image = image;
		this.interpInput.setImage(image);

		this.derivX = derivX;
		this.derivY = derivY;
	}

	/**
	 * Sets the features description using the current image and the location of the feature stored in the feature.
	 * If the feature is an illegal location and cannot be set then false is returned.
	 * @param feature Feature description which is to be set.  Location must be specified.
	 * @return true if the feature's description was modified.
	 */
	@SuppressWarnings({"SuspiciousNameCombination"})
	public boolean setDescription(KltFeature feature) {
		setAllowedBounds(feature);

		if (!isFullyInside(feature.x, feature.y)) {
			if( isFullyOutside(feature.x,feature.y))
				return false;
			else
				return internalSetDescriptionBorder(feature);
		}

		return internalSetDescription(feature);
	}

	protected boolean internalSetDescription(KltFeature feature) {
		int regionWidth = feature.radius * 2 + 1;
		int size = regionWidth * regionWidth;

		float tl_x = feature.x - feature.radius;
		float tl_y = feature.y - feature.radius;

		interpInput.setImage(image);
		interpInput.region(tl_x, tl_y, feature.desc);
		interpDeriv.setImage(derivX);
		interpDeriv.region(tl_x, tl_y, feature.derivX);
		interpDeriv.setImage(derivY);
		interpDeriv.region(tl_x, tl_y, feature.derivY);

		float Gxx = 0, Gyy = 0, Gxy = 0;
		for (int i = 0; i < size; i++) {
			float dX = feature.derivX.data[i];
			float dY = feature.derivY.data[i];

			Gxx += dX * dX;
			Gyy += dY * dY;
			Gxy += dX * dY;
		}

		feature.Gxx = Gxx;
		feature.Gyy = Gyy;
		feature.Gxy = Gxy;

		float det = Gxx * Gyy - Gxy * Gxy;

		return (det >= config.minDeterminant*lengthFeature);
	}

	/**
	 * Computes the descriptor for border features.  All it needs to do
	 * is save the pixel value, but derivative information is also computed
	 * so that it can reject bad features immediately.
	 */
	protected boolean internalSetDescriptionBorder(KltFeature feature) {

		computeSubImageBounds(feature, feature.x, feature.y);

		ImageMiscOps.fill(feature.desc, Float.NaN);
		feature.desc.subimage(dstX0, dstY0, dstX1, dstY1, subimage);
		interpInput.setImage(image);
		interpInput.region(srcX0, srcY0, subimage);

		feature.derivX.subimage(dstX0, dstY0, dstX1, dstY1, subimage);
		interpDeriv.setImage(derivX);
		interpDeriv.region(srcX0, srcY0, subimage);

		feature.derivY.subimage(dstX0, dstY0, dstX1, dstY1, subimage);
		interpDeriv.setImage(derivY);
		interpDeriv.region(srcX0, srcY0, subimage);

		int total= 0;

		Gxx = Gyy = Gxy = 0;
		for( int i = 0; i < lengthFeature; i++ ) {
			if( Float.isNaN(feature.desc.data[i]))
				continue;

			total++;

			float dX = feature.derivX.data[i];
			float dY = feature.derivY.data[i];

			Gxx += dX * dX;
			Gyy += dY * dY;
			Gxy += dX * dY;
		}

		// technically don't need to save this...
		feature.Gxx = Gxx;
		feature.Gyy = Gyy;
		feature.Gxy = Gxy;

		float det = Gxx * Gyy - Gxy * Gxy;

		return (det >= config.minDeterminant*total);
	}

	/**
	 * <p>
	 * Updates the feature's location inside the image.  The feature's position can be modified
	 * even if tracking fails.
	 * </p>
	 *
	 * @param feature Feature being tracked.
	 * @return If the tracking was successful or not.
	 */
	public KltTrackFault track(KltFeature feature) {

		// precompute bounds and other values
		setAllowedBounds(feature);

		// sanity check to make sure it is actually inside the image
		if ( isFullyOutside(feature.x, feature.y))
			return KltTrackFault.OUT_OF_BOUNDS;

		if (currDesc.data.length < lengthFeature) {
			currDesc.reshape(widthFeature,widthFeature);
		}

		// save the original location so that a drifting fault can be detected
		float origX = feature.x, origY = feature.y;

		// If the feature is complete then the fast code can be used when entirely inside
		boolean complete = isDescriptionComplete(feature);

		float det = 0;

		// make sure its inside this image
		if ( complete ) {
			// see if the determinant is too small
			Gxx = feature.Gxx;
			Gyy = feature.Gyy;
			Gxy = feature.Gxy;
			det = Gxx * Gyy - Gxy * Gxy;
			if (det < config.minDeterminant*lengthFeature) {
				return KltTrackFault.FAILED;
			}
		}

		for (int iter = 0; iter < config.maxIterations; iter++) {
			float dx,dy;
			if( complete && isFullyInside(feature.x, feature.y) ) {
				computeE(feature, feature.x, feature.y);
			} else {
				// once it goes outside it must remain outside.  If it starts outside
				int length = computeGandE_border(feature, feature.x, feature.y);

				det = Gxx * Gyy - Gxy * Gxy;
				if (det <= config.minDeterminant*length) {
					return KltTrackFault.FAILED;
				}
			}

			// solve for D
			dx = (Gyy * Ex - Gxy * Ey) / det;
			dy = (Gxx * Ey - Gxy * Ex) / det;

			feature.x += dx;
			feature.y += dy;

			// see if it moved outside of the image
			if ( isFullyOutside(feature.x, feature.y))
				return KltTrackFault.OUT_OF_BOUNDS;

			// see if it has moved more than possible if it is really tracking a target
			// this happens in regions with little texture
			if (Math.abs(feature.x - origX) > widthFeature
					|| Math.abs(feature.y - origY) > widthFeature)
				return KltTrackFault.DRIFTED;

			// see if it has converged to a solution
			if (Math.abs(dx) < config.minPositionDelta && Math.abs(dy) < config.minPositionDelta) {
				break;
			}
		}

		if ( (error=computeError(feature)) > config.maxPerPixelError)
			return KltTrackFault.LARGE_ERROR;

		return KltTrackFault.SUCCESS;
	}

	/**
	 * Precompute image bounds that the feature is allowed inside of
	 */
	protected void setAllowedBounds(KltFeature feature) {
		// compute the feature's width and temporary storage related to it
		widthFeature = feature.radius * 2 + 1;
		lengthFeature = widthFeature * widthFeature;

		allowedLeft = feature.radius;
		allowedTop = feature.radius;
		allowedRight = image.width - feature.radius-1;
		allowedBottom = image.height - feature.radius-1;

		outsideLeft = -feature.radius;
		outsideTop = -feature.radius;
		outsideRight = image.width + feature.radius-1;
		outsideBottom = image.height + feature.radius-1;
	}

	private float computeError(KltFeature feature) {
		float error = 0;
		int total = 0;
		for (int i = 0; i < lengthFeature; i++) {

			if( Float.isNaN(feature.desc.data[i]) || Float.isNaN(currDesc.data[i]))
				continue;

			// compute the difference between the previous and the current image
			error += Math.abs(feature.desc.data[i] - currDesc.data[i]);
			total++;
		}
		return error / total;
	}

	protected void computeE(KltFeature feature, float x, float y) {
		// extract the region in the current image
		interpInput.region(x - feature.radius, y - feature.radius, currDesc);

		Ex = 0;
		Ey = 0;
		for (int i = 0; i < lengthFeature; i++) {
			// compute the difference between the previous and the current image
			float d = feature.desc.data[i] - currDesc.data[i];

			Ex += d * feature.derivX.data[i];
			Ey += d * feature.derivY.data[i];
		}
	}

	/**
	 * When part of the region is outside the image G and E need to be recomputed
	 */
	protected int computeGandE_border(KltFeature feature, float cx, float cy) {

		computeSubImageBounds(feature, cx, cy);

		ImageMiscOps.fill(currDesc, Float.NaN);
		currDesc.subimage(dstX0, dstY0, dstX1, dstY1, subimage);
		interpInput.setImage(image);
		interpInput.region(srcX0, srcY0, subimage);

		int total = 0;

		Gxx = 0; Gyy = 0; Gxy = 0;
		Ex = 0; Ey = 0;

		for( int i = 0; i < lengthFeature; i++ ) {
			float template = feature.desc.data[i];
			float current = currDesc.data[i];

			// if the description was outside of the image here skip it
			if( Float.isNaN(template) || Float.isNaN(current))
				continue;

			// count total number of points inbounds
			total++;

			float dX = feature.derivX.data[i];
			float dY = feature.derivY.data[i];

			// compute the difference between the previous and the current image
			float d = template - current;

			Ex += d * dX;
			Ey += d * dY;

			Gxx += dX * dX;
			Gyy += dY * dY;
			Gxy += dX * dY;
		}

		return total;
	}

	private void computeSubImageBounds(KltFeature feature, float cx, float cy) {
		// initially include the whole destination image
		dstX0 = 0;
		dstY0 = 0;
		dstX1 = widthFeature;
		dstY1 = widthFeature;

		// location of upper left corner of feature in input image
		srcX0 = cx - feature.radius;
		srcY0 = cy - feature.radius;
		float srxX1 = srcX0 + widthFeature;
		float srxY1 = srcY0 + widthFeature;

		// take in account the image border
		if( srcX0 < 0 ) {
			dstX0 = (int)-Math.floor(srcX0);
			srcX0 += dstX0;
		}
		if( srxX1 > image.width ) {
			dstX1 -= (int)Math.ceil(srxX1-image.width);
			// rounding error
			dstX1 -= (srcX0 + (dstX1-dstX0) > image.width ? 1 : 0);
		}
		if( srcY0 < 0 ) {
			dstY0 = (int)-Math.floor(srcY0);
			srcY0 += dstY0;
		}
		if( srxY1 > image.height ) {
			dstY1 -= (int)Math.ceil(srxY1-image.height);
			// rounding error
			dstY1 -= srcY0 + (dstY1-dstY0) > image.height ? 1 : 0;
		}

		if( srcX0 < 0 || srcY0 < 0 || srcX0 + (dstX1-dstX0) > image.width || srcY0 + (dstY1-dstY0) > image.height ) {
			throw new IllegalArgumentException("Region is outside of the image");
		}
	}

	/**
	 * Checks to see if the feature description is complete or if it was created by a feature partially
	 * outside the image
	 */
	public boolean isDescriptionComplete( KltFeature feature ) {
		for( int i = 0; i < lengthFeature; i++ ) {
			if( Float.isNaN(feature.desc.data[i]) )
				return false;
		}
		return true;
	}

	/**
	 * Returns true if the features is entirely enclosed inside of the image.
	 */
	public boolean isFullyInside(float x, float y) {
		if (x < allowedLeft || x > allowedRight)
			return false;
		if (y < allowedTop || y > allowedBottom)
			return false;

		return true;
	}

	/**
	 * Returns true if the features is entirely outside of the image.  A region is entirely outside if not
	 * an entire pixel is contained inside the image.  So if only  0.999 of a pixel is inside then the whole
	 * region is considered to be outside.  Can't interpolate nothing...
	 */
	public boolean isFullyOutside(float x, float y) {
		if (x < outsideLeft || x > outsideRight)
			return true;
		if (y < outsideTop || y > outsideBottom)
			return true;

		return false;
	}

	/**
	 * Average absolute value of the difference between each pixel in the image and the template
	 * @return Average error
	 */
	public float getError() {
		return error;
	}

	public KltConfig getConfig() {
		return config;
	}
}
