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

package boofcv.io.geo;

import boofcv.BoofVersion;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.similar.SimilarImagesData;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.alg.structure.SceneWorkingGraph.InlierInfo;
import boofcv.io.calibration.CalibrationIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static boofcv.io.calibration.CalibrationIO.*;
import static boofcv.misc.BoofMiscOps.getOrThrow;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * For loading and saving data structures related to multiview reconstruction.
 *
 * @author Peter Abeles
 */
public class MultiViewIO {

	public static void save( LookUpSimilarImages db, String path ) {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(path), UTF_8);
			save(db, writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a {@link LookUpSimilarImages} into the {@link Writer}.
	 *
	 * @param db (Input) Information on similar images
	 * @param outputWriter (Output) where the graph is writen to
	 */
	public static void save( LookUpSimilarImages db, Writer outputWriter ) {
		PrintWriter out = new PrintWriter(outputWriter);

		Yaml yaml = createYmlObject();

		out.println("# " + db.getClass().getSimpleName() + " in YAML format. BoofCV " + BoofVersion.VERSION);

		// Get list of all the images
		List<String> imageIds = db.getImageIDs();

		// Storage for image information
		List<Map<String, Object>> imageInfo = new ArrayList<>();
		DogArray<Point2D_F64> features = new DogArray<>(Point2D_F64::new);
		DogArray<AssociatedIndex> matches = new DogArray<>(AssociatedIndex::new);

		// Create a look up table from view ID to index in array. Only need to save associations of lower views
		TObjectIntMap<String> viewToIndex = new TObjectIntHashMap<>();
		for (int i = 0; i < imageIds.size(); i++) {
			viewToIndex.put(imageIds.get(i), i);
		}

		for (int viewIndex = 0; viewIndex < imageIds.size(); viewIndex++) {
			String id = imageIds.get(viewIndex);

			// Map contaiing all the data related to this image/view
			Map<String, Object> imageMap = new HashMap<>();

			// Add the list of features in this image
			db.lookupPixelFeats(id, features);
			// Flatten the pixels into an array for easy storage
			double[] pixels = new double[features.size*2];
			for (int j = 0; j < features.size; j++) {
				Point2D_F64 p = features.get(j);
				pixels[j*2] = p.x;
				pixels[j*2 + 1] = p.y;
			}

			List<String> similarIds = new ArrayList<>();
			db.findSimilar(id, ( s ) -> true, similarIds);

			// Create the list of views its similar to and the feature pairs between the two views
			List<Map<String, Object>> listRelated = new ArrayList<>();
			for (int similarIdx = 0; similarIdx < similarIds.size(); similarIdx++) {
				// don't need to save the same information twice
				int similarViewIndex = viewToIndex.get(similarIds.get(similarIdx));
				if (similarViewIndex < viewIndex)
					continue;

				db.lookupAssociated(similarIds.get(similarIdx), matches);
				Map<String, Object> relationship = new HashMap<>();
				int[] matchesIndexes = new int[matches.size*2];
				for (int j = 0; j < matches.size; j++) {
					AssociatedIndex p = matches.get(j);
					matchesIndexes[j*2] = p.src;
					matchesIndexes[j*2 + 1] = p.dst;
				}

				relationship.put("id", similarIds.get(similarIdx));
				relationship.put("pairs", matchesIndexes);
				listRelated.add(relationship);
			}

			imageMap.put("features", pixels);
			imageMap.put("similar", listRelated);
			imageInfo.add(imageMap);
		}

		Map<String, Object> data = new HashMap<>();
		data.put("images", imageIds);
		data.put("info", imageInfo);
		data.put("data_type", "SimilarImages");
		data.put("version", 0);

		yaml.dump(data, out);

		out.close();
	}

	public static void save( PairwiseImageGraph graph, String path ) {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(path), UTF_8);
			save(graph, writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a {@link PairwiseImageGraph} into the {@link Writer}.
	 *
	 * @param graph (Input) The graph which is to be saved
	 * @param outputWriter (Output) where the graph is writen to
	 */
	public static void save( PairwiseImageGraph graph, Writer outputWriter ) {
		PrintWriter out = new PrintWriter(outputWriter);

		Yaml yaml = createYmlObject();

		out.println("# " + graph.getClass().getSimpleName() + " in YAML format. BoofCV " + BoofVersion.VERSION);

		List<Map<String, Object>> motions = new ArrayList<>();
		for (int motionIdx = 0; motionIdx < graph.edges.size; motionIdx++) {
			PairwiseImageGraph.Motion pmotion = graph.edges.get(motionIdx);
			BoofMiscOps.checkEq(pmotion.index, motionIdx);

			Map<String, Object> element = new HashMap<>();
			motions.add(element);
			element.put("is_3D", pmotion.is3D);
			element.put("score_3d", pmotion.score3D);
			element.put("src", pmotion.src.id);
			element.put("dst", pmotion.dst.id);
			element.put("inliers", encodeInliers(pmotion.inliers));
		}

		List<Map<String, Object>> views = new ArrayList<>();
		for (int viewIdx = 0; viewIdx < graph.nodes.size; viewIdx++) {
			PairwiseImageGraph.View pview = graph.nodes.get(viewIdx);

			List<Integer> connections = new ArrayList<>();
			pview.connections.forIdx(( i, v ) -> connections.add(v.index));

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

	public static void save( SceneStructureMetric scene, String path ) {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(path), UTF_8);
			save(scene, writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves a {@link SceneStructureMetric} into the {@link Writer}.
	 *
	 * @param scene (Input) The scene
	 * @param outputWriter (Output) where the scene is writen to
	 */
	public static void save( SceneStructureMetric scene, Writer outputWriter ) {
		PrintWriter out = new PrintWriter(outputWriter);

		Yaml yaml = createYmlObject();

		out.println("# " + scene.getClass().getSimpleName() + " in YAML format. BoofCV " + BoofVersion.VERSION);

		List<Map<String, Object>> views = new ArrayList<>();
		List<Map<String, Object>> motions = new ArrayList<>();
		List<Map<String, Object>> rigids = new ArrayList<>();
		List<Map<String, Object>> cameras = new ArrayList<>();
		List<Map<String, Object>> points = new ArrayList<>();

		scene.views.forEach(v -> views.add(encodeSceneView(scene, v)));
		scene.motions.forEach(m -> motions.add(encodeSceneMotion(m)));
		scene.rigids.forEach(r -> rigids.add(encodeSceneRigid(r)));
		scene.cameras.forEach(c -> cameras.add(encodeSceneCamera(c)));
		scene.points.forEach(p -> points.add(encodeScenePoint(p)));

		Map<String, Object> data = new HashMap<>();
		data.put("views", views);
		data.put("motions", motions);
		data.put("rigids", rigids);
		data.put("cameras", cameras);
		data.put("points", points);
		data.put("homogenous", scene.isHomogenous());
		data.put("data_type", "SceneStructureMetric");
		data.put("version", 0);

		yaml.dump(data, out);

		out.close();
	}

	private static Map<String, Object> encodeSceneView( SceneStructureMetric scene,
														SceneStructureMetric.View v ) {
		Map<String, Object> encoded = new HashMap<>();
		encoded.put("camera", v.camera);
		encoded.put("parent_to_view", v.parent_to_view);
		if (v.parent != null)
			encoded.put("parent", scene.views.indexOf(v.parent));
		return encoded;
	}

	private static Map<String, Object> encodeSceneMotion( SceneStructureMetric.Motion m ) {
		Map<String, Object> encoded = new HashMap<>();
		encoded.put("known", m.known);
		encoded.put("motion", putSE3(m.motion));
		return encoded;
	}

	private static Map<String, Object> encodeSceneRigid( SceneStructureMetric.Rigid r ) {
		Map<String, Object> encoded = new HashMap<>();
		encoded.put("known", r.known);
		encoded.put("object_to_world", putSE3(r.object_to_world));
		encoded.put("indexFirst", r.indexFirst);
		List<Map<String, Object>> points = new ArrayList<>();
		for (int i = 0; i < r.points.length; i++) {
			points.add(encodeScenePoint(r.points[i]));
		}
		encoded.put("points", points);

		return encoded;
	}

	private static Map<String, Object> encodeScenePoint( SceneStructureCommon.Point p ) {
		Map<String, Object> encoded = new HashMap<>();
		encoded.put("coordinate", p.coordinate);
		encoded.put("views", p.views.toArray());
		return encoded;
	}

	private static SceneStructureCommon.Point decodeScenePoint( Map<String, Object> map,
																@Nullable SceneStructureCommon.Point p )
			throws IOException {
		List<Double> coordinate = getOrThrow(map, "coordinate");
		List<Integer> views = getOrThrow(map, "views");

		if (p == null)
			p = new SceneStructureCommon.Point(coordinate.size());

		for (int i = 0; i < coordinate.size(); i++) {
			p.coordinate[i] = coordinate.get(i);
		}
		p.views.resize(views.size());
		for (int i = 0; i < views.size(); i++) {
			p.views.data[i] = views.get(i);
		}

		return p;
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

	private static Map<String, Object> encodeSceneCamera( SceneStructureCommon.Camera c ) {
		Map<String, Object> encoded = new HashMap<>();
		encoded.put("known", c.known);

		Map<String, Object> model;
		if (c.model instanceof BundlePinholeSimplified) {
			model = putPinholeSimplified((BundlePinholeSimplified)c.model);
			model.put("type", "PinholeSimplified");
		} else {
			throw new RuntimeException("BundleAdjustmentCamera type not yet supported. " + c.getClass().getSimpleName());
		}
		encoded.put("model", model);
		return encoded;
	}

	private static SceneStructureCommon.Camera decodeSceneCamera( Map<String, Object> map,
																  @Nullable SceneStructureCommon.Camera c )
			throws IOException {
		if (c == null)
			c = new SceneStructureCommon.Camera();
		c.known = getOrThrow(map, "known");
		Map<String, Object> model = getOrThrow(map, "model");
		String type = getOrThrow(model, "type");
		c.model = switch (type) {
			case "PinholeSimplified" -> loadPinholeSimplified(model, null);
			default -> throw new RuntimeException("Unknown camera. " + type);
		};

		return c;
	}

	public static SceneStructureMetric load( String path, @Nullable SceneStructureMetric graph ) {
		try {
			Reader reader = new InputStreamReader(new FileInputStream(path), UTF_8);
			return load(reader, graph);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Decodes {@link SceneStructureMetric} encoded in a YAML format from a reader.
	 *
	 * @param reader (Input/Output) Where the graph is read from
	 * @param scene (Output) Optional storage for the scene. If null a new instance is created.
	 * @return The decoded graph
	 */
	public static SceneStructureMetric load( Reader reader, @Nullable SceneStructureMetric scene ) {
		Yaml yaml = createYmlObject();

		Map<String, Object> data = yaml.load(reader);
		try {
			reader.close();
			boolean homogenous = getOrThrow(data, "homogenous");

			List<Map<String, Object>> yamlViews = getOrThrow(data, "views");
			List<Map<String, Object>> yamlMotions = getOrThrow(data, "motions");
			List<Map<String, Object>> yamlRigids = getOrThrow(data, "rigids");
			List<Map<String, Object>> yamlCameras = getOrThrow(data, "cameras");
			List<Map<String, Object>> yamlPoints = getOrThrow(data, "points");

			if (scene != null && scene.isHomogenous() != homogenous)
				scene = null;
			if (scene == null)
				scene = new SceneStructureMetric(homogenous);
			scene.initialize(
					yamlCameras.size(), yamlViews.size(), yamlMotions.size(), yamlPoints.size(), yamlRigids.size());

			for (int i = 0; i < yamlViews.size(); i++) {
				SceneStructureMetric.View v = scene.views.get(i);
				Map<String, Object> yamlView = yamlViews.get(i);
				v.camera = getOrThrow(yamlView, "camera");
				v.parent_to_view = getOrThrow(yamlView, "parent_to_view");
				v.parent = yamlView.containsKey("parent") ? scene.views.get(getOrThrow(yamlView, "parent")) : null;
			}

			for (int i = 0; i < yamlMotions.size(); i++) {
				SceneStructureMetric.Motion m = scene.motions.grow();
				Map<String, Object> yamlMotion = yamlMotions.get(i);
				m.known = getOrThrow(yamlMotion, "known");
				loadSE3(getOrThrow(yamlMotion, "motion"), m.motion);
			}

			for (int i = 0; i < yamlRigids.size(); i++) {
				SceneStructureMetric.Rigid r = scene.rigids.get(i);
				Map<String, Object> yamlRigid = yamlRigids.get(i);
				List<Map<String, Object>> points = getOrThrow(yamlRigid, "points");
				r.init(points.size(), scene.isHomogenous() ? 4 : 3);
				r.known = getOrThrow(yamlRigid, "known");
				r.indexFirst = getOrThrow(yamlRigid, "indexFirst");
				loadSE3(getOrThrow(yamlRigid, "object_to_world"), r.object_to_world);
				for (int j = 0; j < r.points.length; j++) {
					decodeScenePoint(points.get(j), r.points[j]);
				}
			}

			for (int i = 0; i < scene.points.size; i++) {
				decodeScenePoint(yamlPoints.get(i), scene.points.get(i));
			}

			for (int i = 0; i < yamlCameras.size(); i++) {
				decodeSceneCamera(yamlCameras.get(i), scene.cameras.get(i));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return scene;
	}

	public static LookUpSimilarImages loadSimilarImages( String path ) {
		try {
			Reader reader = new InputStreamReader(new FileInputStream(path), UTF_8);
			return loadSimilarImages(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Decodes {@link LookUpSimilarImages} encoded in a YAML format from a reader.
	 *
	 * @param reader (Input/output) where to read the data from
	 * @return The decoded graph
	 */
	public static LookUpSimilarImages loadSimilarImages( Reader reader ) {
		Yaml yaml = createYmlObject();

		SimilarImagesData ret = new SimilarImagesData();

		DogArray<Point2D_F64> features = new DogArray<>(Point2D_F64::new);

		Map<String, Object> data = yaml.load(reader);
		try {
			reader.close();
			List<String> listImages = getOrThrow(data, "images");
			List<Map<String, Object>> yamlInfo = getOrThrow(data, "info");

			BoofMiscOps.checkEq(listImages.size(), yamlInfo.size());

			// Add all the images
			for (int imageIdx = 0; imageIdx < listImages.size(); imageIdx++) {
				String id = listImages.get(imageIdx);
				Map<String, Object> yamlImage = yamlInfo.get(imageIdx);

				List<Double> yamlPixels = getOrThrow(yamlImage, "features");
				features.resize(yamlPixels.size()/2);
				for (int i = 0; i < yamlPixels.size(); i += 2) {
					double x = yamlPixels.get(i);
					double y = yamlPixels.get(i + 1);
					features.get(i/2).setTo(x, y);
				}

				ret.add(id, features.toList());
			}

			// Add the relationships
			var pairs = new DogArray<>(AssociatedIndex::new);
			for (int imageIdx = 0; imageIdx < listImages.size(); imageIdx++) {
				String id = listImages.get(imageIdx);
				Map<String, Object> yamlImage = yamlInfo.get(imageIdx);

				List<Map<String, Object>> listSimilar = getOrThrow(yamlImage, "similar");
				for (int i = 0; i < listSimilar.size(); i++) {
					Map<String, Object> yamlSimilar = listSimilar.get(i);
					String similarID = getOrThrow(yamlSimilar, "id");
					List<Integer> yamlPairs = getOrThrow(yamlSimilar, "pairs");

					pairs.resetResize(yamlPairs.size()/2);
					for (int j = 0; j < pairs.size; j++) {
						pairs.get(j).setTo(yamlPairs.get(j*2), yamlPairs.get(j*2 + 1));
					}
					ret.setRelationship(id, similarID, pairs.toList());
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return ret;
	}

	public static PairwiseImageGraph load( String path, @Nullable PairwiseImageGraph graph ) {
		try {
			Reader reader = new InputStreamReader(new FileInputStream(path), UTF_8);
			return load(reader, graph);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Decodes {@link PairwiseImageGraph} encoded in a YAML format from a reader.
	 *
	 * @param reader (Input/Output) Where the graph is read from
	 * @param graph (Output) Optional storage for the graph. If null a new instance is created.
	 * @return The decoded graph
	 */
	public static PairwiseImageGraph load( Reader reader, @Nullable PairwiseImageGraph graph ) {
		if (graph == null)
			graph = new PairwiseImageGraph();
		else
			graph.reset();
		final var _graph = graph;

		Yaml yaml = createYmlObject();

		Map<String, Object> data = yaml.load(reader);
		try {
			reader.close();
			List<Map<String, Object>> yamlViews = getOrThrow(data, "views");
			List<Map<String, Object>> yamlMotions = getOrThrow(data, "motions");

			graph.nodes.resize(yamlViews.size());
			graph.edges.resize(yamlMotions.size());

			for (int viewIdx = 0; viewIdx < yamlViews.size(); viewIdx++) {
				Map<String, Object> yamlView = yamlViews.get(viewIdx);
				PairwiseImageGraph.View v = graph.nodes.get(viewIdx);
				v.index = viewIdx;
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
				PairwiseImageGraph.Motion m = graph.edges.get(i);

				m.score3D = getOrThrow(yamlMotion, "score_3d");
				m.is3D = getOrThrow(yamlMotion, "is_3D");
				m.src = getOrThrow(graph.mapNodes, getOrThrow(yamlMotion, "src"));
				m.dst = getOrThrow(graph.mapNodes, getOrThrow(yamlMotion, "dst"));
				m.index = i;
				decodeInliers(getOrThrow(yamlMotion, "inliers"), m.inliers);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return graph;
	}

	private static void copyIntoMatrix( List<Double> arrayData, DMatrixRMaj matrix ) {
		BoofMiscOps.checkEq(arrayData.size(), matrix.data.length);
		for (int j = 0; j < matrix.data.length; j++) {
			matrix.data[j] = arrayData.get(j);
		}
	}

	private static void decodeInliers( List<Map<String, Object>> encoded, DogArray<AssociatedIndex> inliers )
			throws IOException {
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

		List<Map<String, Object>> cameras = new ArrayList<>();
		for (int cameraIdx = 0; cameraIdx < working.listCameras.size(); cameraIdx++) {
			SceneWorkingGraph.Camera camera = working.listCameras.get(cameraIdx);

			Map<String, Object> element = new HashMap<>();
			cameras.add(element);
			element.put("index_db", camera.indexDB);
			element.put("prior", CalibrationIO.putModelBrown(camera.prior, null));
			element.put("intrinsic", putPinholeSimplified(camera.intrinsic));
		}

		List<Map<String, Object>> views = new ArrayList<>();
		for (int viewIdx = 0; viewIdx < working.listViews.size(); viewIdx++) {
			SceneWorkingGraph.View wview = working.listViews.get(viewIdx);
			SceneWorkingGraph.Camera camera = working.getViewCamera(wview);

//			assertEq(viewIdx,wview.index,"Inconsistent view index."); // not required to be valid always

			Map<String, Object> element = new HashMap<>();
			views.add(element);
			element.put("pview", wview.pview.id);
			element.put("projective", wview.projective.data);
			element.put("world_to_view", putSe3(wview.world_to_view));
			element.put("camera_index", camera.localIndex);
			element.put("inliers", putInlierInfo(wview.inliers));
		}

		Map<String, Object> data = new HashMap<>();
		data.put("cameras", cameras);
		data.put("views", views);
		data.put("data_type", "SceneWorkingGraph");
		data.put("version", 0);

		yaml.dump(data, out);

		out.close();
	}

	public static SceneWorkingGraph load( String path, PairwiseImageGraph pairwise, @Nullable SceneWorkingGraph working ) {
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
	public static SceneWorkingGraph load( Reader reader, PairwiseImageGraph pairwise, @Nullable SceneWorkingGraph working ) {
		if (working == null)
			working = new SceneWorkingGraph();
		else
			working.reset();

		Yaml yaml = createYmlObject();

		Map<String, Object> data = yaml.load(reader);
		try {
			reader.close();

			List<Map<String, Object>> yamlCameras = getOrThrow(data, "cameras");
			for (int cameraIdx = 0; cameraIdx < yamlCameras.size(); cameraIdx++) {
				Map<String, Object> yamlCamera = yamlCameras.get(cameraIdx);

				int indexDB = getOrThrow(yamlCamera, "index_db");
				SceneWorkingGraph.Camera camera = working.addCamera(indexDB);
				camera.prior.setTo(CalibrationIO.load((Map<String, Object>)getOrThrow(yamlCamera, "prior")));
				loadPinholeSimplified(getOrThrow(yamlCamera, "intrinsic"), camera.intrinsic);
			}

			// First declare all the views and link to their respective pview
			List<Map<String, Object>> yamlViews = getOrThrow(data, "views");
			for (Map<String, Object> yamlView : yamlViews) {
				PairwiseImageGraph.View pview = pairwise.lookupNode(getOrThrow(yamlView, "pview"));
				int cameraIdx = getOrThrow(yamlView, "camera_index");
				SceneWorkingGraph.Camera camera = working.listCameras.get(cameraIdx);
				working.addView(pview, camera);
			}

			for (Map<String, Object> yamlView : yamlViews) {
				SceneWorkingGraph.View wview = working.lookupView(getOrThrow(yamlView, "pview"));
				copyIntoMatrix(getOrThrow(yamlView, "projective"), wview.projective);
				loadSe3(getOrThrow(yamlView, "world_to_view"), wview.world_to_view);
				loadInlierInfo(getOrThrow(yamlView, "inliers"), pairwise, wview.inliers);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return working;
	}

	public static List<Object> putInlierInfo( FastAccess<InlierInfo> listInliers ) {
		List<Object> list = new ArrayList<>();
		for (int infoIdx = 0; infoIdx < listInliers.size; infoIdx++) {
			InlierInfo inliers = listInliers.get(infoIdx);

			Map<String, Object> map = new HashMap<>();

			List<String> views = new ArrayList<>();
			inliers.views.forIdx(( i, v ) -> views.add(v.id));

			List<List<Integer>> observations = new ArrayList<>();
			for (int viewIdx = 0; viewIdx < inliers.views.size; viewIdx++) {
				List<Integer> obs = new ArrayList<>();
				inliers.observations.get(viewIdx).forIdx(( i, v ) -> obs.add(v));
				observations.add(obs);
			}

			map.put("views", views);
			map.put("observations", observations);
			list.add(map);
		}
		return list;
	}

	public static void loadInlierInfo( List<Object> list,
									   PairwiseImageGraph pairwise,
									   DogArray<InlierInfo> listInliers )
			throws IOException {

		listInliers.resetResize(list.size());
		for (int infoIdx = 0; infoIdx < list.size(); infoIdx++) {
			Map<String, Object> map = (Map)list.get(infoIdx);

			InlierInfo inliers = listInliers.get(infoIdx);

			List<String> views = getOrThrow(map, "views");
			List<List<Integer>> observations = getOrThrow(map, "observations");

			inliers.views.resize(views.size());
			inliers.views.reset();
			BoofMiscOps.forIdx(views, ( i, v ) -> inliers.views.add(pairwise.lookupNode(v)));

			inliers.observations.resize(views.size());
			for (int viewIdx = 0; viewIdx < inliers.views.size; viewIdx++) {
				List<Integer> src = observations.get(viewIdx);
				DogArray_I32 dst = inliers.observations.get(viewIdx);
				dst.resize(src.size());
				dst.reset();
				src.forEach(dst::add);
			}
		}
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

	public static Map<String, Object> putSE3( Se3_F64 m ) {
		Map<String, Object> map = new HashMap<>();

		map.put("x", m.T.x);
		map.put("y", m.T.y);
		map.put("z", m.T.z);
		map.put("R", m.R.data);

		return map;
	}

	public static Se3_F64 loadSE3( Map<String, Object> map,
								   @Nullable Se3_F64 m ) throws IOException {
		if (m == null)
			m = new Se3_F64();

		m.T.x = (double)map.get("x");
		m.T.y = (double)map.get("y");
		m.T.z = (double)map.get("z");

		copyIntoMatrix(getOrThrow(map, "R"), m.R);

		return m;
	}
}
