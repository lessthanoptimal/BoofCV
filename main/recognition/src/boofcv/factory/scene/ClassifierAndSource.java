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

package boofcv.factory.scene;

import boofcv.abst.scene.ImageClassifier;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import org.ddogleg.struct.Tuple2;

import java.util.List;

/**
 * Contains a classifier and where to download its models.  Each string in source is
 * an address where the model can be downloaded from.  If one fails then another
 * should be attempted.
 *
 * @author Peter Abeles
 */
public class ClassifierAndSource extends Tuple2<ImageClassifier<Planar<GrayF32>>,List<String>>
{
	public ImageClassifier<Planar<GrayF32>> getClassifier() {
		return data0;
	}

	public List<String> getSource() {
		return data1;
	}

}
