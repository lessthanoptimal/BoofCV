/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.alg.sfm.structure.PairwiseImageGraph.CameraMotion;
import boofcv.alg.sfm.structure.PairwiseImageGraph.CameraView;
import boofcv.struct.calib.CameraPinhole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestPairwiseImageGraph {
	@Test
	public void findCameraMotions() {
		PairwiseImageGraph graph = new PairwiseImageGraph();
		graph.cameras.put("cameraA",new PairwiseImageGraph.Camera("cameraA",null,new CameraPinhole()));
		graph.cameras.put("cameraB",new PairwiseImageGraph.Camera("cameraB",null,new CameraPinhole()));

		graph.nodes.add( new CameraView(0,null));
		graph.nodes.add( new CameraView(0,null));
		graph.nodes.add( new CameraView(0,null));
		graph.nodes.add( new CameraView(0,null));

		graph.nodes.get(0).camera = graph.cameras.get("cameraA");
		graph.nodes.get(1).camera = graph.cameras.get("cameraA");
		graph.nodes.get(2).camera = graph.cameras.get("cameraB");
		graph.nodes.get(3).camera = graph.cameras.get("cameraB");

		graph.edges.add( new CameraMotion());
		graph.edges.add( new CameraMotion());
		graph.edges.add( new CameraMotion());

		graph.edges.get(0).viewSrc = graph.nodes.get(0);
		graph.edges.get(0).viewDst = graph.nodes.get(2);
		graph.edges.get(1).viewSrc = graph.nodes.get(0);
		graph.edges.get(1).viewDst = graph.nodes.get(1);
		graph.edges.get(2).viewSrc = graph.nodes.get(3);
		graph.edges.get(2).viewDst = graph.nodes.get(0);

		List<CameraMotion> found =  graph.findCameraMotions(graph.cameras.get("cameraA"),null);
		assertEquals(1,found.size());
		assertTrue(graph.edges.get(1)==found.get(0));

		found =  graph.findCameraMotions(graph.cameras.get("cameraB"),null);
		assertEquals(0,found.size());

	}

	@Test
	public void CameraMotion_destination() {
		CameraMotion motion = new CameraMotion();
		CameraView viewA = new CameraView(0,null);
		CameraView viewB = new CameraView(0,null);
		CameraView viewC = new CameraView(0,null);

		motion.viewSrc = viewA;
		motion.viewDst = viewB;

		assertSame(viewA, motion.destination(viewB));
		assertSame(viewB, motion.destination(viewA));

		try {
			motion.destination(viewC);
			fail("Exception should have been thrown");
		} catch( RuntimeException ignore ) {}
	}
}