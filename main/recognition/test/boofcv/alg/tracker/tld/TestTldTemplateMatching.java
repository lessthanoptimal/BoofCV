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

package boofcv.alg.tracker.tld;

import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ImageRectangle;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldTemplateMatching {

	int width = 60;
	int height = 80;

	Random rand = new Random(234);

	GrayU8 input = new GrayU8(width,height);

	InterpolatePixelS<GrayU8> interpolate = FactoryInterpolation.bilinearPixelS(GrayU8.class, BorderType.EXTENDED);

	public TestTldTemplateMatching() {
		ImageMiscOps.fillUniform(input, rand, 0, 200);
		interpolate.setImage(input);
	}

	@Test
	public void addDescriptor() {
		TldTemplateMatching alg = new TldTemplateMatching(interpolate);
		alg.setImage(input);

		alg.addDescriptor(true,new ImageRectangle(10,12,45,22));

		assertEquals(1,alg.getTemplatePositive().size());
		assertEquals(0,alg.getTemplateNegative().size());

		// adding another similar positive should fail
		alg.addDescriptor(true,new ImageRectangle(10,12,45,22));
		assertEquals(1,alg.getTemplatePositive().size());

		// don't add a negative which is very similar positive
		alg.addDescriptor(false,new ImageRectangle(10,12,45,22));

		assertEquals(1,alg.getTemplatePositive().size());
		assertEquals(0,alg.getTemplateNegative().size());

		// This negative should be added since it is different
		alg.addDescriptor(false,new ImageRectangle(23,12,55,22));

		assertEquals(1,alg.getTemplatePositive().size());
		assertEquals(1,alg.getTemplateNegative().size());

		alg.addDescriptor(false,new ImageRectangle(23,12,55,22));
		assertEquals(1,alg.getTemplateNegative().size());
	}

	@Test
	public void computeNccDescriptor() {
		TldTemplateMatching alg = new TldTemplateMatching(interpolate);
		alg.setImage(input);

		NccFeature found = alg.createDescriptor();
		alg.computeNccDescriptor(found,2,3,17,18);

		NccFeature expected = alg.createDescriptor();
		DescribePointPixelRegionNCC descriptor = FactoryDescribePointAlgs.pixelRegionNCC(15,15,GrayU8.class);
	    descriptor.setImage(input);
		descriptor.process(7+2,7+3,expected);

		assertEquals(expected.mean, found.mean, 1e-8);
	}

	@Test
	public void reset() {
		TldTemplateMatching alg = new TldTemplateMatching(interpolate);
		alg.setImage(input);

		alg.addDescriptor(true, new ImageRectangle(10, 12, 45, 22));
		alg.addDescriptor(false,new ImageRectangle(23,12,55,22));
		assertEquals(1,alg.getTemplatePositive().size());
		assertEquals(1,alg.getTemplateNegative().size());

		alg.reset();

		assertEquals(0,alg.getTemplatePositive().size());
		assertEquals(0,alg.getTemplateNegative().size());
		assertEquals(2,alg.unused.size());
	}

	@Test
	public void createDescriptor() {
		TldTemplateMatching alg = new TldTemplateMatching(interpolate);

		assertTrue(alg.createDescriptor() != null);
		alg.unused.push(new NccFeature(5));

		assertTrue(alg.createDescriptor() != null);
		assertEquals(0, alg.unused.size());
	}

	@Test
	public void computeConfidence() {
		TldTemplateMatching alg = new TldTemplateMatching(interpolate);
		alg.setImage(input);

		alg.addDescriptor(true, new ImageRectangle(2,3,17,18));
		alg.addDescriptor(false, new ImageRectangle(20,32,40,60));

		assertEquals(1,alg.computeConfidence(new ImageRectangle(2,3,17,18)),1e-8);
		assertEquals(0,alg.computeConfidence(new ImageRectangle(20,32,40,60)),1e-8);

		double found = alg.computeConfidence(new ImageRectangle(14, 30, 20, 50));
		assertTrue(found >= 0 && found <= 1);
	}

	@Test
	public void distance() {
		TldTemplateMatching alg = new TldTemplateMatching(interpolate);
		alg.setImage(input);

		NccFeature a = alg.createDescriptor();
		NccFeature b = alg.createDescriptor();
		NccFeature c = alg.createDescriptor();
		alg.computeNccDescriptor(a,2,3,17,18);
		alg.computeNccDescriptor(b,2,3,17,18);
		alg.computeNccDescriptor(c,20,32,40,60);

		List<NccFeature> list = new ArrayList<>();
		list.add(b);
		list.add(c);

		// same one should produce a distance of zero since the closest is identical
		assertEquals(0,alg.distance(a,list),1e-8);
		assertEquals(0,alg.distance(c,list),1e-8);

		// any random one should be between 0 and 1
		NccFeature d = alg.createDescriptor();
		alg.computeNccDescriptor(d,14,30,20,50);
		double found = alg.distance(d,list);
		assertTrue(found >= 0 && found <= 1);
	}
}
