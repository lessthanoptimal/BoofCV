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

package boofcv.alg.scene.nister2006;

import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree.Node;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import gnu.trove.impl.Constants;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.util.Collections;
import java.util.List;

/**
 * Image recognition based off of [1] using inverted files. A {@link HierarchicalVocabularyTree} is assumed to hav
 * e been already trained. When an image is added to the database a TF-IDF descriptor is computed using the tree
 * and then added to the relevant tree's leaves. When an image is looked up its TF-IDF descriptor is found then
 * all images in the data base are found that share at least one leaf node. These candidate matches are then
 * compared against each other and scored using L2-Norm.
 *
 * <p>
 * [1] Nister, David, and Henrik Stewenius. "Scalable recognition with a vocabulary tree."
 * 2006 IEEE Computer Society Conference on Computer Vision and Pattern Recognition (CVPR'06). Vol. 2. Ieee, 2006.
 * </p>
 *
 * @author Peter Abeles
 */
public class RecognitionVocabularyTreeNister2006<Point> {

	/** Vocabulary Tree */
	public @Getter HierarchicalVocabularyTree<Point, LeafData> tree;

	/** List of images added to the database */
	protected @Getter final DogArray<ImageInfo> imagesDB = new DogArray<>(ImageInfo::new, ImageInfo::reset);
	/** Scores for all candidate images which have been sorted */
	protected @Getter final DogArray<Match> matchScores = new DogArray<>(Match::new, Match::reset);

	/** Distance between two TF-IDF descriptors. L1 and L2 norms are provided */
	protected @Getter @Setter DistanceFunction distanceFunction = this::distanceL1Norm;

	//---------------- Internal Workspace

	// The "frequency" that nodes in the tree appear in this image
	protected final DogArray<Frequency> frequencies = new DogArray<>(Frequency::new, Frequency::reset);
	protected final TIntObjectMap<Frequency> node_to_frequency = new TIntObjectHashMap<>();

	// Temporary storage for an image's TF description while it's being looked up
	protected final TIntFloatMap tempDescTermFreq = new TIntFloatHashMap();

	// Predeclare array for storing keys. Avoids unnecessary array creation
	protected final DogArray_I32 keysDesc = new DogArray_I32(); // ONLY use in describe()
	protected final DogArray_I32 keysDist = new DogArray_I32(); // ONLY use in distance functions.
	protected final DogArray_I32 keysLookUp = new DogArray_I32(); // ONLY use in lookUp

	// Set to store common non-zero key between two descriptors
	protected final TIntSet commonKeys = new TIntHashSet();

	/**
	 * Configures the tree by adding LeafData to all the leaves in the tree then saves a reference for future use
	 *
	 * @param tree Three which is to be used as the database. Saved internally.
	 */
	public void initializeTree( HierarchicalVocabularyTree<Point, LeafData> tree ) {
		this.tree = tree;
		for (int i = 0; i < tree.nodes.size; i++) {
			if (!tree.nodes.get(i).isLeaf())
				continue;
			tree.addData(tree.nodes.get(i), new LeafData());
		}
	}

	/**
	 * Adds a new image to the database.
	 *
	 * @param imageID The image's unique ID for later reference
	 * @param imageFeatures Feature descriptors from an image
	 * @param cookie Optional user defined data which will be attached to the image
	 */
	public void addImage( int imageID, List<Point> imageFeatures, Object cookie ) {
		if (imageFeatures.isEmpty())
			return;

		ImageInfo info = imagesDB.grow();
		info.imageId = imageID;
		info.cookie = cookie;

		// compute a descriptor for this image while adding it to the leaves
		describe(imageFeatures, info.descTermFreq, ( leafNode ) -> {
			// Add the image info to the leaf if it hasn't already been added
			LeafData leafData = tree.listData.get(leafNode.dataIdx);
			if (!leafData.images.containsKey(info.imageId)) {
				leafData.images.put(info.imageId, info);
			}
		});
	}

