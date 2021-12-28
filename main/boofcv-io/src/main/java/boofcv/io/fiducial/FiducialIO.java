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

package boofcv.io.fiducial;

import boofcv.BoofVersion;
import boofcv.io.UtilIO;
import georegression.struct.point.Point2D_F64;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.util.*;

import static boofcv.io.calibration.CalibrationIO.createYmlObject;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * File IO for fiducials. Typically these functions are used to read definition files.
 *
 * @author Peter Abeles
 */
public class FiducialIO {

	/**
	 * See {@link #saveRandomDotYaml(RandomDotDefinition, Writer)} but to a file.
	 */
	public static void saveRandomDotYaml(RandomDotDefinition definition, File file ) {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
			saveRandomDotYaml(definition,writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a Uchiya definition in BoofCV YAML format.
	 *
	 * @param definition Definition to be saved.
	 * @param outputWriter Stream writer
	 */
	public static void saveRandomDotYaml(RandomDotDefinition definition, Writer outputWriter ) {
		Map<String,Object> map = new HashMap<>();
		map.put("version",1); // not used, but there for the future
		map.put("random_seed",definition.randomSeed);
		if( definition.dotDiameter > 0 )
			map.put("dot_diameter",definition.dotDiameter);
		if( definition.maxDotsPerMarker > 0 )
			map.put("num_dots",definition.maxDotsPerMarker);
		if( definition.markerWidth > 0 )
			map.put("marker_width",definition.markerWidth);
		if( definition.markerHeight > 0 )
			map.put("marker_height",definition.markerHeight);
		if( !definition.units.isEmpty() )
			map.put("units",definition.units);

		List<List<double[]>> listMarkers = new ArrayList<>();
		for( var marker : definition.markers ) {
			List<double[]> listPoints = new ArrayList<>();
			for( var p : marker ) {
				listPoints.add( new double[]{p.x,p.y});
			}
			listMarkers.add(listPoints);
		}
		map.put("markers",listMarkers);

		// Create output file
		PrintWriter out = new PrintWriter(outputWriter);
		out.println("# Description of printed UCHIYA markers created by BoofCV "+ BoofVersion.VERSION);
		out.println("# Warning: Individual markers might have less than num_dots points");
		out.println();

		// save markers in yaml file
		createYmlObject().dump(map,out);
	}

	public static RandomDotDefinition loadRandomDotYaml(URL url) {
		try {
			InputStreamReader reader = new InputStreamReader(url.openStream(),UTF_8);
			RandomDotDefinition ret = loadRandomDotYaml(reader);
			reader.close();
			return ret;
		} catch( IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public static RandomDotDefinition loadRandomDotYaml(File file ) {
		URL url = UtilIO.ensureURL(file.getPath());
		if( url == null )
			throw new RuntimeException("Null url path. file="+file.getPath());
		return loadRandomDotYaml(url);
	}

	/**
	 * Loads a Uchiya definition file from a BoofCV yaml document
	 * @param reader Stream reader
	 * @return UchiyaDefinition
	 */
	public static RandomDotDefinition loadRandomDotYaml(Reader reader ) {
		Yaml yaml = createYmlObject();
		Map<String,Object> data = yaml.load(reader);

		var def = new RandomDotDefinition();
		if( data.containsKey("random_seed"))
			def.randomSeed = ((Number)data.get("random_seed")).longValue(); // YAML only library only supports ints?
		if( data.containsKey("dot_diameter"))
			def.dotDiameter = (double) data.get("dot_diameter");
		if( data.containsKey("num_dots"))
			def.maxDotsPerMarker = (int) data.get("num_dots");
		if( data.containsKey("marker_width"))
			def.markerWidth = (double) data.get("marker_width");
		if( data.containsKey("marker_height"))
			def.markerHeight = (double) data.get("marker_height");
		if( data.containsKey("units"))
			def.units = (String) data.get("units");

		// assume it's square if height is not specified
		if( def.markerHeight <= 0)
			def.markerHeight = def.markerWidth;

		List<List<List<Double>>> listMarkers = (List<List<List<Double>>>)Objects.requireNonNull(data.get("markers"));

		for (int markerIdx = 0; markerIdx < listMarkers.size(); markerIdx++) {
			List<List<Double>> listYaml = listMarkers.get(markerIdx);
			List<Point2D_F64> marker = new ArrayList<>(listYaml.size());

			for( List<Double> coordinates : listYaml ) {
				var p = new Point2D_F64(coordinates.get(0), coordinates.get(1));
				marker.add( p );
			}

			def.markers.add(marker);
		}

		return def;
	}
}
