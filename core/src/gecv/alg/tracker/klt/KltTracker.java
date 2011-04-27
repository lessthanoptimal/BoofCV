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

package gecv.alg.tracker.klt;

import gecv.alg.InputSanityCheck;
import gecv.alg.interpolate.InterpolateRegion;
import gecv.struct.image.ImageBase;

/**
 * <p>
 * A Kanade-Lucas-Tomasi (KLT) [1,2,3,4] point feature tracker for a single layer gray scale image.  It tracks point features
 * across a sequence of images by having each feature individually follow the image's gradient.  Feature locations
 * are estimated to within sub-pixel accuracy.
 * </p>
 * <p/>
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
public class KltTracker<InputImage extends ImageBase, DerivativeImage extends ImageBase> {

	// input image
	protected InputImage image;
	// image gradient
	protected DerivativeImage derivX, derivY;

	// Used to interpolate the image and gradient
	protected InterpolateRegion<InputImage> interpInput;
	protected InterpolateRegion<DerivativeImage> interpDeriv;

	// tracker configuration
	KltConfig config;

	// feature description curvature information
	float Gxx, Gyy, Gxy;
	// residual times the gradient
	float Ex, Ey;
	// residual error
	float error;

	// width of the feature
	int widthFeature;
	// length of the feature description
	int lengthFeature;
	// the feature in the current image
	float descFeature[];

	// allowed feature bounds
	float allowedLeft;
	float allowedRight;
	float allowedTop;
	float allowedBottom;

	public KltTracker(InterpolateRegion<InputImage> interpInput,
					  InterpolateRegion<DerivativeImage> interpDeriv,
					  KltConfig config) {
		this.interpInput = interpInput;
		this.interpDeriv = interpDeriv;
		this.config = config;
	}

	/**
	 * Sets the current image it should be tracking with.  In some situations the image derivative might not be set.
	 * If {@link #setDescription(KltFeature)} is used they must always be set.  Otherwise {@link #getRequiresDerivative()}
	 * can be called to see.
	 *
	 * @param image  Original input image.
	 * @param derivX Image derivative along the x-axis
	 * @param derivY Image derivative along the y-axis
	 */
	public void setImage(InputImage image, DerivativeImage derivX, DerivativeImage derivY) {
		if (derivX != null && derivY != null)
			InputSanityCheck.checkSameShape(image, derivX, derivY);

		this.image = image;
		interpInput.setImage(image);

		this.derivX = derivX;
		this.derivY = derivY;
	}

	/**
	 * Does the tracker require image derivatives to be passed in?
	 *
	 * @return
	 */
	public boolean getRequiresDerivative() {
		return false;
	}

	@SuppressWarnings({"SuspiciousNameCombination"})
	public void setDescription(KltFeature feature) {
		if (derivX == null || derivY == null)
			throw new IllegalArgumentException("Image derivatives must be set");

		setAllowedBounds(feature);
		int regionWidth = feature.radius * 2 + 1;
		int size = regionWidth * regionWidth;

		if (!isFullyInside(feature.x, feature.y))
			throw new IllegalArgumentException("Feature is too close to the image's border");

		float tl_x = feature.x - feature.radius;
		float tl_y = feature.y - feature.radius;

		interpInput.region(tl_x, tl_y, feature.pixel, regionWidth, regionWidth);
		interpDeriv.setImage(derivX);
		interpDeriv.region(tl_x, tl_y, feature.derivX, regionWidth, regionWidth);
		interpDeriv.setImage(derivY);
		interpDeriv.region(tl_x, tl_y, feature.derivY, regionWidth, regionWidth);

		float Gxx = 0, Gyy = 0, Gxy = 0;
		for (int i = 0; i < size; i++) {
			float dX = feature.derivX[i];
			float dY = feature.derivY[i];

			Gxx += dX * dX;
			Gyy += dY * dY;
			Gxy += dX * dY;
		}

		feature.Gxx = Gxx;
		feature.Gyy = Gyy;
		feature.Gxy = Gxy;
	}

	public KltTrackFault track(KltFeature feature) {
		// save the original location so that a drifting fault can be detected
		float origX = feature.x, origY = feature.y;
		float x = feature.x, y = feature.y;

		setAllowedBounds(feature);

		// make sure its inside this image
		if (!isFullyInside(x, y)) {
			return KltTrackFault.OUT_OF_BOUNDS;
		}

		// see if the determinant is too small
		Gxx = feature.Gxx;
		Gyy = feature.Gyy;
		Gxy = feature.Gxy;
		float det = Gxx * Gyy - Gxy * Gxy;
		if (det < config.minDeterminant) {
			return KltTrackFault.FAILED;
		}

		// compute the feature's width and temporary storage related to it
		widthFeature = feature.radius * 2 + 1;
		lengthFeature = widthFeature * widthFeature;
		if (descFeature == null || descFeature.length < lengthFeature)
			descFeature = new float[lengthFeature];

		for (int iter = 0; iter < config.maxIterations; iter++) {
			computeE(feature, x, y);

			// solve for D
			float dx = (Gyy * Ex - Gxy * Ey) / det;
			float dy = (Gxx * Ey - Gxy * Ex) / det;

			x += dx;
			y += dy;

			// see if it move outside of the image
			if (!isFullyInside(x, y))
				return KltTrackFault.OUT_OF_BOUNDS;

			// see if it has moved more than possible if it is really tracking a target
			// this happens in regions with little texture
			if (Math.abs(x - origX) > widthFeature
					|| Math.abs(y - origY) > widthFeature)
				return KltTrackFault.DRIFTED;

			if (Math.abs(dx) < config.minPositionDelta && Math.abs(dy) < config.minPositionDelta) {
				break;
			}
		}

		if (computeError(feature) > config.maxError)
			return KltTrackFault.LARGE_ERROR;

		feature.x = x;
		feature.y = y;

		return KltTrackFault.SUCCESS;
	}

	private void setAllowedBounds(KltFeature feature) {
		allowedLeft = feature.radius + config.forbiddenBorder;
		allowedTop = feature.radius + config.forbiddenBorder;
		allowedRight = image.width - (feature.radius + config.forbiddenBorder);
		allowedBottom = image.height - (feature.radius + config.forbiddenBorder);
	}

	private float computeError(KltFeature feature) {
		float error = 0;
		for (int i = 0; i < lengthFeature; i++) {
			// compute the difference between the previous and the current image
			error += Math.abs(feature.pixel[i] - descFeature[i]);
		}
		return error / lengthFeature;
	}

	private void computeE(KltFeature feature, float x, float y) {
		// extract the region in the current image
		interpInput.region(x - feature.radius, y - feature.radius, descFeature, widthFeature, widthFeature);

		Ex = 0;
		Ey = 0;
		error = 0;
		for (int i = 0; i < lengthFeature; i++) {
			// compute the difference between the previous and the current image
			float d = feature.pixel[i] - descFeature[i];

			Ex += d * feature.derivX[i];
			Ey += d * feature.derivY[i];
		}
	}


	/**
	 * Returns true if the features is entirely enclosed inside of the image.
	 */
	protected boolean isFullyInside(float x, float y) {
		if (x < allowedLeft || x >= allowedRight)
			return false;
		if (y < allowedTop || y >= allowedBottom)
			return false;

		return true;
	}

	public KltConfig getConfig() {
		return config;
	}
}