	/**
	 * Looks up the best match from the data base. The list of all potential matches can be accessed by calling
	 * {@link #getMatchScores()}.
	 *
	 * @param imageFeatures Feature descriptors from an image
	 * @return The best matching image with score from the database
	 */
	public boolean lookup( List<Point> imageFeatures ) {
		TIntSet candidates = new TIntHashSet();
		matchScores.reset();

		// Can't match to anything if it's empty
		if (imageFeatures.isEmpty()) {
			return false;
		}

		// Create a description of this image and collect potential matches from leaves
		describe(imageFeatures, tempDescTermFreq, ( leafNode ) -> {
			// Create a list of images that there's at least one leaf matching
			LeafData leafData = tree.listData.get(leafNode.dataIdx);
			keysLookUp.resize(leafData.images.size());
			leafData.images.keys(keysLookUp.data);
			for (int i = 0; i < keysLookUp.size(); i++) {
				ImageInfo c = leafData.images.get(keysLookUp.get(i));
				if (!candidates.add(c.imageId))
					continue;
				matchScores.grow().image = c;
			}
		});

		if (matchScores.isEmpty())
			return false;

		for (int i = 0; i < matchScores.size(); i++) {
			Match m = matchScores.get(i);
			m.error = distanceL2Norm(tempDescTermFreq, m.image.descTermFreq);
		}

		Collections.sort(matchScores.toList());

		return true;
	}

	/**
	 * Given the image features, compute a sparse descriptor for the image and pass in leaf nodes to 'op' for each
	 * image feature.
	 *
	 * @param imageFeatures (Input) All image features in the image
	 * @param descTermFreq (Output) Sparse TF-IDF descriptor for the image
	 * @param op Each leaf node for each feature is passed into this function
	 */
	protected void describe( List<Point> imageFeatures, TIntFloatMap descTermFreq, BoofLambdas.ProcessObject<Node> op ) {
		// Reset work variables
		frequencies.reset();
		node_to_frequency.clear();
		descTermFreq.clear();

		// Sum of the weight of all graph nodes it sees
		for (int descIdx = 0; descIdx < imageFeatures.size(); descIdx++) {
			int leafNodeIdx = tree.searchPathToLeaf(imageFeatures.get(descIdx), ( node ) -> {
				if (node.weight == 0.0)
					return;
				Frequency f = node_to_frequency.get(node.id);
				if (f == null) {
					f = frequencies.grow();
					f.node = node;
					node_to_frequency.put(node.id, f);
				}
				f.sum += node.weight;
			});

			// Process the leaf node in the passed in operation
			op.process(tree.nodes.get(leafNodeIdx));
		}

		// No nodes with a non-zero weight that matched was found
		if (node_to_frequency.isEmpty())
			return;

		//------ Create the TF-IDF descriptor for this image
		// Normalize the vector such that the L2-norm is 1.0
		keysDesc.resize(node_to_frequency.size());
		node_to_frequency.keys(keysDesc.data);
		double normL2 = 0.0;
		for (int i = 0; i < keysDesc.size(); i++) {
			double x = node_to_frequency.get(keysDesc.get(i)).sum;
			normL2 += x*x;
		}
		normL2 = Math.sqrt(normL2);
		BoofMiscOps.checkTrue(normL2 != 0.0, "Sum of weights is zero. Something went very wrong");

		for (int i = 0; i < keysDesc.size(); i++) {
			Frequency f = node_to_frequency.get(keysDesc.get(i));
			descTermFreq.put(f.node.id, (float)(f.sum/normL2));
		}
	}

	/** Used to change distance function to one of the built in types */
	public void setDistanceType( DistanceTypes type ) {
		switch (type) {
			case L1 -> distanceFunction = this::distanceL1Norm;
			case L2 -> distanceFunction = this::distanceL2Norm;
		}
	}

