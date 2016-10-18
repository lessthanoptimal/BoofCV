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

package boofcv.alg.bow;

import boofcv.struct.learning.ClassificationHistogram;
import boofcv.struct.learning.Confusion;

import java.io.File;
import java.util.*;

/**
 * Abstract class which provides a frame work for learning a scene classifier from a set of images.
 *
 * TODO describe how it provides learning
 *
 * @author Peter Abeles
 */
public abstract class LearnSceneFromFiles {

	protected Random rand;

	protected List<String> scenes = new ArrayList<>();

	// The minimum number of images in each type of set
	int minimumTrain;
	int minimumCross;
	int minimumTest;

	// how to divide the input set up
	double fractionTrain;
	double fractionCross;

	// maps for each set of images
	protected Map<String,List<String>> train;
	protected Map<String,List<String>> cross;
	protected Map<String,List<String>> test;

	public Confusion evaluateTest() {
		return evaluate(test);
	}

	/**
	 * Given a set of images with known classification, predict which scene each one belongs in and compute
	 * a confusion matrix for the results.
	 *
	 * @param set Set of classified images
	 * @return Confusion matrix
	 */
	protected Confusion evaluate( Map<String,List<String>> set ) {
		ClassificationHistogram histogram = new ClassificationHistogram(scenes.size());

		int total = 0;
		for (int i = 0; i < scenes.size(); i++) {
			total += set.get(scenes.get(i)).size();
		}
		System.out.println("total images "+total);

		for (int i = 0; i < scenes.size(); i++) {
			String scene = scenes.get(i);

			List<String> images = set.get(scene);
			System.out.println("  "+scene+" "+images.size());
			for (String image : images) {
				int predicted = classify(image);
				histogram.increment(i, predicted);
			}
		}

		return histogram.createConfusion();
	}

	/**
	 * Given an image compute which scene it belongs to
	 * @param path Path to input image
	 * @return integer corresponding to the scene
	 */
	protected abstract int classify( String path );


	public void loadSets( File dirTraining, File dirCross , File dirTest ) {
		train = findImages(dirTraining);
		if( dirCross != null )
			cross = findImages(dirCross);
		test = findImages(dirTest);

		extractKeys(train);
		extractKeys(test);
	}

	private void extractKeys( Map<String,List<String>> images ) {
		Set<String> keys = images.keySet();

		for( String key : keys ) {
			if( !scenes.contains(key)) {
				scenes.add(key);
			}
		}
	}

	public void loadThenSplit( File directory ) {
		Map<String,List<String>> all = findImages(directory);
		train = new HashMap<>();
		if( fractionCross != 0 )
			cross = new HashMap<>();
		test = new HashMap<>();

		Set<String> keys = all.keySet();

		for( String key : keys ) {
			List<String> allImages = all.get(key);

			// randomize the ordering to remove bias
			Collections.shuffle(allImages,rand);

			int numTrain = (int)(allImages.size()*fractionTrain);
			numTrain = Math.max(minimumTrain,numTrain);
			int numCross = (int)(allImages.size()*fractionCross);
			numCross = Math.max(minimumCross,numCross);
			int numTest = allImages.size()-numTrain-numCross;

			if( numTest < minimumTest )
				throw new RuntimeException("Not enough images to create test set. "+key+" total = "+allImages.size());

			createSubSet(key, allImages, train, 0, numTrain);
			if( cross != null ) {
				createSubSet(key, allImages, cross , numTrain, numCross+numTrain);
			}
			createSubSet(key, allImages, test, numCross+numTrain,allImages.size());
		}

		scenes.addAll(keys);
	}

	private void createSubSet(String key, List<String> allImages, Map<String,List<String>> subset ,
							  int start , int end ) {
		List<String> trainImages = new ArrayList<>();
		for (int i = start; i < end; i++) {
			trainImages.add(allImages.get(i));
		}
		subset.put(key, trainImages);
	}

	/**
	 * Loads the paths to image files contained in subdirectories of the root directory.  Each sub directory
	 * is assumed to be a different category of images.
	 */
	public static Map<String,List<String>> findImages( File rootDir ) {
		File files[] = rootDir.listFiles();
		if( files == null )
			return null;

		List<File> imageDirectories = new ArrayList<>();
		for( File f : files ) {
			if( f.isDirectory() ) {
				imageDirectories.add(f);
			}
		}
		Map<String,List<String>> out = new HashMap<>();
		for( File d : imageDirectories ) {
			List<String> images = new ArrayList<>();

			files = d.listFiles();
			if( files == null )
				throw new RuntimeException("Should be a directory!");

			for( File f : files ) {
				if( f.isHidden() || f.isDirectory() || f.getName().endsWith(".txt") ) {
					continue;
				}

				images.add( f.getPath() );
			}

			String key = d.getName().toLowerCase();

			out.put(key,images);
		}

		return out;
	}

	public List<String> getScenes() {
		return scenes;
	}
}
