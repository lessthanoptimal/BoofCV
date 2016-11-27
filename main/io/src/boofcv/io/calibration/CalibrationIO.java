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

package boofcv.io.calibration;

import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.calib.StereoParameters;
import georegression.struct.se.Se3_F64;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Functions for loading and saving camera calibration related data structures from/to disk
 *
 * @author Peter Abeles
 */
public class CalibrationIO {
	public static String MODEL_PINHOLE = "pinhole";
	public static String MODEL_PINHOLE_RADIAL_TAN = "pinhole_radial_tangential";
	public static String MODEL_OMNIDIRECTIONAL_UNIVERSAL = "omnidirectional_universal";
	public static String MODEL_STEREO = "stereo_camera";

	/**
	 * Saves intrinsic camera model to disk
	 *
	 * @param parameters Camera parameters
	 * @param outputWriter Path to where it should be saved
	 */
	public static <T extends CameraPinhole> void save(T parameters , Writer outputWriter ) {
		PrintWriter out = new PrintWriter(outputWriter);

		Yaml yaml = createYmlObject();

		Map<String, Object> data = new HashMap<>();

		if( parameters instanceof CameraPinholeRadial) {
			out.println("# Pinhole camera model with radial and tangential distortion");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println("# radial = radial distortion, (t1,t2) = tangential distortion");
			out.println();
			putModelRadial((CameraPinholeRadial) parameters, data);
		} else if( parameters instanceof CameraUniversalOmni ) {
			out.println("# Omnidirectional camera model with radial and tangential distortion");
			out.println("# C. Mei, and P. Rives. \"Single view point omnidirectional camera calibration" +
					" from planar grids.\"  ICRA 2007");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println("# mirror_offset = offset mirror along z-axis in unit circle");
			out.println("# radial = radial distortion, (t1,t2) = tangential distortion");
			out.println();
			putModelUniversalOmni((CameraUniversalOmni) parameters, data);
		} else {

			out.println("# Pinhole camera model");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println();
			putModelPinhole(parameters,data);
		}

		yaml.dump(data,out);

		out.close();
	}

