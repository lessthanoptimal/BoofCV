/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.calibration;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang99;
import boofcv.alg.geo.calibration.ScoreCalibrationFill;
import boofcv.alg.geo.calibration.ScoreCalibrationGeometricDiversity;
import boofcv.alg.geo.calibration.cameras.Zhang99Camera;
import boofcv.alg.geo.calibration.cameras.Zhang99CameraBrown;
import boofcv.alg.geo.calibration.cameras.Zhang99CameraKannalaBrandt;
import boofcv.alg.geo.calibration.cameras.Zhang99CameraUniversalOmni;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraModel;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Performs the full processing loop for calibrating a mono camera from a planar grid. A
 * directory is specified that the images are read in from. Calibration points are detected
 * inside the image and feed into the Zhang99 algorithm for parameter estimation.
 * </p>
 *
 * <p>
 * Internally it supports status updates for a GUI and skips over bad images. Invoke functions
 * in the following order:
 * <ol>
 * <li>{@link #configure}</li>
 * <li>{@link #initialize}</li>
 * <li>{@link #addImage}</li>
 * <li>{@link #process}</li>
 * <li>{@link #getIntrinsic}</li>
 * </ol>
 * </p>
 *
 * <p>
 * <b>Most 3D operations in BoofCV assume that the image coordinate system is right handed and the +Z axis is
 * pointing out of the camera.</b>  In standard image coordinate the origin (0,0) is at the top left corner with +x going
 * to the right and +y going down, then if it is right handed +z will be out of the image. <b>However some times
 * this pseudo standard is not followed and the y-axis needs to be inverted by setting isInverted to true.</b>
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CalibrateMonoPlanar implements VerbosePrint {

	// detects calibration points inside of images
	protected DetectSingleFiducialCalibration detector;

	/** Layout of points on each calibration target */
	protected List<List<Point2D_F64>> layouts;

	/** computes calibration parameters */
	@Getter protected CalibrationPlanarGridZhang99 zhang99;

	// computed parameters
	@Getter protected SceneStructureMetric structure;
	protected CameraModel foundIntrinsic;

	// Information on calibration targets and results
	protected @Getter List<CalibrationObservation> observations = new ArrayList<>();
	protected @Getter List<ImageResults> errors = new ArrayList<>();

	public @Nullable PrintStream verbose = null;

	// shape of the image
	private int imageWidth;
	private int imageHeight;

	/**
	 * Resets internal data structures. Must call before adding images
	 *
	 * @param width Image width
	 * @param height Image height
	 */
	public void initialize( int width, int height, List<List<Point2D_F64>> layouts ) {
		observations = new ArrayList<>();
		errors = new ArrayList<>();
		imageWidth = width;
		imageHeight = height;
		this.layouts = layouts;
	}

	/**
	 * Specifies the calibration model.
	 */
	public void configure( boolean assumeZeroSkew, Zhang99Camera camera ) {
		zhang99 = new CalibrationPlanarGridZhang99(camera);
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	public void configurePinhole( boolean assumeZeroSkew,
								  int numRadialParam,
								  boolean includeTangential ) {
		var camera = new Zhang99CameraBrown(assumeZeroSkew, includeTangential, numRadialParam);
		zhang99 = new CalibrationPlanarGridZhang99(camera);
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	public void configureUniversalOmni( boolean assumeZeroSkew,
										int numRadialParam,
										boolean includeTangential ) {
		zhang99 = new CalibrationPlanarGridZhang99(
				new Zhang99CameraUniversalOmni(assumeZeroSkew, includeTangential, numRadialParam));
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	public void configureKannalaBrandt( boolean assumeZeroSkew,
										int numSymmetric,
										int numAsymmetric ) {
		zhang99 = new CalibrationPlanarGridZhang99(
				new Zhang99CameraKannalaBrandt(assumeZeroSkew, numSymmetric, numAsymmetric));
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	public void configureUniversalOmni( boolean assumeZeroSkew,
										int numRadialParam,
										boolean includeTangential,
										double mirrorOffset ) {
		zhang99 = new CalibrationPlanarGridZhang99(
				new Zhang99CameraUniversalOmni(assumeZeroSkew, includeTangential, numRadialParam, mirrorOffset));
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	/** Convience function which returns true if the provided shape matches the expected image shape */
	public boolean isExpectedShape( int width, int height ) {
		return width == imageWidth && height == imageHeight;
	}

	/**
	 * Adds the observations from a calibration target detector.
	 *
	 * <p>Note: If you see two targets in one image then that image is treated as two image, one for each observations. </p>
	 *
	 * @param observation Detected calibration points
	 */
	public void addImage( CalibrationObservation observation ) {
		observations.add(observation);
	}

	/**
	 * Removes the most recently added image
	 */
	public void removeLatestImage() {
		observations.remove(observations.size() - 1);
	}

	/**
	 * After calibration points have been found this invokes the Zhang99 algorithm to
	 * estimate calibration parameters. Error statistics are also computed.
	 */
	public <T extends CameraModel> T process() {
		if (imageWidth == 0)
			throw new RuntimeException("Must call initialize() first");
		if (zhang99 == null)
			throw new IllegalArgumentException("Please call configure first.");
		zhang99.setLayouts(layouts);
		zhang99.setVerbose(verbose, null);
		if (!zhang99.process(observations)) {
			throw new RuntimeException("Zhang99 algorithm failed!");
		}

		structure = zhang99.getStructure();

		errors = zhang99.computeErrors();

		foundIntrinsic = zhang99.getCameraModel();
		foundIntrinsic.width = imageWidth;
		foundIntrinsic.height = imageHeight;

		return (T)foundIntrinsic;
	}

	/**
	 * Returns estimated transform from calibration target to camera view
	 */
	public Se3_F64 getTargetToView( int viewIdx ) {
		return structure.getParentToView(viewIdx);
	}

	public String computeQualityText( List<String> imageNames ) {
		var fillScore = new ScoreCalibrationFill();
		var quality = new CalibrationQuality();
		computeQuality(foundIntrinsic, fillScore, layouts, observations, quality);
		return computeQualityText(errors, imageNames, quality);
	}

	/** Creates human-readable text with metrics that indicate calibration quality */
	public static String computeQualityText( List<ImageResults> errors,
											 List<String> imageNames,
											 CalibrationQuality quality ) {
		BoofMiscOps.checkEq(errors.size(), imageNames.size());

		// Compute a histogram of how many observations have a residual error less than these values
		var summaryThresholds = new double[]{0.25, 0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 20.0};
		var counts = new int[summaryThresholds.length];
		int totalObservations = 0;
		for (int i = 0; i < imageNames.size(); i++) {
			ImageResults r = errors.get(i);
			totalObservations += r.pointError.length;
			for (int j = 0; j < r.pointError.length; j++) {
				double e = r.pointError[j];
				for (int iterThresh = summaryThresholds.length - 1; iterThresh >= 0; iterThresh--) {
					if (summaryThresholds[iterThresh] < e)
						break;
					counts[iterThresh]++;
				}
			}
		}

		double averageError = 0.0;
		double maxError = 0.0;
		for (int i = 0; i < imageNames.size(); i++) {
			ImageResults r = errors.get(i);
			averageError += r.meanError;
			maxError = Math.max(maxError, r.maxError);
		}
		averageError /= imageNames.size();
		String text = "";
		text += String.format("quality.fill_border  %5.1f%%\n", 100*quality.borderFill);
		text += String.format("quality.fill_inner   %5.1f%%\n", 100*quality.innerFill);
		text += String.format("quality.geometric    %5.1f%%\n", 100*quality.geometric);
		text += "\n";
		text += String.format("Reprojection Errors (px):\nmean=%.3f max=%.3f\n\n", averageError, maxError);

		var builder = new StringBuilder();
		generateReprojectionErrorHistogram(summaryThresholds, counts, totalObservations, builder);
		text += builder.toString();

		text += String.format("%-10s | %8s\n", "image", "max (px)");
		for (int i = 0; i < imageNames.size(); i++) {
			String image = imageNames.get(i);
			ImageResults r = errors.get(i);
			text += String.format("%-12s %8.3f\n", new File(image).getName(), r.maxError);
		}
		return text;
	}

	public static void generateReprojectionErrorHistogram( double[] thresholds, int[] counts,
														   int totalObservations,
														   StringBuilder builder ) {
		builder.append("Percent Reprojection Errors Less than X pixels. N=").append(totalObservations).append("\n");
		for (int i = 0; i < thresholds.length; i++) {
			builder.append(String.format(" %6.2f |", thresholds[i]));
		}
		builder.append("\n");
		for (int i = 0; i < counts.length; i++) {
			builder.append(String.format(" %5.1f%% |", 100.0*counts[i]/totalObservations));
		}
		builder.append("\n\n");
	}

	/**
	 * Computes quality metrics to quantify how good of a job the person calibrating did
	 *
	 * @param intrinsic Estimated camera model from calibration
	 * @param fillScorer Used to compute image fill score
	 * @param targetLayouts Known location of points in world coordinates
	 * @param observations Observed calibration points
	 * @param quality (Output) Metrics used to evaluate how good the calibration is
	 */
	public static void computeQuality( CameraModel intrinsic,
									   ScoreCalibrationFill fillScorer,
									   List<List<Point2D_F64>> targetLayouts,
									   List<CalibrationObservation> observations,
									   CalibrationQuality quality ) {
		fillScorer.initialize(intrinsic.width, intrinsic.height);
		var geoScorer = new ScoreCalibrationGeometricDiversity(true);

		for (int i = 0; i < observations.size(); i++) {
			CalibrationObservation obs = observations.get(i);
			fillScorer.addObservation(obs.points);
			geoScorer.addObservation(obs.points, targetLayouts.get(obs.target));
		}
		geoScorer.computeScore();

		quality.borderFill = fillScorer.getScoreBorder();
		quality.innerFill = fillScorer.getScoreInner();
		quality.geometric = geoScorer.getScore();
	}

	/**
	 * Prints out error information to standard out
	 */
	public static void printErrors( List<ImageResults> results, PrintStream out ) {
		double totalError = 0;
		for (int i = 0; i < results.size(); i++) {
			ImageResults r = results.get(i);
			totalError += r.meanError;

			out.printf("image %3d errors (px) mean=%7.1e max=%7.1e, bias: %8.1e %8.1e\n", i, r.meanError, r.maxError, r.biasX, r.biasY);
		}
		out.println("Average Mean Error = " + (totalError/results.size()));
	}

	public <T extends CameraModel> T getIntrinsic() {
		return (T)foundIntrinsic;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
