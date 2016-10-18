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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.FastQueue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/**
 * @author Peter Abeles
 */
public class BenchmarkAssociationSpeedSurf {

	static final String image1 = UtilIO.pathExample("apartment_building_01.jpg");
	static final String image2 = UtilIO.pathExample("apartment_building_02.jpg");


	static final long TEST_TIME = 1000;

	FastQueue<TupleDesc_F64> listA;
	FastQueue<TupleDesc_F64> listB;

	DetectDescribePoint<GrayF32,TupleDesc_F64> detector;

	public BenchmarkAssociationSpeedSurf() {
		detector = (DetectDescribePoint)FactoryDetectDescribe.surfStable(null, null, null, GrayF32.class);
		listA = createSet(image1);
		listB = createSet(image2);
		
		System.out.println("Size A = "+listA.size()+"  B = "+listB.size());
	}

	public Performer createProfile( String name, AssociateDescription<TupleDesc_F64> alg ) {
		return new General(name,alg);
	}

	public class General implements Performer {

		AssociateDescription<TupleDesc_F64> alg;
		String name;

		public General(String name, AssociateDescription<TupleDesc_F64> alg) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			alg.setSource(listA);
			alg.setDestination(listB);
			alg.associate();
		}

		@Override
		public String getName() {
			return name;
		}
	}

	private FastQueue<TupleDesc_F64> createSet( String imageName ) {

		try {
			BufferedImage image = ImageIO.read(new File(imageName));
			GrayF32 gray = ConvertBufferedImage.convertFrom(image, (GrayF32) null);

			FastQueue<TupleDesc_F64> ret = new FastQueue<>(10, TupleDesc_F64.class, false);

			detector.detect(gray);
			
			for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
				ret.add( detector.getDescription(i).copy() );
			}
			
			return ret;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main( String argsp[ ] ) {
		BenchmarkAssociationSpeedSurf app = new BenchmarkAssociationSpeedSurf();

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class,true);
		
		int DOF = app.detector.createDescription().size();

		ProfileOperation.printOpsPerSec(app.createProfile("Greedy",
				FactoryAssociation.greedy(score, Double.MAX_VALUE,  false)),TEST_TIME);
		ProfileOperation.printOpsPerSec(app.createProfile("Greedy Backwards",
				FactoryAssociation.greedy(score, Double.MAX_VALUE,  true)),TEST_TIME);
		ProfileOperation.printOpsPerSec(app.createProfile("Random Forest",
				FactoryAssociation.kdRandomForest(DOF, 500, 15, 5, 1233445565)),TEST_TIME);
		
	}
}
