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

package boofcv.alg.feature.detect.line;

import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.alg.feature.detect.line.gridline.GridLineModelDistance;
import boofcv.alg.feature.detect.line.gridline.GridLineModelFitter;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.MatrixOfList;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.fitting.line.ModelManagerLinePolar2D_F32;
import georegression.struct.line.LinePolar2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ModelMatcherPost;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contains tests for all implementations of {@link GridRansacLineDetector}
 *
 * @author Peter Abeles
 */
public abstract class CommonGridRansacLineDetectorChecks< D extends ImageGray<D>> extends BoofStandardJUnit {
	int width = 40;
	int height = 30;

	Class<D> derivType;

	protected CommonGridRansacLineDetectorChecks( Class<D> derivType) {
		this.derivType = derivType;
	}

	@Test void checkObvious() {
		for(int size = 11; size <= 19; size += 2 )
			checkObvious(size);
	}

	public abstract GridRansacLineDetector<D> createDetector( int regionSize, int maxDetectLines ,
															  ModelMatcher<LinePolar2D_F32, Edgel> robustMatcher );

	/**
	 * Give it a single straight line and see if it can detect it. Allow the region size to be changed to check
	 * for issues related to that
	 */
	protected void checkObvious( int regionSize ) {
//		System.out.println("regionSize = "+regionSize);
		int where = 25;
		GrayU8 edgeImage = new GrayU8(width,height);
		D derivX = GeneralizedImageOps.createSingleBand(derivType,width,height);
		D derivY = GeneralizedImageOps.createSingleBand(derivType,width,height);

		for( int i = 0; i < height; i++ ) {
			edgeImage.set(where,i,1);
			GeneralizedImageOps.set(derivX,where,i,20);
		}

		ModelManagerLinePolar2D_F32 manager = new ModelManagerLinePolar2D_F32();

		ModelMatcherPost<LinePolar2D_F32, Edgel> matcher = new Ransac<>(123123, 25, 1, manager,Edgel.class);
		matcher.setModel(()->new GridLineModelFitter(0.9f), ()->new GridLineModelDistance(0.9f));
		GridRansacLineDetector<D> alg = createDetector(regionSize,5,matcher);

		alg.process(derivX,derivY,edgeImage);

		MatrixOfList<LineSegment2D_F32> lines = alg.getFoundLines();

		assertEquals(width/regionSize,lines.getWidth());
		assertEquals(height/regionSize,lines.getHeight());

		int gridCol = where/regionSize;
		for( int i = 0; i < lines.height; i++ ) {
			List<LineSegment2D_F32> l = lines.get(gridCol,i);

			assertEquals(l.size(), 1);
			LineSegment2D_F32 a = l.get(0);
			assertTrue(Math.abs(a.slopeY())>1);
			assertTrue(Math.abs(a.slopeX())<0.01);
		}
	}

}
