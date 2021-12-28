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

package boofcv.abst.scene.ann;

import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.alg.scene.ann.RecognitionNearestNeighborInvertedFile;
import boofcv.alg.scene.bow.BowMatch;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.struct.FactoryTupleDesc;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.PackedArray;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.kmeans.FactoryTupleCluster;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.clustering.kmeans.StandardKMeans;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.Factory;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around {@link RecognitionNearestNeighborInvertedFile} for {@link FeatureSceneRecognition}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class FeatureSceneRecognitionNearestNeighbor<TD extends TupleDesc<TD>> implements FeatureSceneRecognition<TD> {
	/** Configuration */
	@Getter ConfigRecognitionNearestNeighbor config;

	/** Model for the words */
	@Getter NearestNeighbor<TD> nearestNeighbor;

	/** BOW algorithm and storage for image database */
	@Getter RecognitionNearestNeighborInvertedFile<TD> database;

	/** List of all the words */
	@Getter List<TD> dictionary = new ArrayList<>();

	/** Stores features found in one image */
	@Getter @Setter DogArray<TD> imageFeatures;

	/** List of all the images in the dataset */
	@Getter List<String> imageIds = new ArrayList<>();

	/** Performance tuning. If less than this number of features a single thread algorithm will be used */
	@Getter @Setter public int minimumForThread = 500; // This value has not been proven to be optimal

	// Internal Profiling. All times in milliseconds
	@Getter long timeLearnDescribeMS;
	@Getter long timeLearnClusterMS;

	// Describes how to store the feature descriptor
	@Getter Class<TD> tupleType;
	@Getter int tupleDOF;

	// If not null then print verbose information
	@Nullable PrintStream verbose;

	public FeatureSceneRecognitionNearestNeighbor( ConfigRecognitionNearestNeighbor config, Factory<TD> factory ) {
		this.config = config;
		this.imageFeatures = new DogArray<>(factory);
		this.database = new RecognitionNearestNeighborInvertedFile<>();

		database.setDistanceType(config.distanceNorm);

		tupleDOF = imageFeatures.grow().size();
		tupleType = (Class)imageFeatures.get(0).getClass();
	}

	@Override public void learnModel( Iterator<Features<TD>> images ) {
		PackedArray<TD> packedFeatures = FactoryTupleDesc.createPackedBig(tupleDOF, tupleType);

		// Keep track of where features from one image begins/ends
		DogArray_I32 startIndex = new DogArray_I32();

		// Detect features in all the images and save into a single array
		long time0 = System.currentTimeMillis();
		while (images.hasNext()) {
			Features<TD> image = images.next();
			startIndex.add(packedFeatures.size());
			int N = image.size();
			packedFeatures.reserve(N);
			for (int i = 0; i < N; i++) {
				packedFeatures.append(image.getDescription(i));
			}
			if (verbose != null)
				verbose.println("described.size=" + startIndex.size + " features=" + N + " packed.size=" + packedFeatures.size());
		}
		startIndex.add(packedFeatures.size());
		if (verbose != null) verbose.println("packedFeatures.size=" + packedFeatures.size());
		long time1 = System.currentTimeMillis();
		timeLearnDescribeMS = time1 - time0;

		// Learn the words by clustering. This could take a while
		StandardKMeans<TD> clustering = FactoryTupleCluster.kmeans(config.kmeans, minimumForThread, tupleDOF, tupleType);
		if (verbose != null) clustering.setVerbose(true);
		clustering.initialize(config.randSeed);
		clustering.process(packedFeatures, config.numberOfWords);
		long time2 = System.currentTimeMillis();
		timeLearnClusterMS = time2 - time1;

		// The dictionary is now defined. Initialize the dataset
		setDictionary(clustering.getBestClusters().toList());
	}

	@Override public void clearDatabase() {
		imageIds.clear();
		database.clearImages();
	}

	@Override public void addImage( String id, Features<TD> features ) {
		// Copy image features into an array
		imageFeatures.resize(features.size());
		for (int i = 0; i < imageFeatures.size; i++) {
			imageFeatures.get(i).setTo(features.getDescription(i));
		}

		// Save the ID and convert into a format the database understands
		int imageIndex = imageIds.size();
		imageIds.add(id);

		if (verbose != null)
			verbose.println("added[" + imageIndex + "].size=" + features.size() + " id=" + id);

		// Add the image
		database.addImage(imageIndex, imageFeatures.toList());
	}

	@Override public List<String> getImageIds( @Nullable List<String> storage ) {
		if (storage == null)
			storage = new ArrayList<>();
		else
			storage.clear();

		storage.addAll(imageIds);
		return storage;
	}

	@Override
	public boolean query( Features<TD> query,
						  BoofLambdas.@Nullable Filter<String> filter, int limit,
						  DogArray<SceneRecognition.Match> matches ) {

		// Default is no matches
		matches.resize(0);

		// Handle the case where the limit is unlimited
		limit = limit <= 0 ? Integer.MAX_VALUE : limit;

		// Detect image features then copy features into an array
		imageFeatures.resize(query.size());
		for (int i = 0; i < imageFeatures.size; i++) {
			imageFeatures.get(i).setTo(query.getDescription(i));
		}

		// Wrap the user provided filter by converting the int ID into a String ID
		BoofLambdas.FilterInt filterInt = filter == null ? null : ( index ) -> filter.keep(imageIds.get(index));

		// Look up the closest matches
		if (!database.query(imageFeatures.toList(), filterInt, limit))
			return false;

		DogArray<BowMatch> found = database.getMatches();

		if (verbose != null) verbose.println("matches.size=" + found.size + " best.error=" + found.get(0).error);

		// Copy results into output format
		matches.resize(found.size);
		for (int i = 0; i < matches.size; i++) {
			BowMatch f = found.get(i);
			matches.get(i).id = imageIds.get(f.identification);
			matches.get(i).error = f.error;
		}

		return !matches.isEmpty();
	}

	/**
	 * Replaces the old dictionary with the new dictionary.
	 *
	 * @param dictionary Dictionary of words
	 */
	public void setDictionary( List<TD> dictionary ) {
		clearDatabase();
		this.dictionary = dictionary;
		NearestNeighbor<TD> nearestNeighbor = FactoryNearestNeighbor.generic(config.nearestNeighbor,
				FactoryAssociation.kdtreeDistance(tupleDOF, tupleType));
		nearestNeighbor.setPoints(dictionary, true);

		database.initialize(nearestNeighbor, dictionary.size());
	}

	@Override public int getQueryWord( int featureIdx ) {
		return database.observedWords.get(featureIdx);
	}

	@Override public void getQueryWords( int featureIdx, DogArray_I32 words ) {
		words.reset();
		words.add(database.observedWords.get(featureIdx));
	}

	@Override public int lookupWord( TD description ) {
		database.search.findNearest(description, -1, database.searchResult);
		return database.searchResult.index;
	}

	@Override public void lookupWords( TD description, DogArray_I32 words ) {
		words.reset();
		words.add(lookupWord(description));
	}

	@Override public int getTotalWords() {
		return config.numberOfWords;
	}

	@Override public Class<TD> getDescriptorType() {
		return tupleType;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> config ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
