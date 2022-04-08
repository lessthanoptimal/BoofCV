/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.calibration.ScoreCalibrationBorderFill;
import boofcv.alg.geo.calibration.ScoreCalibrationInnerFill;
import boofcv.alg.geo.calibration.cameras.Zhang99Camera;
import boofcv.alg.geo.calibration.cameras.Zhang99CameraBrown;
import boofcv.alg.geo.calibration.cameras.Zhang99CameraKannalaBrandt;
import boofcv.alg.geo.calibration.cameras.Zhang99CameraUniversalOmni;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraModel;
import georegression.struct.point.Point2D_F64;
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
 * <li>{@link #reset}</li>
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

	// how the points are laid out
	protected List<Point2D_F64> layout;

	// computes calibration parameters
	protected CalibrationPlanarGridZhang99 zhang99;

	// computed parameters
	protected SceneStructureMetric structure;
	protected CameraModel foundIntrinsic;

	// Information on calibration targets and results
	protected @Getter List<CalibrationObservation> observations = new ArrayList<>();
	protected @Getter List<ImageResults> errors = new ArrayList<>();

	public @Nullable PrintStream verbose = null;

	// shape of the image
	private int imageWidth;
	private int imageHeight;

	public CalibrateMonoPlanar( List<Point2D_F64> layout ) {
		this.layout = layout;
	}

	/**
	 * Specifies the calibration model.
	 */
	public void configure( boolean assumeZeroSkew, Zhang99Camera camera ) {
		zhang99 = new CalibrationPlanarGridZhang99(layout, camera);
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	public void configurePinhole( boolean assumeZeroSkew,
								  int numRadialParam,
								  boolean includeTangential ) {
		var camera = new Zhang99CameraBrown(layout, assumeZeroSkew, includeTangential, numRadialParam);
		zhang99 = new CalibrationPlanarGridZhang99(layout, camera);
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	public void configureUniversalOmni( boolean assumeZeroSkew,
										int numRadialParam,
										boolean includeTangential ) {
		zhang99 = new CalibrationPlanarGridZhang99(layout,
				new Zhang99CameraUniversalOmni(layout, assumeZeroSkew, includeTangential, numRadialParam));
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	public void configureKannalaBrandt( boolean assumeZeroSkew,
										int numSymmetric,
										int numAsymmetric ) {
		zhang99 = new CalibrationPlanarGridZhang99(layout,
				new Zhang99CameraKannalaBrandt(assumeZeroSkew, numSymmetric, numAsymmetric));
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	public void configureUniversalOmni( boolean assumeZeroSkew,
										int numRadialParam,
										boolean includeTangential,
										double mirrorOffset ) {
		zhang99 = new CalibrationPlanarGridZhang99(layout,
				new Zhang99CameraUniversalOmni(layout, assumeZeroSkew, includeTangential, numRadialParam, mirrorOffset));
		zhang99.setZeroSkew(assumeZeroSkew);
	}

	/**
	 * Resets internal data structures. Must call before adding images
	 */
	public void reset() {
		observations = new ArrayList<>();
		errors = new ArrayList<>();
		imageHeight = imageWidth = 0;
	}

	/**
	 * Adds the observations from a calibration target detector
	 *
	 * @param observation Detected calibration points
	 */
	public void addImage( CalibrationObservation observation ) {
		if (imageWidth == 0) {
			this.imageWidth = observation.getWidth();
			this.imageHeight = observation.getHeight();
		} else if (observation.getWidth() != this.imageWidth || observation.getHeight() != this.imageHeight) {
			throw new IllegalArgumentException("Image shape miss match. Are these all from the same camera? " +
					imageWidth + "x" + imageHeight + " vs " + observation.getWidth() + "x" + observation.getHeight());
		}
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
		if (zhang99 == null)
			throw new IllegalArgumentException("Please call configure first.");
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

	public String computeQualityText( List<String> imageNames ) {
		var quality = new CalibrationQuality();
		computeQuality(foundIntrinsic, observations, quality);
		return computeQualityText(errors, imageNames, quality);
	}

	/** Creates human-readable text with metrics that indicate calibration quality */
	public static String computeQualityText( List<ImageResults> errors,
											 List<String> imageNames,
											 CalibrationQuality quality ) {
		BoofMiscOps.checkEq(errors.size(), imageNames.size());

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
		text += "\n";
		text += String.format("Reprojection Errors (px):\nmean=%.3f max=%.3f\n\n", averageError, maxError);
		text += String.format("%-10s | %8s\n", "image", "max (px)");
		for (int i = 0; i < imageNames.size(); i++) {
			String image = imageNames.get(i);
			ImageResults r = errors.get(i);
			text += String.format("%-12s %8.3f\n", new File(image).getName(), r.maxError);
		}
		return text;
	}

	/**
	 * Computes quality metrics to quantify how good of a job the person calibrating did
	 */
	public static void computeQuality( CameraModel intrinsic,
									   List<CalibrationObservation> observations,
									   CalibrationQuality quality ) {
		var border = new ScoreCalibrationBorderFill();
		var inner = new ScoreCalibrationInnerFill();

		border.initialize(intrinsic.width, intrinsic.height);
		inner.initialize(intrinsic.width, intrinsic.height);

		for (int i = 0; i < observations.size(); i++) {
			border.add(observations.get(i));
			inner.add(observations.get(i));
		}

		quality.borderFill = border.getScore();
		quality.innerFill = inner.getScore();
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

	public void setRobust( boolean robust ) {
		this.zhang99.setRobust(robust);
	}

	public SceneStructureMetric getStructure() {
		return structure;
	}

	public <T extends CameraModel> T getIntrinsic() {
		return (T)foundIntrinsic;
	}

	public CalibrationPlanarGridZhang99 getZhang99() {
		return zhang99;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
