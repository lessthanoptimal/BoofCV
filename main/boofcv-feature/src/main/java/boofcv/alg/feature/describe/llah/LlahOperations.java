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

package boofcv.alg.feature.describe.llah;

import boofcv.struct.geo.PointIndex2D_F64;
import georegression.helper.KdTreePoint2D_F64;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import org.ddogleg.combinatorics.Combinations;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.sorting.QuickSort_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Locally Likely Arrangement Hashing (LLAH) [1] computes a descriptor for a landmark based on the local geometry of
 * neighboring landmarks on the image plane. Originally proposed for document retrieval. These features are either
 * invariant to perspective or affine transforms.
 *
 * <p>Works by sampling the N neighbors around a point. These ports are sorted in clockwise order. However,
 * it is not known which points should be first so all cyclical permutations of set-N are now found.
 * It is assumed that at least M points in set M are a member
 * of the set used to compute the feature, so all M combinations of points in set-N are found. Then the geometric
 * invariants are computed using set-M.</p>
 *
 * <p>When describing the documents the hash and invariant values of each point in a document is saved. When
 * looking up documents these features are again computed for all points in view, but then the document
 * type is voted upon and returned.</p>
 *
 * <ol>
 *     <li>Nakai, Tomohiro, Koichi Kise, and Masakazu Iwamura.
 *     "Use of affine invariants in locally likely arrangement hashing for camera-based document image retrieval."
 *     International Workshop on Document Analysis Systems. Springer, Berlin, Heidelberg, 2006.</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class LlahOperations {

	// Number of nearest neighbors it will search for
	@Getter final int numberOfNeighborsN;
	// Size of combination set from the set of neighbors
	@Getter final int sizeOfCombinationM;
	// Number of invariants in the feature. Determined by the type and M
	@Getter final int numberOfInvariants;

	final List<Point2D_F64> setM = new ArrayList<>();
	final List<Point2D_F64> permuteM = new ArrayList<>();

	// Computes the hash value for each feature
	@Getter LlahHasher hasher;
	// Used to look up features/documents
	@Getter final LlahHashTable hashTable = new LlahHashTable();

	// List of all documents
	@Getter final DogArray<LlahDocument> documents = new DogArray<>(LlahDocument::new, LlahDocument::reset);

	//========================== Internal working variables
	final NearestNeighbor<Point2D_F64> nn = FactoryNearestNeighbor.kdtree(new KdTreePoint2D_F64());
	private final NearestNeighbor.Search<Point2D_F64> search = nn.createSearch();
	private final DogArray<NnData<Point2D_F64>> resultsNN = new DogArray<>(NnData::new);
	final List<Point2D_F64> neighbors = new ArrayList<>();
	private final double[] angles;
	private final QuickSort_F64 sorter = new QuickSort_F64();
	private final DogArray<FoundDocument> resultsStorage = new DogArray<>(FoundDocument::new);
	private final TIntObjectHashMap<FoundDocument> foundMap = new TIntObjectHashMap<>();

	private final DogArray<LlahFeature> allFeatures;

	// Used to compute all the combinations of a set
	private final Combinations<Point2D_F64> combinator = new Combinations<>();

	// Recycling memory
	DogArray<DotVotingBooth> votingBooths = new DogArray<>(DotVotingBooth::new, DotVotingBooth::reset);

	/**
	 * Configures the LLAH feature computation
	 *
	 * @param numberOfNeighborsN Number of neighbors to be considered
	 * @param sizeOfCombinationM Number of different combinations within the neighbors
	 * @param hasher Computes the hash code
	 */
	public LlahOperations( int numberOfNeighborsN, int sizeOfCombinationM,
						   LlahHasher hasher ) {
		this.numberOfNeighborsN = numberOfNeighborsN;
		this.sizeOfCombinationM = sizeOfCombinationM;
		this.numberOfInvariants = hasher.getNumberOfInvariants(sizeOfCombinationM);
		this.hasher = hasher;

		angles = new double[numberOfNeighborsN];
		allFeatures = new DogArray<>(() -> new LlahFeature(numberOfInvariants), LlahFeature::reset);
	}

	/**
	 * Forgets all the documents and recycles data
	 */
	public void clearDocuments() {
		documents.reset();
		allFeatures.reset();
		hashTable.reset();
	}

	/**
	 * Learns the hashing function from the set of point sets
	 *
	 * @param pointSets Point sets. Each set represents one document
	 * @param numDiscrete Number of discrete values the invariant is converted to
	 * @param histogramLength Number of elements in the histogram. 100,000 is recommended
	 * @param maxInvariantValue The maximum number of value an invariant is assumed to have.
	 * For affine ~25. Cross Ratio
	 */
	public void learnHashing( Iterable<List<Point2D_F64>> pointSets, int numDiscrete,
							  int histogramLength, double maxInvariantValue ) {

		// to make the math faster use a fine grained array with more extreme values than expected
		int[] histogram = new int[histogramLength];

		// Storage for computed invariants
		double[] invariants = new double[numberOfInvariants];

		// Go through each point and compute some invariants from it
		for (var locations2D : pointSets) { // lint:forbidden ignore_line
			nn.setPoints(locations2D, false);

			computeAllFeatures(locations2D, ( idx, l ) -> {
				hasher.computeInvariants(l, invariants, 0);

				for (int i = 0; i < invariants.length; i++) {
					int j = Math.min(histogram.length - 1, (int)(histogram.length*invariants[i]/maxInvariantValue));
					histogram[j]++;
				}
			});
		}

		// Sanity check
		double endFraction = histogram[histogram.length - 1]/(double)IntStream.of(histogram).sum();
		double maxAllowed = 0.5/numDiscrete;
		if (endFraction > maxAllowed)
			System.err.println("WARNING: last element in histogram has a significant count. " + endFraction + " > " + maxAllowed +
					" maxInvariantValue should be increased");

		hasher.learnDiscretization(histogram, histogram.length, maxInvariantValue, numDiscrete);
	}

	/**
	 * Creates a new document from the 2D points. The document and points are added to the hash table
	 * for later retrieval.
	 *
	 * @param locations2D Location of points inside the document
	 * @return The document which was added to the hash table.
	 */
	public LlahDocument createDocument( List<Point2D_F64> locations2D ) {
		checkListSize(locations2D);

		LlahDocument doc = documents.grow();
		doc.documentID = documents.size() - 1;

		// copy the points
		doc.landmarks.copyAll(locations2D, ( src, dst ) -> dst.setTo(src));

		computeAllFeatures(locations2D, ( idx, l ) -> createProcessor(doc, idx));

		return doc;
	}

	/**
	 * Computes the maximum number of unique hash code a point can have.
	 */
	public long computeMaxUniqueHashPerPoint() {
		long comboHash = Combinations.computeTotalCombinations(numberOfNeighborsN, sizeOfCombinationM);
		return comboHash*sizeOfCombinationM;
	}

	private void createProcessor( LlahDocument doc, int idx ) {
		// Given this set compute the feature
		LlahFeature feature = allFeatures.grow();
		hasher.computeHash(permuteM, feature);

		// save the results
		feature.landmarkID = idx;
		feature.documentID = doc.documentID;
		doc.features.add(feature);
		hashTable.add(feature);
	}

	/**
	 * Given the set of observed locations, compute all the features for each point. Have processor handle
	 * the results as they are found
	 */
	void computeAllFeatures( List<Point2D_F64> dots, ProcessPermutation processor ) {
		// set up nn search
		nn.setPoints(dots, false);

		// Compute the features for all points in this document
		for (int dotIdx = 0; dotIdx < dots.size(); dotIdx++) {
//			System.out.println("================ pointID "+dotIdx);

			findNeighbors(dots.get(dotIdx));

			// All combinations of size M from neighbors
			combinator.init(neighbors, sizeOfCombinationM);
			do {
				setM.clear();
				for (int i = 0; i < sizeOfCombinationM; i++) {
					setM.add(combinator.get(i));
				}

				// Cyclical permutations of 'setM'
				// When you look it up you won't know the order points are observed in
				for (int i = 0; i < sizeOfCombinationM; i++) {
					permuteM.clear();
					for (int j = 0; j < sizeOfCombinationM; j++) {
						int idx = (i + j)%sizeOfCombinationM;
						permuteM.add(setM.get(idx));
					}

					processor.process(dotIdx, permuteM);
				}
			} while (combinator.next());
		}
	}

	/**
	 * Finds all the neighbors
	 */
	void findNeighbors( Point2D_F64 target ) {
		// Find N nearest-neighbors of p0
		search.findNearest(target, -1, numberOfNeighborsN + 1, resultsNN);

		// Find the neighbors, removing p0
		neighbors.clear();
		for (int i = 0; i < resultsNN.size; i++) {
			Point2D_F64 n = resultsNN.get(i).point;
			if (n == target) // it will always find the p0 point
				continue;
			neighbors.add(n);
		}

		// Compute the angle of each neighbor
		for (int i = 0; i < neighbors.size(); i++) {
			Point2D_F64 n = neighbors.get(i);
			angles[i] = Math.atan2(n.y - target.y, n.x - target.x);
		}

		// sort the neighbors in clockwise order
		sorter.sort(angles, angles.length, neighbors);

//		System.out.println("tgt"+target);
//		for (int i = 0; i < neighbors.size(); i++) {
//			System.out.println("  "+neighbors.get(i));
//		}
	}

	/**
	 * Looks up all the documents which match observed features.
	 *
	 * @param dots Observed feature locations
	 * @param minLandmarks Minimum number of landmarks that are assigned to a document for it to be accepted
	 * @param output Storage for results. WARNING: Results are recycled on next call!
	 */
	public void lookupDocuments( List<Point2D_F64> dots, int minLandmarks, List<FoundDocument> output ) {
		output.clear();

		// It needs to have a minimum of this number of points to work
		if (dots.size() < numberOfNeighborsN + 1)
			return;

		votingBooths.reset();
		foundMap.clear();
		resultsStorage.reset();

		// Used to keep track of what has been seen and what has not been seen
		votingBooths.resize(dots.size());

		var featureComputed = new LlahFeature(numberOfInvariants);

		// Compute features, look up matching known features, then vote
		computeAllFeatures(dots, ( dotIdx, pointSet ) ->
				lookupProcessor(pointSet, dotIdx, featureComputed, votingBooths));

		for (int dotIdx = 0; dotIdx < dots.size(); dotIdx++) {
			DotVotingBooth booth = votingBooths.get(dotIdx);
			if (booth.votes.size == 0)
				continue;
			DotToLandmark best = booth.votes.get(0);
			for (int i = 1; i < booth.votes.size; i++) {
				DotToLandmark b = booth.votes.get(i);
				if (b.count > best.count) {
					best = b;
				}
			}

			FoundDocument doc = foundMap.get(best.documentID);
			if (doc == null) {
				doc = resultsStorage.grow();
				doc.init(documents.get(best.documentID));
				foundMap.put(best.documentID, doc);
			}

			if (doc.landmarkHits.get(best.landmarkID) < best.count) {
				doc.landmarkHits.set(best.landmarkID, best.count);
				doc.landmarkToDots.set(best.landmarkID, dotIdx);
			}
		}

		foundMap.forEachEntry(( docID, doc ) -> {
			if (doc.countSeenLandmarks() >= minLandmarks) {
				output.add(doc);
			}
			return true;
		});
	}

	/**
	 * Place holder function for the document retrieval in the LLAH paper. Just throws an exception for now.
	 *
	 * @param dots observed dots
	 * @param output storage for found document
	 * @return true if successful
	 */
	public boolean lookUpDocument( List<Point2D_F64> dots, FoundDocument output ) {
		throw new RuntimeException("Implement");
	}

	/**
	 * Ensures that the points passed in is an acceptable size
	 */
	void checkListSize( List<Point2D_F64> locations2D ) {
		if (locations2D.size() < numberOfNeighborsN + 1)
			throw new IllegalArgumentException("There needs to be at least " + (numberOfNeighborsN + 1) + " points");
	}

	/**
	 * Computes the feature for the set of points and see if they match anything in the dictionary. If they do vote.
	 */
	private void lookupProcessor( List<Point2D_F64> pointSet, int dotIdx, LlahFeature featureComputed,
								  DogArray<DotVotingBooth> votingBooths ) {
		DotVotingBooth booth = votingBooths.get(dotIdx);

		// Compute the feature for this set
		hasher.computeHash(pointSet, featureComputed);

		// Find the set of features which match this has code
		LlahFeature foundFeat = hashTable.lookup(featureComputed.hashCode);
		while (foundFeat != null) {
			// Condition 1: See if the invariant's match
			if (featureComputed.doInvariantsMatch(foundFeat)) {
				DotToLandmark vote = booth.lookup(foundFeat.documentID, foundFeat.landmarkID);
				vote.count += 1;
			}

			foundFeat = foundFeat.next;
		}
	}

	/**
	 * Abstracts the inner most step when computing features
	 */
	interface ProcessPermutation {
		void process( int dotIdx, List<Point2D_F64> points );
	}

	public static class DotVotingBooth {
		// Stores the votes each dot casts for which document/feature it belongs to
		final DogArray<DotToLandmark> votes = new DogArray<>(DotToLandmark::new);
		// map from documentID to map from landmarkID to document
		final TIntObjectHashMap<TIntObjectHashMap<DotToLandmark>> map = new TIntObjectHashMap<>();

		// Recycles maps using in map.
		final DogArray<TIntObjectHashMap<DotToLandmark>> storageMaps =
				new DogArray<>(TIntObjectHashMap::new, TIntObjectHashMap::clear);

		public void reset() {
			votes.reset();
			map.clear();
			storageMaps.reset();
		}

		public DotToLandmark lookup( int documentID, int landmarkID ) {
			TIntObjectHashMap<DotToLandmark> voteDoc = map.get(documentID);

			if (voteDoc == null) {
				voteDoc = storageMaps.grow();
				map.put(documentID, voteDoc);
			}

			DotToLandmark vote = voteDoc.get(landmarkID);
			if (vote == null) {
				vote = votes.grow();
				vote.documentID = documentID;
				vote.landmarkID = landmarkID;
				vote.count = 0;
				voteDoc.put(landmarkID, vote);
			}

			return vote;
		}
	}

	public static class DotToLandmark {
		public int documentID;
		public int landmarkID;
		public int count;
	}

	/**
	 * Used to relate observed dots to landmarks in a document
	 */
	public static class DotCount {
		// index of dot in input array
		public int dotIdx;
		// how many times this dot was matched to this landmark
		public int counts;

		public void reset() {
			dotIdx = -1;
			counts = 0;
		}

		@Override
		public int hashCode() {
			return dotIdx;
		}
	}

	/**
	 * Documents that were found to match observed dots
	 */
	@SuppressWarnings({"NullAway.Init"})
	public static class FoundDocument {
		/** Which document */
		public LlahDocument document;

		/**
		 * Indicates the number of times a particular point was matched
		 */
		public final DogArray_I32 landmarkHits = new DogArray_I32();

		public final DogArray_I32 landmarkToDots = new DogArray_I32();

		public void init( LlahDocument document ) {
			this.document = document;
			final int totalLandmarks = document.landmarks.size;
			landmarkHits.resize(totalLandmarks);
			landmarkHits.fill(0);
			landmarkToDots.resize(totalLandmarks);
			landmarkToDots.fill(-1);
		}

		public boolean seenLandmark( int which ) {
			return landmarkHits.get(which) > 0;
		}

		public void lookupMatches( DogArray<PointIndex2D_F64> matches ) {
			matches.reset();
			for (int i = 0; i < landmarkHits.size; i++) {
				if (landmarkHits.get(i) > 0) {
					Point2D_F64 p = document.landmarks.get(i);
					matches.grow().setTo(p.x, p.y, i);
				}
			}
		}

		public int countSeenLandmarks() {
			int total = 0;
			for (int i = 0; i < landmarkHits.size; i++) {
				if (landmarkHits.get(i) > 0)
					total++;
			}
			return total;
		}

		public int countHits() {
			int total = 0;
			for (int i = 0; i < landmarkHits.size; i++) {
				total += landmarkHits.get(i);
			}
			return total;
		}
	}
}
