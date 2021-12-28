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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.filter.convolve.ImageConvolveSparse;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.feature.detect.selector.SampleIntensityScalePoint;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.filter.convolve.FactoryConvolveSparse;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.GrayF32;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

import java.util.List;

import static boofcv.alg.feature.detect.interest.FastHessianFeatureDetector.polyPeak;

/**
 * <p>Implementation of SIFT [1] feature detector. Feature detection is first done by creating the first octave in
 * a {@link SiftScaleSpace scale space}. Then the Difference-of-Gaussian (DoG) is computed from sequential
 * scales inside the scale-space. From the DoG images, pixels which are maximums and minimums spatially and with
 * in scale are found. Edges of objects can cause false positives so those are suppressed. The remaining
 * features are interpolated spatially and across scale.</p>
 *
 * <p>This class is designed so that it can operate as a stand alone feature detector or so that it can be
 * extended to compute feature descriptors too. The advantage of the former is that the scale-space only
 * needs to be constructed once.</p>
 *
 * <h2>Processing Steps</h2>
 * <ol>
 * <li>Construct first octave of DoG images using {@link SiftScaleSpace}</li>
 * <li>For DoG images 1 to N+1, detect features</li>
 * <li>Use {@link NonMaxLimiter} to detect features spatially.</li>
 * <li>Check to see if detected features are minimums or maximums in DoG scale space by checking the equivalent 3x3
 * regions in the DoG images above and below it. {@link #isScaleSpaceExtremum}
 * <li>Detect false positive edges using trace and determinant from Hessian of DoG image</li>
 * <li>Interpolate feature's (x,y,sigma) coordinate using the peak of a 2nd order polynomial (quadratic).
 * {@link #createDetection}</li>
 * </ol>
 * <p>Where N is the number of scale parameters. There are N+3 scale images and N+2 DoG images in an octave.
 *
 * <h2>Edge Detection</h2>
 * <p>Edges can also cause local extremes (false positives) in the DoG image. To remove those false positives an
 * edge detector is proposed by Lowe. The edge detector is turned with the parameter 'r' and a point is considered
 * an edge if the following is true:<br>
 * Tr<sup>2</sup>/Det < (r+1)<sup>2</sup>/r<br>
 * where Tr and Det are the trace an determinant of a 2x2 hessian matrix computed from the DoG hessian at that point,
 * [dXX,dXY;dYX,dYY]</p>
 *
 * <h2>Deviations from standard SIFT</h2>
 * <ol>
 * <li>Spatial maximums are not limited to a 3x3 region like they are in the paper. User configurable.</li>
 * <li>Quadratic interpolation is used independently on x,y, and scale axis.</li>
 * <li>What the scale of a DoG image is isn't specified in the paper. Assumed to be the same as the lower indexed
 * scale image it was computed from.</li>
 * </ol>
 *
 * <p>
 * [1] Lowe, D. "Distinctive image features from scale-invariant keypoints". International Journal of
 * Computer Vision, 60, 2 (2004), pp.91--110.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SiftDetector {

	// conversion factor to go from pixel coordinate in current octave to input image
	double pixelScaleToInput;

	// edge detector threshold
	// In the paper this is (r+1)**2/r
	double edgeThreshold;

	/** Maximum number of features after combining results across all scales. if <= 0 then all are returned */
	public int maxFeaturesAll = -1;

	// all the found detections in a single octave
	protected DogArray<SiftPoint> detectionsAll = new DogArray<>(SiftPoint::new);

	// Computes image derivatives. used in edge rejection
	ImageConvolveSparse<GrayF32, ?> derivXX;
	ImageConvolveSparse<GrayF32, ?> derivXY;
	ImageConvolveSparse<GrayF32, ?> derivYY;

	// local scale space around the current scale image being processed
	GrayF32 dogLower;  // DoG image in lower scale
	GrayF32 dogTarget; // DoG image in target scale
	GrayF32 dogUpper;  // DoG image in upper scale
	double sigmaLower, sigmaTarget, sigmaUpper;

	// finds features from 2D intensity image
	private final @Getter NonMaxLimiter extractor;

	// Used to select features from the combined set when there are too many
	private final FeatureSelectLimitIntensity<SiftPoint> selectFeaturesAll;
	private final FastArray<SiftPoint> selectedAll = new FastArray<>(SiftPoint.class);

	/**
	 * Configures SIFT detector
	 *
	 * @param edgeR Threshold used to remove edge responses. Larger values means its less strict. Try 10
	 * @param extractor Spatial feature detector that can be configured to limit the number of detected features in each scale.
	 */
	public SiftDetector( FeatureSelectLimitIntensity<ScalePoint> selectFeaturesAll,
						 double edgeR,
						 NonMaxLimiter extractor ) {
		if (!extractor.getNonmax().canDetectMaximums() || !extractor.getNonmax().canDetectMinimums())
			throw new IllegalArgumentException("The extractor must be able to detect maximums and minimums");
		if (edgeR < 1) {
			throw new IllegalArgumentException("R must be >= 1");
		}

		if (extractor.getNonmax().getIgnoreBorder() != 1) {
			throw new RuntimeException("Non-max should have an ignore border of 1");
		}

		this.extractor = extractor;

		this.edgeThreshold = (edgeR + 1)*(edgeR + 1)/edgeR;
		this.selectFeaturesAll = (FeatureSelectLimitIntensity)selectFeaturesAll;
		selectFeaturesAll.setSampler(new SampleIntensityScalePoint());

		createSparseDerivatives();
	}

	/**
	 * Define sparse image derivative operators.
	 */
	private void createSparseDerivatives() {
		Kernel1D_F32 kernelD = new Kernel1D_F32(new float[]{-1, 0, 1}, 3);

		Kernel1D_F32 kernelDD = KernelMath.convolve1D_F32(kernelD, kernelD);
		Kernel2D_F32 kernelXY = KernelMath.convolve2D(kernelD, kernelD);

		derivXX = FactoryConvolveSparse.horizontal1D(GrayF32.class, kernelDD);
		derivXY = FactoryConvolveSparse.convolve2D(GrayF32.class, kernelXY);
		derivYY = FactoryConvolveSparse.vertical1D(GrayF32.class, kernelDD);

		ImageBorder<GrayF32> border = FactoryImageBorder.single(BorderType.EXTENDED, GrayF32.class);

		derivXX.setImageBorder(border);
		derivXY.setImageBorder(border);
		derivYY.setImageBorder(border);
	}

	/**
	 * Detects SIFT features inside the input image
	 *
	 * @param scaleSpace (Input) Precomputed scale space. Not modified.
	 */
	public void process( SiftScaleSpace scaleSpace ) {
		detectionsAll.reset();
		selectedAll.reset();

		for (int octaveIdx = 0; octaveIdx < scaleSpace.octaves.length; octaveIdx++) {
			if (scaleSpace.isOctaveTooSmall(octaveIdx))
				break;
			int octave = octaveIdx + scaleSpace.firstOctave;
			SiftScaleSpace.Octave o = scaleSpace.octaves[octaveIdx];

			// scale from octave to input image
			pixelScaleToInput = scaleSpace.pixelScaleCurrentToInput(octave);

			// detect features in the image
			for (int scaleIdx = 1; scaleIdx < scaleSpace.getNumScales() + 1; scaleIdx++) {
				// not really sure how to compute the scale for features found at a particular DoG image
				// using the average resulted in less visually appealing circles in a test image
				sigmaLower = scaleSpace.computeSigmaScale(octave, scaleIdx - 1);
				sigmaTarget = scaleSpace.computeSigmaScale(octave, scaleIdx);
				sigmaUpper = scaleSpace.computeSigmaScale(octave, scaleIdx + 1);

				// grab the local DoG scale space images
				dogLower = o.differenceOfGaussian[scaleIdx - 1];
				dogTarget = o.differenceOfGaussian[scaleIdx];
				dogUpper = o.differenceOfGaussian[scaleIdx + 1];

				detectFeatures(octaveIdx, scaleIdx);
			}
		}

		if (maxFeaturesAll > 0)
			selectFeaturesAll.select(null, scaleSpace.getOriginalWidth(), scaleSpace.getOriginalHeight(),
					true, null, detectionsAll, maxFeaturesAll, selectedAll);
	}

	/**
	 * Detect features inside the Difference-of-Gaussian image at the current scale
	 *
	 * @param scaleIndex Which scale in the octave is it detecting features inside up.
	 * Primarily provided here for use in child classes.
	 */
	protected void detectFeatures( int octaveIndex, int scaleIndex ) {
		extractor.process(dogTarget);
		FastAccess<NonMaxLimiter.LocalExtreme> found = extractor.getFeatures();

		derivXX.setImage(dogTarget);
		derivXY.setImage(dogTarget);
		derivYY.setImage(dogTarget);

		for (int i = 0; i < found.size; i++) {
			NonMaxLimiter.LocalExtreme e = found.get(i);

			if (isScaleSpaceExtremum(e.location.x, e.location.y, e.intensity, e.max ? 1f : -1f)) {
				if (isEdge(e.location.x, e.location.y))
					continue;

				SiftPoint p = createDetection(e.location.x, e.location.y, e.intensity, e.max);
				p.octaveIdx = (byte)octaveIndex;
				p.scaleIdx = (byte)scaleIndex;
			}
		}
	}

	/**
	 * See if the point is a local extremum in scale-space above and below.
	 *
	 * @param c_x x-coordinate of extremum
	 * @param c_y y-coordinate of extremum
	 * @param positiveIntensity The maximum value it is checking. (positive value)
	 * @param signAdj Adjust the sign so that it can check for maximums
	 * @return true if its a local extremum
	 */
	boolean isScaleSpaceExtremum( int c_x, int c_y, float positiveIntensity, float signAdj ) {
		if (c_x <= 1 || c_y <= 1 || c_x >= dogLower.width - 1 || c_y >= dogLower.height - 1)
			return false;

		float v;

		for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				v = dogLower.unsafe_get(c_x + x, c_y + y);
				if (v*signAdj >= positiveIntensity)
					return false;
				v = dogUpper.unsafe_get(c_x + x, c_y + y);
				if (v*signAdj >= positiveIntensity)
					return false;
			}
		}

		return true;
	}

	/**
	 * Examines a local spatial extremum and interpolates its coordinates using a quadratic function. Very first
	 * thing it does is check to see if the feature is really an edge/false positive. After that interpolates
	 * the coordinate independently using a quadratic function along each axis. Resulting coordinate will be
	 * in the image image's coordinate system.
	 *
	 * @param x x-coordinate of extremum
	 * @param y y-coordinate of extremum
	 * @param positiveIntensity value of the extremum. Positive intensity value.
	 * @param maximum true if it was a maximum
	 */
	protected SiftPoint createDetection( int x, int y, float positiveIntensity, boolean maximum ) {
		// Estimate the scale and 2D point by fitting 2nd order polynomials
		// This is different from the original paper
		float signAdj = maximum ? 1 : -1;

		float x0 = dogTarget.unsafe_get(x - 1, y)*signAdj;
		float x2 = dogTarget.unsafe_get(x + 1, y)*signAdj;
		float y0 = dogTarget.unsafe_get(x, y - 1)*signAdj;
		float y2 = dogTarget.unsafe_get(x, y + 1)*signAdj;

		float s0 = dogLower.unsafe_get(x, y)*signAdj;
		float s2 = dogUpper.unsafe_get(x, y)*signAdj;

		SiftPoint p = detectionsAll.grow();

		// Compute the interpolated coordinate of the point in the original image coordinates
		p.pixel.x = pixelScaleToInput*(x + polyPeak(x0, positiveIntensity, x2));
		p.pixel.y = pixelScaleToInput*(y + polyPeak(y0, positiveIntensity, y2));

		// find the peak then do bilinear interpolate between the two appropriate sigmas
		double sigmaInterp = polyPeak(s0, positiveIntensity, s2); // scaled from -1 to 1
		if (sigmaInterp < 0) {
			p.scale = sigmaLower*-sigmaInterp + (1 + sigmaInterp)*sigmaTarget;
		} else {
			p.scale = sigmaUpper*sigmaInterp + (1 - sigmaInterp)*sigmaTarget;
		}

		// a maximum corresponds to a dark object and a minimum to a whiter object
		p.white = !maximum;

		// Set the intensity to a positive value
		p.intensity = positiveIntensity;

		return p;
	}

	/**
	 * Performs an edge test to remove false positives. See 4.1 in [1].
	 */
	boolean isEdge( int x, int y ) {
		if (edgeThreshold <= 0)
			return false;

		double xx = derivXX.compute(x, y);
		double xy = derivXY.compute(x, y);
		double yy = derivYY.compute(x, y);

		double Tr = xx + yy;
		double det = xx*yy - xy*xy;

		// Paper quite "In the unlikely event that the determinant is negative, the curvatures have different signs
		// so the point is discarded as not being an extremum"
		if (det <= 0)
			return true;
		else {
			// In paper this is:
			// Tr**2/Det < (r+1)**2/r
			return Tr*Tr >= edgeThreshold*det;
		}
	}

	/**
	 * All the found and selected detections across scale space
	 */
	public List<SiftPoint> getDetections() {
		return (maxFeaturesAll > 0 ? selectedAll : detectionsAll).toList();
	}

	/**
	 * Adds information about the scale space it was detected in for quick reference when
	 * computing the descriptor
	 */
	public static class SiftPoint extends ScalePoint {
		/** Which octave the point was detected in */
		public byte octaveIdx;
		/** The scale the point was detected in */
		public byte scaleIdx;
	}
}
