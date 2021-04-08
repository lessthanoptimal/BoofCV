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

package boofcv.alg.structure;

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.alg.similar.SimilarImagesFromTracks;
import boofcv.misc.LookUpImages;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Getter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * High level interface designed to hide almost all of the complexity of converting an image sequence
 * into a sparse reconstruction.
 *
 * @author Peter Abeles
 */
public class ImageSequenceToSparseScene<T extends ImageGray<T>> implements VerbosePrint {

	/** Frame to frame image tracker */
	@Getter PointTracker<T> tracker;
	/** Identifies similar images taking advantage of features being tracked in a sequence */
	@Getter SimilarImagesFromTracks trackerSimilar;
	/** Creates a pairwise gram from similar images */
	@Getter GeneratePairwiseImageGraph generatePairwise;
	/** Create a metric scene from a pairwise image graph */
	@Getter MetricFromUncalibratedPairwiseGraph metricFromPairwise;
	/** Refines the metric reconstruction */
	@Getter RefineMetricWorkingGraph refineScene;
	/** Lookup table from input image Id into the scene view index */
	@Getter TObjectIntMap<String> imageIdToSceneViewIdx =
			new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY,Constants.DEFAULT_LOAD_FACTOR,-1);

	/** Maximum image pixels before it down samples */
	public int maxImagePixels = 800*600;

	// If not null then debug information is printed
	private PrintStream verbose;

	// Storage for image frame from sequence
	final private T imageFull;
	final private T imageDown;

	// Storage for active tracks
	final List<PointTrack> activeTracks = new ArrayList<>();

	// Profiling information
	private @Getter double timeTrackingMS;
	private @Getter double timePairwiseMS;
	private @Getter double timeMetricMS;
	private @Getter double timeRefineMS;

	/**
	 * Constructor which specifies all the internal implementations
	 */
	public ImageSequenceToSparseScene( PointTracker<T> tracker,
									   SimilarImagesFromTracks trackerSimilar,
									   GeneratePairwiseImageGraph generatePairwise,
									   MetricFromUncalibratedPairwiseGraph metricFromPairwise,
									   RefineMetricWorkingGraph refineScene,
									   ImageType<T> imageType ) {
		this.tracker = tracker;
		this.trackerSimilar = trackerSimilar;
		this.generatePairwise = generatePairwise;
		this.metricFromPairwise = metricFromPairwise;
		this.refineScene = refineScene;
		this.imageFull = imageType.createImage(1, 1);
		this.imageDown = imageFull.createSameShape();
	}

	/**
	 * Computes a sparse reconstruction from the image sequence. Images are processed in order thta they
	 * appear in the list and are assumed to be taken by a single camera with a small time step between frames.
	 *
	 * @param imageIDs (Input) Ordered list of image IDs in the sequence
	 * @param lookUpImages (Input) Used to go from image ID to image.
	 * @return true if successful or false if it failed
	 */
	public boolean process( List<String> imageIDs, LookUpImages lookUpImages ) {
		// Reset internal profiling information
		timeTrackingMS = 0;
		timePairwiseMS = 0;
		timeMetricMS = 0;
		timeRefineMS = 0;

		imageIdToSceneViewIdx.clear();

		long time0 = System.nanoTime();
		// Load images and feed into feature tracker
		for (int indexIDs = 0; indexIDs < imageIDs.size(); indexIDs++) {
			lookUpImages.loadImage(imageIDs.get(indexIDs), imageFull);

			// Make sure the image is a reasonable size for processing. Also many tuning parameters
			// are not scale invariant
			T image = AverageDownSampleOps.downMaxPixels(imageFull, imageDown, maxImagePixels);

			if (indexIDs == 0) {
				tracker.reset();
				trackerSimilar.initialize(image.width, image.height);
			}

			tracker.process(image);
			tracker.spawnTracks();
			tracker.getActiveTracks(activeTracks);
			trackerSimilar.processFrame(activeTracks, tracker.getFrameID());
		}

		// Find the pairwise geometric relationship between views
		long time1 = System.nanoTime();
		generatePairwise.process(trackerSimilar);
		long time2 = System.nanoTime();

		// Compute a first pass metric reconstruction
		if (!metricFromPairwise.process(trackerSimilar, generatePairwise.getGraph())) {
			if (verbose != null) verbose.println("Failed at metric from pairwise");
			return false;
		}
		long time3 = System.nanoTime();

		// Order of views in this graph is the same as the views in the SBA scene
		// Since the image name is lost in the tracker it's referred to by frame number
		SceneWorkingGraph graph = metricFromPairwise.workGraph;
		for (int sbaIdx = 0; sbaIdx < graph.viewList.size(); sbaIdx++) {
			int imageIdx = Integer.parseInt(graph.viewList.get(sbaIdx).pview.id);
			imageIdToSceneViewIdx.put(imageIDs.get(imageIdx), sbaIdx);
		}

		// Refine the entire scene all at once to get a better estimate
		if (!refineScene.process(trackerSimilar, metricFromPairwise.getWorkGraph())) {
			if (verbose != null) verbose.println("Failed at scene refine");
			return false;
		}
		long time4 = System.nanoTime();

		timeTrackingMS = (time1 - time0)*1e-6;
		timePairwiseMS = (time2 - time1)*1e-6;
		timeMetricMS = (time3 - time2)*1e-6;
		timeRefineMS = (time4 - time3)*1e-6;

		return true;
	}

	/** Returns the found sparse scene which has been reconstructed */
	public SceneStructureMetric getSceneStructure() {
		return refineScene.bundleAdjustment.structure;
	}

	/** Returns the observations used to reconstruct the scene */
	public SceneObservations getSceneObservations() {
		return refineScene.bundleAdjustment.observations;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> set ) {
		this.verbose = out;
	}
}
