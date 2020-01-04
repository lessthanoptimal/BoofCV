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

package boofcv.alg.feature.describe.llah;

import boofcv.alg.nn.KdTreePoint2D_F64;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import org.ddogleg.combinatorics.Combinations;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.sorting.QuickSort_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.HashSet;
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
	@Getter final FastQueue<LlahDocument> documents = new FastQueue<>(LlahDocument::new);

	//========================== Internal working variables
	final NearestNeighbor<Point2D_F64> nn = FactoryNearestNeighbor.kdtree(new KdTreePoint2D_F64());
	private final NearestNeighbor.Search<Point2D_F64> search = nn.createSearch();
	private final FastQueue<NnData<Point2D_F64>> resultsNN = new FastQueue<>(NnData::new);
	final List<Point2D_F64> neighbors = new ArrayList<>();
	private final double[] angles;
	private final QuickSort_F64 sorter = new QuickSort_F64();
	private final FastQueue<FoundDocument> resultsStorage = new FastQueue<>(FoundDocument::new);
	private final TIntObjectHashMap<FoundDocument> foundMap = new TIntObjectHashMap<>();

	private final FastQueue<LlahFeature> allFeatures;

	// Used to compute all the combinations of a set
	private final Combinations<Point2D_F64> combinator = new Combinations<>();

	// recycle to avoid garbage collectior
	FastQueue<DotCount> storageD2L = new FastQueue<>(DotCount::new);

	/**
	 * Configures the LLAH feature computation
	 *
	 * @param numberOfNeighborsN Number of neighbors to be considered
	 * @param sizeOfCombinationM Number of different combinations within the neighbors
	 * @param hasher Computes the hash code
	 */
	public LlahOperations( int numberOfNeighborsN , int sizeOfCombinationM,
						   LlahHasher hasher ) {
		this.numberOfNeighborsN = numberOfNeighborsN;
		this.sizeOfCombinationM = sizeOfCombinationM;
		this.numberOfInvariants = hasher.getNumberOfInvariants(sizeOfCombinationM);
		this.hasher = hasher;

		angles = new double[numberOfNeighborsN];
		allFeatures = new FastQueue<>(()->new LlahFeature(numberOfInvariants));
	}

	/**
	 * Forgets all the documents and recycles data
	 */
	public void clearDocuments() {
		documents.reset();
		allFeatures.reset();
	}

	/**
	 * Learns the hashing function from the set of point sets
	 * @param pointSets Point sets. Each set represents one document
	 * @param numDiscrete Number of discrete values the invariant is converted to
	 * @param histogramLength Number of elements in the histogram. 100,000 is recommended
	 * @param maxInvariantValue The maximum number of value an invariant is assumed to have.
	 *                          For affine ~25. Cross Ratio
	 */
	public void learnHashing(Iterable<List<Point2D_F64>> pointSets , int numDiscrete ,
							 int histogramLength,double maxInvariantValue ) {

		// to make the math faster use a fine grained array with more extreme values than expected
		int[] histogram = new int[histogramLength];

		// Storage for computed invariants
		double[] invariants = new double[numberOfInvariants];

		// Go through each point and compute some invariants from it
		for( var locations2D : pointSets ) {
			nn.setPoints(locations2D,false);

			computeAllFeatures(locations2D, (idx,l)-> {
				hasher.computeInvariants(l,invariants,0);

				for (int i = 0; i < invariants.length; i++) {
					int j = Math.min(histogram.length-1,(int)(histogram.length*invariants[i]/maxInvariantValue));
					histogram[j]++;
				}
			});
		}

		// Sanity check
		double endFraction = histogram[histogram.length-1]/(double)IntStream.of(histogram).sum();
		double maxAllowed = 0.5/numDiscrete;
		if( endFraction > maxAllowed )
			System.err.println("WARNING: last element in histogram has a significant count. " +endFraction+" > "+maxAllowed+
					" maxInvariantValue should be increased");

		hasher.learnDiscretization(histogram,histogram.length,maxInvariantValue,numDiscrete);
	}

	/**
	 * Creates a new document from the 2D points. The document and points are added to the hash table
	 * for later retrieval.
	 *
	 * @param locations2D Location of points inside the document
	 * @return The document which was added to the hash table.
	 */
	public LlahDocument createDocument(List<Point2D_F64> locations2D ) {
		checkListSize(locations2D);

		LlahDocument doc = documents.grow();
		doc.reset();
		doc.documentID = documents.size()-1;

		// copy the points
		for (Point2D_F64 p : locations2D) {
			doc.landmarks.grow().set(p);
		}
		computeAllFeatures(locations2D, (idx,l) -> createProcessor(doc, idx));

		return doc;
	}

	/**
	 * Computes the maximum number of unique hash code a point can have.
	 */
	public long computeMaxUniqueHashPerPoint() {
		long comboHash = Combinations.computeTotalCombinations(numberOfNeighborsN,sizeOfCombinationM);
		return comboHash*sizeOfCombinationM;
	}

	private void createProcessor(LlahDocument doc, int idx) {
		// Given this set compute the feature
		LlahFeature feature = allFeatures.grow();
		hasher.computeHash(permuteM,feature);

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
	void computeAllFeatures(List<Point2D_F64> locations2D, ProcessPermutation processor ) {
		// set up nn search
		nn.setPoints(locations2D,false);

		// Compute the features for all points in this document
		for (int pointID = 0; pointID < locations2D.size(); pointID++) {

			findNeighbors(locations2D.get(pointID));

			// All combinations of size M from neighbors
			combinator.init(neighbors, sizeOfCombinationM);
			do {
				setM.clear();
				for (int i = 0; i < sizeOfCombinationM; i++) {
					setM.add( combinator.get(i) );
				}

				// Cyclical permutations of 'setM'
				// When you look it up you won't know the order points are observed in
				for (int i = 0; i < sizeOfCombinationM; i++) {
					permuteM.clear();
					for (int j = 0; j < sizeOfCombinationM; j++) {
						int idx = (i+j)%sizeOfCombinationM;
						permuteM.add(setM.get(idx));
					}

					processor.process(pointID,permuteM);
				}
			} while( combinator.next() );
		}
	}

	/**
	 * Finds all the neighbors
	 */
	void findNeighbors(Point2D_F64 target) {
		// Find N nearest-neighbors of p0
		search.findNearest(target,-1, numberOfNeighborsN+1,resultsNN);

		// Find the neighbors, removing p0
		neighbors.clear();
		for (int i = 0; i < resultsNN.size; i++) {
			Point2D_F64 n = resultsNN.get(i).point;
			if( n == target ) // it will always find the p0 point
				continue;
			neighbors.add(n);
		}

		// Compute the angle of each neighbor
		for (int i = 0; i < neighbors.size(); i++) {
			Point2D_F64 n = neighbors.get(i);
			angles[i] = Math.atan2(n.y-target.y, n.x-target.x);
		}

		// sort the neighbors in clockwise order
		sorter.sort(angles,angles.length,neighbors);
	}

	/**
	 * Looks up all the documents which match observed features.
	 * @param dots Observed feature locations
	 * @param threshold A threshold from 0 to 1, inclusive. 0 = everything passes. 1 = reject everything
	 * @param output Storage for results. WARNING: Results are recycled on next call!
	 */
	public void lookupDocuments( List<Point2D_F64> dots , double threshold, List<FoundDocument> output ) {
		checkListSize(dots);

		storageD2L.reset();
		output.clear();
		foundMap.clear();
		resultsStorage.reset();

		// Used to keep track of what has been seen and what has not been seen
		var knownFeatures = new HashSet<LlahFeature>();
		var featureComputed = new LlahFeature(numberOfInvariants);

		// Compute features, look up matching known features, then vote
		computeAllFeatures(dots, (dotIdx,pointSet)->
				lookupProcessor(foundMap, knownFeatures, featureComputed,pointSet,dotIdx));

		int minimumHits = (int)Math.round(computeMaxUniqueHashPerPoint()*threshold);
		foundMap.forEachEntry((docID,foundDoc)->{
			int totalValid = 0;
			for (int i = 0; i < foundDoc.landmarkHits.size; i++) {
				// Skip if the point was not found
				if( !foundDoc.seenLandmark(i) )
					continue;

				if( foundDoc.landmarkHits.data[i] >= minimumHits ) {
					totalValid++;
				} else {
					// mark it as not seen if below the threshold
					foundDoc.landmarkHits.data[i] = 0;
				}
			}
			if( totalValid > 0 ) {
				output.add(foundDoc);
			}
			return true;
		});
	}

	/**
	 * Ensures that the points passed in is an acceptable size
	 */
	void checkListSize(List<Point2D_F64> locations2D) {
		if (locations2D.size() < numberOfNeighborsN + 1)
			throw new IllegalArgumentException("There needs to be at least " + (numberOfNeighborsN + 1) + " points");
	}

	/**
	 * Computes the feature for the set of points and see if they match anything in the dictionary. If they do vote.
	 */
	private void lookupProcessor(TIntObjectHashMap<FoundDocument> output, HashSet<LlahFeature> knownFeatures,
								 LlahFeature featureComputed, List<Point2D_F64> pointSet, int dotIdx)
	{
		// Compute the feature for this set
		hasher.computeHash(pointSet,featureComputed);

		// Find the set of features which match this has code
		LlahFeature foundFeat = hashTable.lookup(featureComputed.hashCode);
		while( foundFeat != null ) {
			// Condition 1: See if the invariant's match
			if( !featureComputed.doInvariantsMatch(foundFeat) ) {
				foundFeat = foundFeat.next;
				continue;
			}

			// Condition 2: Make sure this known feature hasn't already been counted
			if( knownFeatures.contains(foundFeat)) {
				foundFeat = foundFeat.next;
				continue;
			} else {
				knownFeatures.add(foundFeat);
			}

			// get results for this document
			FoundDocument results = output.get(foundFeat.documentID);
			if( results == null ) {
				results = resultsStorage.grow();
				results.init(documents.get(foundFeat.documentID));
				output.put(foundFeat.documentID,results);
			}

			// note which dot referenced this landmark
			TIntObjectHashMap<DotCount> dots = results.landmarkToDots.get(foundFeat.landmarkID);
			DotCount d2l = dots.get(dotIdx);
			if( d2l == null ) {
				d2l = storageD2L.grow();
				d2l.reset();
				d2l.dotIdx = dotIdx;
				dots.put(dotIdx,d2l);
			}
			d2l.counts++;

			// note which point matched this document
			results.landmarkHits.data[foundFeat.landmarkID]++;

			// Condition 3: Abort after a match was found to ensure featureComputed is only matched once
			break;
		}
	}

	/**
	 * Abstracts the inner most step when computing features
	 */
	interface ProcessPermutation
	{
		void process( int dotIdx, List<Point2D_F64> points );
	}

	/**
	 * Used to relate observed dots to landmarks in a document
	 */
	public static class DotCount
	{
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
	public static class FoundDocument {
		/** Which document */
		public LlahDocument document;

		/**
		 * Indicates the number of times a particular point was matched
		 */
		public final GrowQueue_I32 landmarkHits = new GrowQueue_I32();
		/**
		 * Used to see which dots have been matched to this document and how often
		 */
		public final FastQueue<TIntObjectHashMap<DotCount>> landmarkToDots =
				new FastQueue<>(TIntObjectHashMap::new);

		public void init( LlahDocument document) {
			this.document = document;
			final int totalLandmarks = document.landmarks.size;
			landmarkHits.resize(totalLandmarks);
			landmarkHits.fill(0);

			landmarkToDots.resize(totalLandmarks);
			for (int i = 0; i < totalLandmarks; i++) {
				landmarkToDots.get(i).clear();
			}
		}

		public boolean seenLandmark( int which ) {
			return landmarkHits.get(which) > 0;
		}

		public void lookupMatches(FastQueue<PointIndex2D_F64> matches ) {
			matches.reset();
			for (int i = 0; i < landmarkHits.size; i++) {
				if( landmarkHits.get(i) > 0 ) {
					var p = document.landmarks.get(i);
					matches.grow().set(p.x,p.y,i);
				}
			}
		}

		public int countSeenLandmarks() {
			int total = 0;
			for (int i = 0; i < landmarkHits.size; i++) {
				if( landmarkHits.get(i) > 0 )
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