	/**
	 * Computes L2-Norm for score between the two descriptions. Searches for common non-zero elements between
	 * the two then uses the simplified equation from [1].
	 */
	public float distanceL2Norm( TIntFloatMap descA, TIntFloatMap descB ) {
		// Get the key and make sure it doesn't declare new memory
		keysDist.resize(descA.size());
		descA.keys(keysDist.data);

		// Compute dot product of common non-zero elements
		float sum = 0.0f;
		for (int keyIdx = 0; keyIdx < keysDist.size; keyIdx++) {
			int key = keysDist.data[keyIdx];

			float valueA = descA.get(key);
			float valueB = descB.get(key);
			if (valueB <= 0.0f)
				continue;

			sum += valueA*valueB;
		}

		// max to avoid floating point error causing it to go slightly negative
		return (float)Math.sqrt(Math.max(0.0f, 2.0f - 2.0f*sum)); // TODO should it use L2-norm squared for speed?
	}

	/**
	 * Computes the L1-norm distance between the descriptors. This is more complex to compute
	 * but according to the paper provides better results.
	 */
	public float distanceL1Norm( TIntFloatMap descA, TIntFloatMap descB ) {
		// Find the set of common keys between the two descriptors
		findCommonKeys(descA, descB, commonKeys, keysDist);

		// Look up the common keys
		keysDist.resize(commonKeys.size());
		commonKeys.toArray(keysDist.data);

		// L1-norm is the sum of the difference magnitude of each element
		float sum = 0.0f;
		for (int keyIdx = 0; keyIdx < keysDist.size; keyIdx++) {
			int key = keysDist.data[keyIdx];

			// If a key doesn't exist in the descriptor the default value is -1, but the actual value
			// is 0.0, hence the max.
			float valueA = Math.max(0.0f, descA.get(key));
			float valueB = Math.max(0.0f, descB.get(key));

			sum += Math.abs(valueA-valueB);
		}

		return sum;
	}

	/**
	 * Finds common keys between the two descriptors
	 */
	private void findCommonKeys( TIntFloatMap descA, TIntFloatMap descB, TIntSet commonKeys,
								 DogArray_I32 work ) {
		commonKeys.clear();

		// Add keys from descA
		work.resize(descA.size());
		descA.keys(work.data);
		for (int i = 0; i < work.size; i++) {
			commonKeys.add(work.get(i));
		}

		// Add keys from descB
		work.resize(descB.size());
		descB.keys(work.data);
		for (int i = 0; i < work.size; i++) {
			commonKeys.add(work.get(i));
		}
	}

	/** Information about an image stored in the database */
	public static class ImageInfo {
		/** TF-IDF description of the image. Default -1 for no key and no value. */
		public TIntFloatMap descTermFreq = new TIntFloatHashMap(
				Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);

		/** Use specified data associated with this image */
		public Object cookie;
		/** Unique ID for this image */
		public int imageId;

		public <T> T getCookie() {
			return (T)cookie;
		}

		public void reset() {
			descTermFreq.clear();
			cookie = null;
			imageId = -1;
		}
	}

	// Used to sum the frequency of words (graph nodes) in the image
	protected static class Frequency {
		// sum of weights
		double sum;
		// The node which is referenced
		Node node;

		public void reset() {
			sum = 0;
			node = null;
		}
	}

	/**
	 * Match and score information.
	 */
	public static class Match implements Comparable<Match> {
		/** Fit error. 0.0 = perfect. */
		public float error;
		/** Reference to the image in the data base that was matched */
		public ImageInfo image;

		public void reset() {
			error = 0;
			image = null;
		}

		@Override public int compareTo( Match o ) {
			return Float.compare(error, o.error);
		}
	}

	public static class LeafData {
		/** images at this leaf. key=imageID value=ImageInfo */
		public TIntObjectMap<ImageInfo> images = new TIntObjectHashMap<>();
	}

	/** Different built in distance norms. */
	public enum DistanceTypes {L1,L2}

	/** Computes the distance between two TF-IDF descriptors */
	public interface DistanceFunction {
		/**
		 * Distance between the two descriptors. All empty elements in the maps are assumed to be zero, but
		 * as a precondition the must return -1.
		 */
		float distance( TIntFloatMap descA, TIntFloatMap descB );
	}
}
