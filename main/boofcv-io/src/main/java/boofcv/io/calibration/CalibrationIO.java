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

package boofcv.io.calibration;

import boofcv.BoofVersion;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.*;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.se.Se3_F64;
import org.apache.commons.io.IOUtils;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static boofcv.misc.BoofMiscOps.getOrThrow;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Functions for loading and saving camera calibration related data structures from/to disk
 *
 * @author Peter Abeles
 */
@SuppressWarnings("ALL")
public class CalibrationIO {
	public static String MODEL_PINHOLE = "pinhole";
	public static String MODEL_BROWN = "pinhole_radial_tangential";
	public static String MODEL_OMNIDIRECTIONAL_UNIVERSAL = "omnidirectional_universal";
	public static String MODEL_KANNALA_BRANDT = "kannala_brandt";
	public static String MODEL_STEREO = "stereo_camera";
	public static String MODEL_RIGID_BODY = "rigid_body";
	public static String MODEL_VISUAL_DEPTH = "visual_depth";
	public static String MODEL_MONO_PLANE = "monocular_plane";

	public static String VERSION = "version";

	/**
	 * Saves intrinsic camera model to disk
	 *
	 * @param parameters Camera parameters
	 * @param outputWriter Path to where it should be saved
	 */
	public static <T extends CameraModel> void save( T parameters, Writer outputWriter ) {
		PrintWriter out = new PrintWriter(outputWriter);

		Yaml yaml = createYmlObject();

		Map<String, Object> data = new HashMap<>();

		if (parameters instanceof CameraPinholeBrown) {
			out.println("# Pinhole camera model with radial and tangential distortion");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println("# radial = radial distortion, (t1,t2) = tangential distortion");
			out.println();
			putModelBrown((CameraPinholeBrown)parameters, data);
		} else if (parameters instanceof CameraUniversalOmni) {
			out.println("# Omnidirectional camera model with radial and tangential distortion");
			out.println("# C. Mei, and P. Rives. \"Single view point omnidirectional camera calibration" +
					" from planar grids.\"  ICRA 2007");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println("# mirror_offset = offset mirror along z-axis in unit circle");
			out.println("# radial = radial distortion, (t1,t2) = tangential distortion");
			out.println();
			putModelUniversalOmni((CameraUniversalOmni)parameters, data);
		} else if (parameters instanceof CameraKannalaBrandt) {
			out.println("# A camera model for pinhole, wide angle, and fisheye cameras.");
			out.println("# Kannala, J., and Brandt, S. S. \"A generic camera model and calibration method for conventional,");
			out.println("# wide-angle, and fish-eye lenses.\" IEEE transactions on pattern analysis and machine intelligence, 2006");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println("# Everything else is coefficients for different types of distortion");
			out.println();
			putKannalaBrandt((CameraKannalaBrandt)parameters, data);
		} else {
			out.println("# Pinhole camera model");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println();
			putModelPinhole((CameraPinhole)parameters, data);
		}

		yaml.dump(data, out);
		out.flush();
	}

