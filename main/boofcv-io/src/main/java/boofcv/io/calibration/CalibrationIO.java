/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.io.UtilIO;
import boofcv.struct.calib.*;
import georegression.struct.se.Se3_F64;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public static String MODEL_PINHOLE_RADIAL_TAN = "pinhole_radial_tangential";
	public static String MODEL_OMNIDIRECTIONAL_UNIVERSAL = "omnidirectional_universal";
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
	public static <T extends CameraPinhole> void save( T parameters, Writer outputWriter ) {
		PrintWriter out = new PrintWriter(outputWriter);

		Yaml yaml = createYmlObject();

		Map<String, Object> data = new HashMap<>();

		if (parameters instanceof CameraPinholeBrown) {
			out.println("# Pinhole camera model with radial and tangential distortion");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println("# radial = radial distortion, (t1,t2) = tangential distortion");
			out.println();
			putModelRadial((CameraPinholeBrown)parameters, data);
		} else if (parameters instanceof CameraUniversalOmni) {
			out.println("# Omnidirectional camera model with radial and tangential distortion");
			out.println("# C. Mei, and P. Rives. \"Single view point omnidirectional camera calibration" +
					" from planar grids.\"  ICRA 2007");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println("# mirror_offset = offset mirror along z-axis in unit circle");
			out.println("# radial = radial distortion, (t1,t2) = tangential distortion");
			out.println();
			putModelUniversalOmni((CameraUniversalOmni)parameters, data);
		} else {

			out.println("# Pinhole camera model");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println();
			putModelPinhole(parameters, data);
		}

		yaml.dump(data, out);

		out.close();
	}

	public static <T extends CameraPinhole> void save( T parameters, String filePath ) {
		try {
			save(parameters, new OutputStreamWriter(new FileOutputStream(filePath), UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T extends CameraPinhole> void save( T parameters, File filePath ) {
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
		map.put("left", putModelRadial(parameters.left, null));
		map.put("right", putModelRadial(parameters.right, null));
		map.put("rightToLeft", putSe3(parameters.rightToLeft));

		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# Intrinsic and extrinsic parameters for a stereo camera pair");
		Yaml yaml = createYmlObject();
		yaml.dump(map, out);
		out.close();
	}

	public static void save( StereoParameters parameters, String outputPath ) {
		try {
			save(parameters, new OutputStreamWriter(new FileOutputStream(outputPath), UTF_8));
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
		try {
			save(rigidBody, new OutputStreamWriter(new FileOutputStream(outputPath), UTF_8));
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
		out.close();
	}

	public static void save( VisualDepthParameters parameters, File filePath ) {
		save(parameters, filePath.getPath());
	}

	public static void save( VisualDepthParameters parameters, String outputPath ) {
		try {
			save(parameters, new OutputStreamWriter(new FileOutputStream(outputPath), UTF_8));
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
		map.put("intrinsic", putModelRadial(parameters.getVisualParam(), null));

		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# RGB Depth Camera Calibration");
		Yaml yaml = createYmlObject();
		yaml.dump(map, out);
		out.close();
	}

	public static void save( MonoPlaneParameters parameters, Writer outputWriter ) {
		Map<String, Object> map = new HashMap<>();
		map.put("model", MODEL_MONO_PLANE);
		map.put(VERSION, 0);
		map.put("intrinsic", putModelRadial(parameters.getIntrinsic(), null));
		map.put("plane_to_camera", putSe3(parameters.getPlaneToCamera()));

		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# Monocular Camera with Known Plane Distance");
		Yaml yaml = createYmlObject();
		yaml.dump(map, out);
		out.close();
	}

	public static <T> T load( @Nullable URL path ) {
		if (path == null)
			throw new RuntimeException("Null path");
		try {
			return load(new InputStreamReader(path.openStream(), UTF_8));
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
			reader.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return load(data);
	}

	private static <T> T load( Map<String, Object> data ) {

//		int version = data.containsKey("version") ? (int)data.get("version") : 0;

		String model = (String)data.get("model");
		if (model == null)
			throw new RuntimeException("Missing model parameter");

		if (model.equals(MODEL_PINHOLE)) {
			CameraPinhole parameters = new CameraPinhole();
			loadPinhole(getOrThrow(data, "pinhole"), parameters);

			return (T)parameters;
		} else if (model.equals(MODEL_PINHOLE_RADIAL_TAN)) {
			CameraPinholeBrown parameters = new CameraPinholeBrown();

			loadPinhole((Map<String, Object>)data.get("pinhole"), parameters);

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
		} else if (model.equals(MODEL_STEREO)) {
			StereoParameters parameters = new StereoParameters();
			parameters.left = load((Map<String, Object>)getOrThrow(data, "left"));
			parameters.right = load((Map<String, Object>)getOrThrow(data, "right"));
			parameters.rightToLeft = loadSe3(getOrThrow(data, "rightToLeft"), null);
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

	private static Map<String, Object> putModelPinhole( CameraPinhole parameters, Map<String, Object> map ) {
		if (map == null)
			map = new HashMap<>();

		map.put("model", MODEL_PINHOLE);
		map.put(VERSION, 0);
		map.put("pinhole", putParamsPinhole(parameters));

		return map;
	}

	private static Map<String, Object> putModelRadial( CameraPinholeBrown parameters, Map<String, Object> map ) {
		if (map == null)
			map = new HashMap<>();

		map.put("model", MODEL_PINHOLE_RADIAL_TAN);
		map.put(VERSION, 0);
		map.put("pinhole", putParamsPinhole(parameters));
		map.put("radial_tangential", putParamsRadialTangent(parameters));

		return map;
	}

	private static Map<String, Object> putModelUniversalOmni( CameraUniversalOmni parameters, Map<String, Object> map ) {
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
		parameters.width = getOrThrow(map, "width");
		parameters.height = getOrThrow(map, "height");
		parameters.fx = getOrThrow(map, "fx");
		parameters.fy = getOrThrow(map, "fy");
		parameters.skew = getOrThrow(map, "skew");
		parameters.cx = getOrThrow(map, "cx");
		parameters.cy = getOrThrow(map, "cy");
	}

	public static Se3_F64 loadSe3( Map<String, Object> map, Se3_F64 transform ) {
		if (transform == null)
			transform = new Se3_F64();
		List<Double> rotation = getOrThrow(map, "rotation");

		transform.T.x = getOrThrow(map, "x");
		transform.T.y = getOrThrow(map, "y");
		transform.T.z = getOrThrow(map, "z");

		for (int i = 0; i < 9; i++) {
			transform.R.data[i] = rotation.get(i);
		}
		return transform;
	}
}