	public static <T extends CameraPinhole> void save(T parameters , String filePath ) {
		try {
			save(parameters, new FileWriter(filePath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T extends CameraPinhole> void save(T parameters , File filePath ) {
		save(parameters, filePath.getPath());
	}

	private static Yaml createYmlObject() {
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
	public static void save(StereoParameters parameters , Writer outputWriter ) {

		Map<String, Object> data = new HashMap<>();
		data.put("model",MODEL_STEREO);
		data.put("left",putModelRadial(parameters.left,null));
		data.put("right",putModelRadial(parameters.right,null));
		data.put("rightToLeft",putSe3(parameters.rightToLeft));

		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# Intrinsic and extrinsic parameters for a stereo camera pair");
		Yaml yaml = createYmlObject();
		yaml.dump(data,out);

	}

	public static void save(StereoParameters parameters , String outputPath ) {
		try {
			save(parameters,new FileWriter(outputPath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void save(StereoParameters parameters , File filePath ) {
		save(parameters, filePath.getPath());
	}

	public static <T> T load(URL path ) {
		try {
			return load( new InputStreamReader(path.openStream()) );
		} catch (IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T load(File path ) {
		try {
			return load( new FileReader(path));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T load(String path ) {
		try {
			return load( new FileReader(path));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Loads intrinsic parameters from disk
	 * @param reader Reader
	 * @return Camera model
	 */
	public static <T> T load(Reader reader ) {
		Yaml yaml = createYmlObject();

		Map<String,Object> data = (Map<String, Object>) yaml.load(reader);

		try {
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return load(data);
	}

	private static <T> T load(Map<String, Object> data) {
		String model = (String)data.get("model");

		if( model.equals(MODEL_PINHOLE)) {
			CameraPinhole parameters = new CameraPinhole();
			loadPinhole((Map<String,Object> )data.get("pinhole"),parameters);

			return (T)parameters;
		} else if( model.equals(MODEL_PINHOLE_RADIAL_TAN) ) {
			CameraPinholeRadial parameters = new CameraPinholeRadial();

			loadPinhole((Map<String, Object>) data.get("pinhole"), parameters);

			Map<String, Object> distortion = (Map<String, Object>) data.get("radial_tangential");
			if( distortion.containsKey("radial") ) {
				List<Double> list = (List<Double>) distortion.get("radial");
				if( list != null ) {
					parameters.radial = new double[list.size()];
					for (int i = 0; i < list.size(); i++) {
						parameters.radial[i] = list.get(i);
					}
				}
			}
			if( distortion.containsKey("t1"))
				parameters.t1 = (double) distortion.get("t1");
			if( distortion.containsKey("t2"))
				parameters.t2 = (double) distortion.get("t2");

			return (T) parameters;
		} else if( model.equals(MODEL_OMNIDIRECTIONAL_UNIVERSAL) ) {
			CameraUniversalOmni parameters = new CameraUniversalOmni(0);

			loadPinhole((Map<String, Object>) data.get("pinhole"), parameters);
			parameters.mirrorOffset = (double)data.get("mirror_offset");

			Map<String, Object> distortion = (Map<String, Object>) data.get("radial_tangential");
			if( distortion.containsKey("radial") ) {
				List<Double> list = (List<Double>) distortion.get("radial");
				if( list != null ) {
					parameters.radial = new double[list.size()];
					for (int i = 0; i < list.size(); i++) {
						parameters.radial[i] = list.get(i);
					}
				}
			}
			if( distortion.containsKey("t1"))
				parameters.t1 = (double) distortion.get("t1");
			if( distortion.containsKey("t2"))
				parameters.t2 = (double) distortion.get("t2");
			return (T)parameters;

		} else if( model.equals(MODEL_STEREO) ) {
			StereoParameters parameters = new StereoParameters();
			parameters.left = load((Map<String, Object>)data.get("left"));
			parameters.right = load((Map<String, Object>)data.get("right"));
			parameters.rightToLeft = loadSe3((Map<String, Object>)data.get("rightToLeft"),null);
			return (T) parameters;
		} else {
			throw new RuntimeException("Unknown camera model: "+model);
		}
	}


	private static Map<String,Object> putModelPinhole( CameraPinhole parameters , Map<String,Object> map ) {
		if( map == null )
			map = new HashMap<>();
		map.put("model",MODEL_PINHOLE);
		map.put("pinhole", putParamsPinhole(parameters));
		return map;
	}

	private static Map<String,Object> putModelRadial( CameraPinholeRadial parameters , Map<String,Object> map ) {
		if( map == null )
			map = new HashMap<>();

		map.put("model",MODEL_PINHOLE_RADIAL_TAN);
		map.put("pinhole", putParamsPinhole(parameters));
		map.put("radial_tangential", putParamsRadialTangent(parameters));

		return map;
	}

	private static Map<String,Object> putModelUniversalOmni( CameraUniversalOmni parameters , Map<String,Object> map ) {
		if( map == null )
			map = new HashMap<>();

		map.put("model",MODEL_OMNIDIRECTIONAL_UNIVERSAL);
		map.put("pinhole", putParamsPinhole(parameters));
		map.put("mirror_offset",parameters.mirrorOffset);

		Map<String,Object> mapDistort = new HashMap<>();

		if( parameters.radial != null )
			mapDistort.put("radial",parameters.radial);
		mapDistort.put("t1",parameters.t1);
		mapDistort.put("t2",parameters.t2);

		map.put("radial_tangential", mapDistort);

		return map;
	}

	private static Map<String,Object> putParamsPinhole(CameraPinhole parameters  ) {
		Map<String,Object> map = new HashMap<>();

		map.put("width",parameters.width);
		map.put("height",parameters.height);
		map.put("fx",parameters.fx);
		map.put("fy",parameters.fy);
		map.put("skew",parameters.skew);
		map.put("cx",parameters.cx);
		map.put("cy",parameters.cy);

		return map;
	}

	private static Map<String,Object> putParamsRadialTangent(CameraPinholeRadial parameters ) {
		Map<String,Object> map = new HashMap<>();

		if( parameters.radial != null )
			map.put("radial",parameters.radial);
		map.put("t1",parameters.t1);
		map.put("t2",parameters.t2);

		return map;
	}

	private static Map<String,Object> putSe3( Se3_F64 transform ) {
		Map<String,Object> map = new HashMap<>();

		map.put("rotation",transform.R.data);
		map.put("x",transform.T.x);
		map.put("y",transform.T.y);
		map.put("z",transform.T.z);

		return map;
	}

	private static void loadPinhole(Map<String,Object> map , CameraPinhole parameters ) {
		parameters.width = (int)map.get("width");
		parameters.height = (int)map.get("height");
		parameters.fx = (double)map.get("fx");
		parameters.fy = (double)map.get("fy");
		parameters.skew = (double)map.get("skew");
		parameters.cx = (double)map.get("cx");
		parameters.cy = (double)map.get("cy");
	}

	private static Se3_F64 loadSe3(Map<String,Object> map , Se3_F64 transform) {
		if( transform == null )
			transform = new Se3_F64();
		List<Double> rotation = (List<Double>)map.get("rotation");

		transform.T.x = (double)map.get("x");
		transform.T.y = (double)map.get("y");
		transform.T.z = (double)map.get("z");

		for (int i = 0; i < 9; i++) {
			transform.R.data[i] = rotation.get(i);
		}
		return transform;
	}
}
