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

package boofcv.deepboof;

import boofcv.abst.scene.ImageClassifier;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import deepboof.Function;
import deepboof.graph.FunctionSequence;
import deepboof.tensors.Tensor_F32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for ImageClassifiers which implements common elements
 *
 * @author Peter Abeles
 */
public abstract class BaseImageClassifier implements ImageClassifier<Planar<GrayF32>> {

	protected FunctionSequence<Tensor_F32,Function<Tensor_F32>> network;

	// List of all the categories
	protected List<String> categories = new ArrayList<>();

	protected ImageType<Planar<GrayF32>> imageType = ImageType.pl(3,GrayF32.class);

	// Resizes input image for the network
	protected ClipAndReduce<Planar<GrayF32>> massage = new ClipAndReduce<>(true,imageType);

	// size of square image
	protected int imageSize;

	//  Input image adjusted to network input size
	protected Planar<GrayF32> imageRgb;

	// Storage for the tensor into the image
	protected Tensor_F32 tensorInput;
	protected Tensor_F32 tensorOutput;

	// storage for the final output
	protected FastQueue<Score> categoryScores = new FastQueue<>(Score.class,true);
	protected int categoryBest;

	Comparator<Score> comparator = new Comparator<Score>() {
		@Override
		public int compare(Score o1, Score o2) {
			if( o1.score < o2.score )
				return 1;
			else if( o1.score > o2.score )
				return -1;
			else
				return 0;
		}
	};

	public BaseImageClassifier( int imageSize ) {
		this.imageSize = imageSize;
		imageRgb = new Planar<>(GrayF32.class,imageSize,imageSize,3);
		tensorInput = new Tensor_F32(1,3,imageSize,imageSize);
	}

	@Override
	public ImageType<Planar<GrayF32>> getInputType() {
		return imageType;
	}

	/**
	 * The original implementation takes in an image then crops it randomly.  This is primarily for training but is
	 * replicated here to reduce the number of differences
	 *
	 * @param image Image being processed.  Must be RGB image.  Pixel values must have values from 0 to 255.
	 */
	@Override
	public void classify(Planar<GrayF32> image) {
		DataManipulationOps.imageToTensor(preprocess(image),tensorInput,0);
		innerProcess(tensorInput);
	}

	/**
	 * Massage the input image into a format recognized by the network
	 */
	protected Planar<GrayF32> preprocess(Planar<GrayF32> image) {
		// Shrink the image to input size
		if( image.width == imageSize && image.height == imageSize ) {
			this.imageRgb.setTo(image);
		} else if( image.width < imageSize || image.height < imageSize ) {
			throw new IllegalArgumentException("Image width or height is too small");
		} else {
			massage.massage(image,imageRgb);
		}
		return imageRgb;
	}


	protected void innerProcess( Tensor_F32 tensorInput ) {
		// process the tensor
		network.process(tensorInput,tensorOutput);

		// now find the best score and sort them
		categoryScores.reset();
		double scoreBest = -Double.MAX_VALUE;
		categoryBest = -1;
		for (int category = 0; category < tensorOutput.length(1); category++) {
			double score = tensorOutput.get(0,category);
			categoryScores.grow().set(score,category);
			if( score > scoreBest ) {
				scoreBest = score;
				categoryBest = category;
			}
		}

		// order the categories by most to least likely
		Collections.sort(categoryScores.toList(),comparator);
	}

	@Override
	public int getBestResult() {
		return categoryBest;
	}

	@Override
	public List<Score> getAllResults() {
		return categoryScores.toList();
	}

	@Override
	public List<String> getCategories() {
		return categories;
	}

	public Planar<GrayF32> getImageRgb() {
		return imageRgb;
	}
}
