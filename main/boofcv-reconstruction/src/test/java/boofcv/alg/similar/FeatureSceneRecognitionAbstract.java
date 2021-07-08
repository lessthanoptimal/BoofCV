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

package boofcv.alg.similar;

import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.TupleDesc;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Provides default implementations for all functions.
 *
 * @author Peter Abeles
 */
public class FeatureSceneRecognitionAbstract<TD extends TupleDesc<TD>> implements FeatureSceneRecognition<TD> {
	// @formatter:off
	@Override public void learnModel( Iterator<Features<TD>> images ) {}
	@Override public void clearDatabase() {}
	@Override public void addImage( String id, Features<TD> features ) {}
	@Override public List<String> getImageIds( @Nullable List<String> storage ) {return null;}
	@Override public boolean query( Features<TD> query, BoofLambdas.Filter<String> filter,
									int limit, DogArray<SceneRecognition.Match> matches ) {return false;}
	@Override public int getQueryWord( int featureIdx ) {return 0;}
	@Override public void getQueryWords( int featureIdx, DogArray_I32 words ) {}
	@Override public int lookupWord( TD description ) {return 0;}
	@Override public void lookupWords( TD description, DogArray_I32 word ) {}
	@Override public int getTotalWords() {return 1;}
	@Override public Class<TD> getDescriptorType() {return null;}
	@Override public void setVerbose( @Nullable PrintStream printStream, @Nullable Set<String> set ) {}
	// @formatter:on
}
