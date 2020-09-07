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

package boofcv.io.geo;

import boofcv.BoofVersion;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2;
import boofcv.alg.sfm.structure2.SceneWorkingGraph;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static boofcv.io.calibration.CalibrationIO.*;
import static boofcv.misc.BoofMiscOps.assertEq;
import static boofcv.misc.BoofMiscOps.getOrThrow;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * For loading and saving data structures related to multiview reconstruction.
 *
 * @author Peter Abeles
 */
public class MultiViewIO {

	public static void save( PairwiseImageGraph2 graph, String path ) {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(path), UTF_8);
			save(graph, writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a {@link PairwiseImageGraph2} into the {@link Writer}.
	 *
	 * @param graph (Input) The graph which is to be saved
	 * @param outputWriter (Output) where the graph is writen to
	 */
	public static void save( PairwiseImageGraph2 graph, Writer outputWriter ) {
		PrintWriter out = new PrintWriter(outputWriter);

		Yaml yaml = createYmlObject();

		out.println("# " + graph.getClass().getSimpleName() + " in YAML format. BoofCV " + BoofVersion.VERSION);

		List<Map<String, Object>> motions = new ArrayList<>();
		for (int motionIdx = 0; motionIdx < graph.edges.size; motionIdx++) {
			PairwiseImageGraph2.Motion pmotion = graph.edges.get(motionIdx);
			assertEq(pmotion.index, motionIdx);

			Map<String, Object> element = new HashMap<>();
			motions.add(element);
			element.put("count_f", pmotion.countF);
			element.put("count_h", pmotion.countH);
			element.put("is_3D", pmotion.is3D);
			element.put("src", pmotion.src.id);
			element.put("dst", pmotion.dst.id);
			element.put("F", pmotion.F.data);
			element.put("inliers", encodeInliers(pmotion.inliers));
		}

		List<Map<String, Object>> views = new ArrayList<>();
		for (int viewIdx = 0; viewIdx < graph.nodes.size; viewIdx++) {
			PairwiseImageGraph2.View pview = graph.nodes.get(viewIdx);

			List<Integer> connections = new ArrayList<>();
			pview.connections.forEach(( i, v ) -> connections.add(v.index));

			Map<String, Object> element = new HashMap<>();
			views.add(element);
			element.put("id", pview.id);
			element.put("total_observations", pview.totalObservations);
			element.put("connections", connections);
		}

		Map<String, Object> data = new HashMap<>();
		data.put("motions", motions);
		data.put("views", views);
		data.put("data_type", "PairwiseImageGraph");
		data.put("version", 0);

		yaml.dump(data, out);

		out.close();
	}

	private static List<Map<String, Object>> encodeInliers( FastAccess<AssociatedIndex> inliers ) {
		List<Map<String, Object>> encoded = new ArrayList<>();
		for (int i = 0; i < inliers.size; i++) {
			AssociatedIndex a = inliers.get(i);
			Map<String, Object> element = new HashMap<>();
			element.put("src", a.src);
			element.put("dst", a.dst);
			encoded.add(element);
		}
		return encoded;
	}

	public static PairwiseImageGraph2 load( String path, @Nullable PairwiseImageGraph2 graph ) {
		try {
			Reader reader = new InputStreamReader(new FileInputStream(path), UTF_8);
			return load(reader, graph);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Decodes {@link PairwiseImageGraph2} encoded in a YAML format from a reader.
	 *
	 * @param reader (Input/Output) Where the graph is read from
	 * @param graph (Output) Optional storage for the graph. If null a new instance is created.
	 * @return The decoded graph
	 */
	public static PairwiseImageGraph2 load( Reader reader, @Nullable PairwiseImageGraph2 graph ) {
		if (graph == null)
			graph = new PairwiseImageGraph2();
		else
			graph.reset();
		final var _graph = graph;

		Yaml yaml = createYmlObject();

		Map<String, Object> data = yaml.load(reader);
		try {
			reader.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		List<Map<String, Object>> yamlViews = getOrThrow(data, "views");
		List<Map<String, Object>> yamlMotions = getOrThrow(data, "motions");

		graph.nodes.resize(yamlViews.size());
		graph.edges.resize(yamlMotions.size());

		for (int i = 0; i < yamlViews.size(); i++) {
			Map<String, Object> yamlView = yamlViews.get(i);
			PairwiseImageGraph2.View v = graph.nodes.get(i);
			v.id = getOrThrow(yamlView, "id");
			v.totalObservations = getOrThrow(yamlView, "total_observations");

			List<Integer> yamlConnections = getOrThrow(yamlView, "connections");
			v.connections.resize(yamlConnections.size());
			v.connections.reset();
			yamlConnections.forEach(it -> v.connections.add(_graph.edges.get(it)));

			graph.mapNodes.put(v.id, v);
		}

		for (int i = 0; i < yamlMotions.size(); i++) {
			Map<String, Object> yamlMotion = yamlMotions.get(i);
			PairwiseImageGraph2.Motion m = graph.edges.get(i);

			m.countF = getOrThrow(yamlMotion, "count_f");
			m.countH = getOrThrow(yamlMotion, "count_h");
			m.is3D = getOrThrow(yamlMotion, "is_3D");
			m.src = getOrThrow(graph.mapNodes, getOrThrow(yamlMotion, "src"));
			m.dst = getOrThrow(graph.mapNodes, getOrThrow(yamlMotion, "dst"));
			m.index = i;
			copyIntoMatrix(getOrThrow(yamlMotion, "F"), m.F);
			decodeInliers(getOrThrow(yamlMotion, "inliers"), m.inliers);
		}

		return graph;
	}

	private static void copyIntoMatrix( List<Double> arrayData, DMatrixRMaj matrix ) {
		assertEq(arrayData.size(), matrix.data.length);
		for (int j = 0; j < matrix.data.length; j++) {
			matrix.data[j] = arrayData.get(j);
		}
	}

	private static void decodeInliers( List<Map<String, Object>> encoded, FastQueue<AssociatedIndex> inliers ) {
		inliers.resize(encoded.size());

		for (int i = 0; i < inliers.size; i++) {
			Map<String, Object> element = encoded.get(i);
			AssociatedIndex a = inliers.get(i);
			a.src = getOrThrow(element, "src");
			a.dst = getOrThrow(element, "dst");
		}
	}

	public static void save( SceneWorkingGraph working, String path ) {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(path), UTF_8);
			save(working, writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a {@link SceneWorkingGraph} into the {@link Writer} in a YAML format.
	 *
	 * @param working (Input) The graph which is to be saved
	 * @param outputWriter (Output) where the graph is writen to
	 */
	public static void save( SceneWorkingGraph working, Writer outputWriter ) {
		var out = new PrintWriter(outputWriter);
		Yaml yaml = createYmlObject();

		out.println("# " + working.getClass().getSimpleName() + " in YAML format. BoofCV " + BoofVersion.VERSION);

		List<Map<String, Object>> views = new ArrayList<>();
		for (int viewIdx = 0; viewIdx < working.viewList.size(); viewIdx++) {
			SceneWorkingGraph.View wview = working.viewList.get(viewIdx);
//			assertEq(viewIdx,wview.index,"Inconsistent view index."); // not required to be valid always

			Map<String, Object> element = new HashMap<>();
			views.add(element);
			element.put("pview", wview.pview.id);
			element.put("projective", wview.projective.data);
			element.put("intrinsic", putPinholeSimplified(wview.intrinsic));
			element.put("world_to_view", putSe3(wview.world_to_view));
			element.put("image_dimension", putDimension(wview.imageDimension));
			element.put("inliers", putInlierInfo(wview.inliers));
		}

		Map<String, Object> data = new HashMap<>();
		data.put("views", views);
		data.put("data_type", "SceneWorkingGraph");
		data.put("version", 0);

		yaml.dump(data, out);

		out.close();
	}

	public static SceneWorkingGraph load( String path, PairwiseImageGraph2 pairwise, @Nullable SceneWorkingGraph working ) {
		try {
			Reader reader = new InputStreamReader(new FileInputStream(path), UTF_8);
			return load(reader, pairwise, working);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Decodes {@link SceneWorkingGraph} encoded in a YAML format from a reader.
	 *
	 * @param reader (Input/Output) Where the graph is read from
	 * @param pairwise (Input) Pairwise graph which is referenced by the SceneWorkingGraph.
	 * @param working (Output) Optional storage for the working graph. If null a new instance is created.
	 * @return The decoded graph
	 */
	public static SceneWorkingGraph load( Reader reader, PairwiseImageGraph2 pairwise, @Nullable SceneWorkingGraph working ) {
		if (working == null)
			working = new SceneWorkingGraph();
		else
			working.reset();

		Yaml yaml = createYmlObject();

		Map<String, Object> data = yaml.load(reader);
		try {
			reader.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		List<Map<String, Object>> yamlViews = getOrThrow(data, "views");

		// First declare all the views and link to their respective pview
		for (Map<String, Object> yamlView : yamlViews) {
			PairwiseImageGraph2.View pview = pairwise.lookupNode(getOrThrow(yamlView, "pview"));
			working.addView(pview);
		}
		for (Map<String, Object> yamlView : yamlViews) {
			SceneWorkingGraph.View wview = working.lookupView(getOrThrow(yamlView, "pview"));
			copyIntoMatrix(getOrThrow(yamlView, "projective"), wview.projective);
			loadPinholeSimplified(getOrThrow(yamlView, "intrinsic"), wview.intrinsic);
			loadSe3(getOrThrow(yamlView, "world_to_view"), wview.world_to_view);
			loadDimension(getOrThrow(yamlView, "image_dimension"), wview.imageDimension);
			loadInlierInfo(getOrThrow(yamlView, "inliers"), pairwise, wview.inliers);
		}

		return working;
	}

	public static Map<String, Object> putInlierInfo( SceneWorkingGraph.InlierInfo inliers ) {
		Map<String, Object> map = new HashMap<>();

		List<String> views = new ArrayList<>();
		inliers.views.forEach(( i, v ) -> views.add(v.id));

		List<List<Integer>> observations = new ArrayList<>();
		for (int viewIdx = 0; viewIdx < inliers.views.size; viewIdx++) {
			List<Integer> obs = new ArrayList<>();
			inliers.observations.get(viewIdx).forIdx(( i, v ) -> obs.add(v));
			observations.add(obs);
		}

		map.put("views", views);
		map.put("observations", observations);

		return map;
	}

	public static SceneWorkingGraph.InlierInfo loadInlierInfo( Map<String, Object> map,
															   PairwiseImageGraph2 pairwise,
															   @Nullable SceneWorkingGraph.InlierInfo inliers ) {
		if (inliers == null)
			inliers = new SceneWorkingGraph.InlierInfo();
		SceneWorkingGraph.InlierInfo _inliers = inliers;

		List<String> views = getOrThrow(map, "views");
		List<List<Integer>> observations = getOrThrow(map, "observations");

		inliers.views.resize(views.size());
		inliers.views.reset();
		BoofMiscOps.forIdx(views, ( i, v ) -> _inliers.views.add(pairwise.lookupNode(v)));

		inliers.observations.resize(views.size());
		for (int viewIdx = 0; viewIdx < inliers.views.size; viewIdx++) {
			List<Integer> src = observations.get(viewIdx);
			GrowQueue_I32 dst = inliers.observations.get(viewIdx);
			dst.resize(src.size());
			dst.reset();
			src.forEach(dst::add);
		}

		return inliers;
	}

	public static Map<String, Object> putDimension( ImageDimension d ) {
		Map<String, Object> map = new HashMap<>();

		map.put("width", d.width);
		map.put("height", d.height);

		return map;
	}

	public static ImageDimension loadDimension( Map<String, Object> map, @Nullable ImageDimension dimension ) {
		if (dimension == null)
			dimension = new ImageDimension();

		dimension.width = (int)map.get("width");
		dimension.height = (int)map.get("height");

		return dimension;
	}

	public static Map<String, Object> putPinholeSimplified( BundlePinholeSimplified intrinsic ) {
		Map<String, Object> map = new HashMap<>();

		map.put("f", intrinsic.f);
		map.put("k1", intrinsic.k1);
		map.put("k2", intrinsic.k2);

		return map;
	}

	public static BundlePinholeSimplified loadPinholeSimplified( Map<String, Object> map,
																 @Nullable BundlePinholeSimplified intrinsic ) {
		if (intrinsic == null)
			intrinsic = new BundlePinholeSimplified();

		intrinsic.f = (double)map.get("f");
		intrinsic.k1 = (double)map.get("k1");
		intrinsic.k2 = (double)map.get("k2");

		return intrinsic;
	}
}