	public static <T extends CameraModel> void save( T parameters, String filePath ) {
		try (var stream = new FileOutputStream(filePath)) {
			save(parameters, new OutputStreamWriter(stream, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T extends CameraModel> void save( T parameters, File filePath ) {
		save(parameters, filePath.getPath());
	}

	public static Yaml createYmlObject() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return new Yaml(options);
	}

	/**
	 * Saves stereo camera model to disk
	 *
	 * @param parameters Camera parameters
	 * @param outputWriter Stream to save the parameters to
	 */
	public static void save( StereoParameters parameters, Writer outputWriter ) {
		Map<String, Object> map = new HashMap<>();
		map.put("model", MODEL_STEREO);
		map.put(VERSION, 0);
		map.put("left", putModelBrown(parameters.left, null));
		map.put("right", putModelBrown(parameters.right, null));
		map.put("rightToLeft", putSe3(parameters.right_to_left));

		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# Intrinsic and extrinsic parameters for a stereo camera pair");
		Yaml yaml = createYmlObject();
		yaml.dump(map, out);
		out.flush();
	}

	public static void save( StereoParameters parameters, String outputPath ) {
		try (var stream = new FileOutputStream(outputPath)) {
			save(parameters, new OutputStreamWriter(stream, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void save( StereoParameters parameters, File filePath ) {
		save(parameters, filePath.getPath());
	}

	public static void save( Se3_F64 rigidBody, File filePath ) {
		save(rigidBody, filePath.getPath());
	}

	public static void save( Se3_F64 rigidBody, String outputPath ) {
		try (var stream = new FileOutputStream(outputPath)) {
			save(rigidBody, new OutputStreamWriter(stream, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void save( Se3_F64 rigidBody, Writer outputWriter ) {
		Map<String, Object> map = new HashMap<>();
		map.put("model", MODEL_RIGID_BODY);
		map.put(VERSION, 0);
		map.put("parameters", putSe3(rigidBody));

		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# Rigid Body transformation");
		Yaml yaml = createYmlObject();
		yaml.dump(map, out);
		out.flush();
	}

	public static void save( VisualDepthParameters parameters, File filePath ) {
		save(parameters, filePath.getPath());
	}

	public static void save( VisualDepthParameters parameters, String outputPath ) {
		try (var stream = new FileOutputStream(outputPath)) {
			save(parameters, new OutputStreamWriter(stream, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void save( VisualDepthParameters parameters, Writer outputWriter ) {
		Map<String, Object> map = new HashMap<>();
		map.put("model", MODEL_VISUAL_DEPTH);
		map.put(VERSION, 0);
		map.put("max_depth", parameters.getMaxDepth());
		map.put("no_depth", parameters.getPixelNoDepth());
		map.put("intrinsic", putModelBrown(parameters.getVisualParam(), null));

		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# RGB Depth Camera Calibration");
		Yaml yaml = createYmlObject();
		yaml.dump(map, out);
		out.flush();
	}

	public static void save( MonoPlaneParameters parameters, Writer outputWriter ) {
		Map<String, Object> map = new HashMap<>();
		map.put("model", MODEL_MONO_PLANE);
		map.put(VERSION, 0);
		map.put("intrinsic", putModelBrown(parameters.getIntrinsic(), null));
		map.put("plane_to_camera", putSe3(parameters.getPlaneToCamera()));

		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# Monocular Camera with Known Plane Distance");
		Yaml yaml = createYmlObject();
		yaml.dump(map, out);
		out.flush();
	}

	public static <T> T load( @Nullable URL path ) {
		if (path == null)
			throw new RuntimeException("Null path");
		try (InputStream stream = path.openStream()) {
			return load(new InputStreamReader(stream, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T> T load( File path ) {
		URL url = UtilIO.ensureURL(path.getPath());
		if (url == null)
			throw new RuntimeException("Can't find " + path.getPath());
		return load(url);
	}

	public static <T> T load( String path ) {
		URL url = UtilIO.ensureURL(path);
		if (url == null)
			throw new RuntimeException("Can't find " + path);
		return load(url);
	}

	/**
	 * Loads intrinsic parameters from disk
	 *
	 * @param reader Reader
	 * @return Camera model
	 */
	public static <T> T load( Reader reader ) {
		Yaml yaml = createYmlObject();

		Map<String, Object> data = yaml.load(reader);

		try {
			return load(data);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T> T load( Map<String, Object> data ) throws IOException {
//		int version = data.containsKey("version") ? (int)data.get("version") : 0;

		String model = (String)data.get("model");
		if (model == null)
			throw new RuntimeException("Missing model parameter");

		if (model.equals(MODEL_PINHOLE)) {
			CameraPinhole parameters = new CameraPinhole();
			loadPinhole(getOrThrow(data, "pinhole"), parameters);

			return (T)parameters;
		} else if (model.equals(MODEL_BROWN)) {
			CameraPinholeBrown parameters = new CameraPinholeBrown();

			loadPinhole((Map<String, Object>)Objects.requireNonNull(data.get("pinhole")), parameters);

			Map<String, Object> distortion = getOrThrow(data, "radial_tangential");
			if (distortion.containsKey("radial")) {
				List<Double> list = (List<Double>)distortion.get("radial");
				if (list != null) {
					double[] radial = new double[list.size()];
					parameters.radial = radial;
					for (int i = 0; i < list.size(); i++) {
						radial[i] = list.get(i);
					}
				}
			}
			if (distortion.containsKey("t1"))
				parameters.t1 = (double)distortion.get("t1");
			if (distortion.containsKey("t2"))
				parameters.t2 = (double)distortion.get("t2");

			return (T)parameters;
		} else if (model.equals(MODEL_OMNIDIRECTIONAL_UNIVERSAL)) {
			CameraUniversalOmni parameters = new CameraUniversalOmni(0);

			loadPinhole(getOrThrow(data, "pinhole"), parameters);
			parameters.mirrorOffset = (double)data.get("mirror_offset");

			Map<String, Object> distortion = getOrThrow(data, "radial_tangential");
			if (distortion.containsKey("radial")) {
				List<Double> list = (List<Double>)distortion.get("radial");
				if (list != null) {
					parameters.radial = new double[list.size()];
					for (int i = 0; i < list.size(); i++) {
						parameters.radial[i] = list.get(i);
					}
				}
			}
			if (distortion.containsKey("t1"))
				parameters.t1 = (double)distortion.get("t1");
			if (distortion.containsKey("t2"))
				parameters.t2 = (double)distortion.get("t2");
			return (T)parameters;
		} else if (model.equals(MODEL_KANNALA_BRANDT)) {
			var parameters = new CameraKannalaBrandt();
			loadPinhole(getOrThrow(data, "pinhole"), parameters);
			parameters.fsetSymmetric(loadCoefficients(data, "symmetric"));
			parameters.fsetRadial(loadCoefficients(data, "radial"));
			parameters.fsetRadialTrig(loadCoefficients(data, "radial_trig"));
			parameters.fsetTangent(loadCoefficients(data, "tangent"));
			parameters.fsetTangentTrig(loadCoefficients(data, "tangent_trig"));
			return (T)parameters;
		} else if (model.equals(MODEL_STEREO)) {
			StereoParameters parameters = new StereoParameters();
			parameters.left = load((Map<String, Object>)getOrThrow(data, "left"));
			parameters.right = load((Map<String, Object>)getOrThrow(data, "right"));
			parameters.right_to_left = loadSe3(getOrThrow(data, "rightToLeft"), null);
			return (T)parameters;
		} else if (model.equals(MODEL_VISUAL_DEPTH)) {
			VisualDepthParameters parameters = new VisualDepthParameters();
			parameters.maxDepth = getOrThrow(data, "max_depth");
			parameters.pixelNoDepth = getOrThrow(data, "no_depth");
			parameters.visualParam = load((Map<String, Object>)getOrThrow(data, "intrinsic"));
			return (T)parameters;
		} else if (model.equals(MODEL_MONO_PLANE)) {
			MonoPlaneParameters parameters = new MonoPlaneParameters();
			parameters.intrinsic = load((Map<String, Object>)getOrThrow(data, "intrinsic"));
			parameters.planeToCamera = loadSe3(getOrThrow(data, "plane_to_camera"), null);
			return (T)parameters;
		} else if (model.equals(MODEL_RIGID_BODY)) {
			return (T)loadSe3(getOrThrow(data, "parameters"), null);
		} else {
			throw new RuntimeException("Unknown camera model: " + model);
		}
	}

	public static Map<String, Object> putModelPinhole( CameraPinhole parameters, Map<String, Object> map ) {
		if (map == null)
			map = new HashMap<>();

		map.put("model", MODEL_PINHOLE);
		map.put(VERSION, 0);
		map.put("pinhole", putParamsPinhole(parameters));

		return map;
	}

	public static Map<String, Object> putModelBrown( CameraPinholeBrown parameters,
													 @Nullable Map<String, Object> map ) {
		if (map == null)
			map = new HashMap<>();

		map.put("model", MODEL_BROWN);
		map.put(VERSION, 0);
		map.put("pinhole", putParamsPinhole(parameters));
		map.put("radial_tangential", putParamsRadialTangent(parameters));

		return map;
	}

	public static Map<String, Object> putModelUniversalOmni( CameraUniversalOmni parameters,
															 @Nullable Map<String, Object> map ) {
		if (map == null)
			map = new HashMap<>();

		map.put("model", MODEL_OMNIDIRECTIONAL_UNIVERSAL);
		map.put(VERSION, 0);
		map.put("pinhole", putParamsPinhole(parameters));
		map.put("mirror_offset", parameters.mirrorOffset);

		Map<String, Object> mapDistort = new HashMap<>();

		if (parameters.radial != null)
			mapDistort.put("radial", parameters.radial);
		mapDistort.put("t1", parameters.t1);
		mapDistort.put("t2", parameters.t2);

		map.put("radial_tangential", mapDistort);

		return map;
	}

	public static Map<String, Object> putKannalaBrandt( CameraKannalaBrandt parameters,
														@Nullable Map<String, Object> map ) {
		if (map == null)
			map = new HashMap<>();

		map.put("model", MODEL_KANNALA_BRANDT);
		map.put(VERSION, 0);
		map.put("pinhole", putParamsPinhole(parameters));

		map.put("symmetric", parameters.symmetric);
		map.put("radial", parameters.radial);
		map.put("radial_trig", parameters.radialTrig);
		map.put("tangent", parameters.tangent);
		map.put("tangent_trig", parameters.tangentTrig);

		return map;
	}

	public static Map<String, Object> putParamsPinhole( CameraPinhole parameters ) {
		Map<String, Object> map = new HashMap<>();

		map.put("width", parameters.width);
		map.put("height", parameters.height);
		map.put("fx", parameters.fx);
		map.put("fy", parameters.fy);
		map.put("skew", parameters.skew);
		map.put("cx", parameters.cx);
		map.put("cy", parameters.cy);

		return map;
	}

	public static Map<String, Object> putParamsRadialTangent( CameraPinholeBrown parameters ) {
		Map<String, Object> map = new HashMap<>();

		if (parameters.radial != null)
			map.put("radial", parameters.radial);
		map.put("t1", parameters.t1);
		map.put("t2", parameters.t2);

		return map;
	}

	public static Map<String, Object> putSe3( Se3_F64 transform ) {
		Map<String, Object> map = new HashMap<>();

		map.put("rotation", transform.R.data);
		map.put("x", transform.T.x);
		map.put("y", transform.T.y);
		map.put("z", transform.T.z);

		return map;
	}

	public static void loadPinhole( Map<String, Object> map, CameraPinhole parameters ) {
		try {
			parameters.width = getOrThrow(map, "width");
			parameters.height = getOrThrow(map, "height");
			parameters.fx = getOrThrow(map, "fx");
			parameters.fy = getOrThrow(map, "fy");
			parameters.skew = getOrThrow(map, "skew");
			parameters.cx = getOrThrow(map, "cx");
			parameters.cy = getOrThrow(map, "cy");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static double[] loadCoefficients( Map<String, Object> map, String name ) {
		if (!map.containsKey(name))
			return new double[0];
		List<Double> list = (List<Double>)map.get(name);
		double[] coefficients = new double[list.size()];

		for (int i = 0; i < list.size(); i++) {
			coefficients[i] = list.get(i);
		}
		return coefficients;
	}

	public static Se3_F64 loadSe3( Map<String, Object> map, @Nullable Se3_F64 transform ) {
		if (transform == null)
			transform = new Se3_F64();

		try {
			List<Double> rotation = getOrThrow(map, "rotation");

			transform.T.x = getOrThrow(map, "x");
			transform.T.y = getOrThrow(map, "y");
			transform.T.z = getOrThrow(map, "z");

			for (int i = 0; i < 9; i++) {
				transform.R.data[i] = rotation.get(i);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return transform;
	}

	public static void saveLandmarksCsv( String inputFile,
										 String detector,
										 CalibrationObservation landmarks,
										 File outputFile ) {
		try (var stream = new FileOutputStream(outputFile)) {
			saveLandmarksCsv(inputFile, detector, landmarks, stream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves detected landmarks from calibration in a CSV file format
	 *
	 * @param inputFile (Input) path to input image
	 * @param detector (Input) description of the detector
	 * @param landmarks (Input) detected landmarks
	 * @param outputStream (Output) where results are writetn to
	 */
	public static void saveLandmarksCsv( String inputFile,
										 String detector,
										 CalibrationObservation landmarks, OutputStream outputStream ) {
		var out = new PrintWriter(new OutputStreamWriter(outputStream, UTF_8));

		out.println("# Landmarks detected on a calibration target");
		out.println("# " + inputFile);
		out.println("# Image Shape: " + landmarks.getWidth() + " x " + landmarks.getHeight());
		out.println("# " + detector);
		out.println("# BoofCV Version: " + BoofVersion.VERSION);
		out.println("# BoofCV GITSHA: " + BoofVersion.GIT_SHA);
		out.println("# (landmark id), pixel-x, pixel-y");
		for (int i = 0; i < landmarks.size(); i++) {
			PointIndex2D_F64 p = landmarks.get(i);
			out.println(p.index + "," + p.p.x + "," + p.p.y);
		}
		out.flush();
	}

	/**
	 * Reads in a CSV that encodes {@link CalibrationObservation}.
	 *
	 * @param input The input stream containing the CSV file
	 * @return decoded observations
	 */
	public static CalibrationObservation loadLandmarksCsv( InputStream input ) {
		var ret = new CalibrationObservation();

		var buffer = new StringBuilder();
		try {
			while (true) {
				String line = UtilIO.readLine(input, buffer);
				if (line.isEmpty())
					break;
				if (line.startsWith("# Image Shape:")) {
					String[] words = line.split(" ");
					ret.width = Integer.parseInt(words[3]);
					ret.height = Integer.parseInt(words[5]);
					continue;
				} else if (line.startsWith("#")) {
					continue;
				}

				String[] words = line.split(",");
				BoofMiscOps.checkEq(3, words.length, "Expected 3 words: int, double, double");
				int which = Integer.parseInt(words[0]);
				double x = Double.parseDouble(words[1]);
				double y = Double.parseDouble(words[2]);
				ret.add(x, y, which);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return ret;
	}

	public static void saveOpencv( CameraPinholeBrown intrinsics, String path ) {
		try (var stream = new FileOutputStream(path)) {
			saveOpencv(intrinsics, new OutputStreamWriter(stream, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves the calibration in OpenCV yaml format.
	 *
	 * @param intrinsics (Input) Calibration that's to be saved
	 * @param outputWriter (Output) where to save it to
	 */
	public static void saveOpencv( CameraPinholeBrown intrinsics,
								   Writer outputWriter ) {
		PrintWriter out = new PrintWriter(outputWriter);

		// Snakeyaml isn't flexible enough. Just dump it manually
		try {
			Date date = Calendar.getInstance().getTime();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

			outputWriter.write("%YAML:1.0\n");
			outputWriter.write("calibration_time: \"" + dateFormat.format(date) + "\"\n");
			outputWriter.write("image_width: " + intrinsics.width + "\n");
			outputWriter.write("image_height: " + intrinsics.height + "\n");
			outputWriter.write("flags: 0\n");
			writeOpenCVMatrix(outputWriter, "camera_matrix", 3, 3,
					intrinsics.fx, intrinsics.skew, intrinsics.cx,
					0.0, intrinsics.fy, intrinsics.cy,
					0.0, 0.0, 1.0);

			double[] distortion = new double[5];
			if (intrinsics.radial != null) {
				if (intrinsics.radial.length > 0)
					distortion[0] = intrinsics.radial[0];
				if (intrinsics.radial.length > 1)
					distortion[1] = intrinsics.radial[01];
				if (intrinsics.radial.length > 2)
					distortion[4] = intrinsics.radial[2];
			}
			distortion[2] = intrinsics.t1;
			distortion[3] = intrinsics.t2;

			writeOpenCVMatrix(outputWriter, "distortion_coefficients", 5, 1, distortion);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		out.flush();
	}

	private static void writeOpenCVMatrix( Writer writer, String name, int rows, int cols, double... data ) throws IOException {
		writer.write(name + ": !!opencv-matrix\n");
		writer.write("    rows: " + rows + "\n");
		writer.write("    cols: " + cols + "\n");
		writer.write("    dt: d\n");
		writer.write("    data: [");
		for (int i = 0; i < data.length; i++) {
			writer.write(" " + data[i]);
			if (i + 1 != data.length)
				writer.write(",");
		}
		writer.write(" ]\n");
	}

	public static CameraPinholeBrown loadOpenCV( String path ) {
		URL url = UtilIO.ensureURL(path);
		if (url == null)
			throw new RuntimeException("Unknown path=" + path);
		return loadOpenCV(url);
	}

	public static CameraPinholeBrown loadOpenCV( URL path ) {
		try (InputStream stream = path.openStream()) {
			return loadOpenCV(new InputStreamReader(stream, UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Loads intrinsic parameters in OpenCV format.
	 */
	public static CameraPinholeBrown loadOpenCV( Reader reader ) {
		CameraPinholeBrown out = new CameraPinholeBrown();
		try {
			String filtered = IOUtils.toString(reader);
			// It doesn't like the header or that class def
			filtered = filtered.replace("%YAML:1.0", "");
			filtered = filtered.replace("!!opencv-matrix", "");

			Representer representer = new Representer();
			representer.getPropertyUtils().setSkipMissingProperties(true);

			Yaml yaml = new Yaml(new Constructor(), representer);
			Map<String, Object> map = yaml.load(filtered);

			int width = getOrThrow(map, "image_width");
			int height = getOrThrow(map, "image_height");

			DMatrixRMaj K = loadOpenCVMatrix(getOrThrow(map, "camera_matrix"));
			DMatrixRMaj distortion = loadOpenCVMatrix(getOrThrow(map, "distortion_coefficients"));

			PerspectiveOps.matrixToPinhole(K, width, height, out);

			if (distortion.getNumElements() >= 5)
				out.setRadial(distortion.get(0), distortion.get(1), distortion.get(4));
			else if (distortion.getNumElements() >= 2)
				out.setRadial(distortion.get(0), distortion.get(1));
			if (distortion.getNumElements() >= 5)
				out.fsetTangental(distortion.get(2), distortion.get(3));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return out;
	}

	private static DMatrixRMaj loadOpenCVMatrix( Map<String, Object> map ) throws IOException {
		int rows = getOrThrow(map, "rows");
		int cols = getOrThrow(map, "cols");

		var mat = new DMatrixRMaj(rows, cols);
		List<Double> array = getOrThrow(map, "data");
		for (int i = 0; i < array.size(); i++) {
			mat.data[i] = array.get(i);
		}
		return mat;
	}
}
