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

package boofcv.abst.scene;

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Converts {@link FeatureSceneRecognition} into {@link SceneRecognition}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class WrapFeatureToSceneRecognition<Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
		implements SceneRecognition<Image> {
	/** Detects image features */
	protected @Getter @Setter DetectDescribePoint<Image, TD> detector;

	// Used to ensure the image is at the expected scale
	protected BoofLambdas.Transform<Image> downSample;

	/** The {@link FeatureSceneRecognition} */
	protected @Setter FeatureSceneRecognition<TD> recognizer;

	/** Optional reference to a config. Useful when saving to disk */
	public @Getter @Setter ConfigFeatureToSceneRecognition config;

	// Used to plug in detected image features to the scene recognition algorithm
	FeatureSceneRecognition.Features<TD> wrappedDetector = wrap();

	public WrapFeatureToSceneRecognition( DetectDescribePoint<Image, TD> detector,
										  BoofLambdas.Transform<Image> downSample,
										  FeatureSceneRecognition<TD> recognizer ) {
		this.detector = detector;
		this.downSample = downSample;
		this.recognizer = recognizer;
	}

	/**
	 * Wrap the image iterator by adding detection to it
	 */
	@Override public void learnModel( Iterator<Image> images ) {
		recognizer.learnModel(new Iterator<>() {
			@Override public boolean hasNext() {return images.hasNext();}

			@Override public FeatureSceneRecognition.Features<TD> next() {
				detector.detect(images.next());
				return wrappedDetector;
			}
		});
	}

	@Override public void clearDatabase() {
		recognizer.clearDatabase();
	}

	@Override public void addImage( String id, Image image ) {
		detector.detect(image);
		recognizer.addImage(id, wrappedDetector);
	}

	@Override
	public boolean query( Image queryImage, @Nullable BoofLambdas.Filter<String> filter, int limit, DogArray<Match> matches ) {
		detector.detect(queryImage);
		return recognizer.query(wrappedDetector, filter, limit, matches);
	}

	@Override public List<String> getImageIds( @Nullable List<String> storage ) {
		return recognizer.getImageIds(storage);
	}

	@Override public ImageType<Image> getImageType() {
		return detector.getInputType();
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> options ) {
		recognizer.setVerbose(out, options);
	}

	// @formatter:off
	private FeatureSceneRecognition.Features<TD> wrap() {
		return new FeatureSceneRecognition.Features<>() {
			@Override public Point2D_F64 getPixel( int index ) {return detector.getLocation(index);}
			@Override public TD getDescription( int index ) {return detector.getDescription(index);}
			@Override public int size() {return detector.getNumberOfFeatures();}
		};
	}
	// @formatter:on

	public <T extends FeatureSceneRecognition<TD>> T getRecognizer() {
		return (T)recognizer;
	}
}
