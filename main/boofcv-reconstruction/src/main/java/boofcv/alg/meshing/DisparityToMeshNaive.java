/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.meshing;

import boofcv.abst.filter.binary.BinaryContourFinderLinearExternal;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.alg.shapes.polyline.splitmerge.PolylineSplitMerge;
import boofcv.factory.filter.binary.FactoryBinaryContourFinder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.geometry.polygon.ThreeIndexes;
import georegression.geometry.polygon.TriangulateSimpleRemoveEars_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;

import java.util.List;
import java.util.Objects;

/**
 * Very simple way to convert a disparity image into a mesh. Detects all regions with estimated disparity, fits
 * a polygon around them, lays a regular grid, triangulates, saves the result. Colorizing the vertices is not handled
 * here.
 *
 * @author Peter Abeles
 */
public class DisparityToMeshNaive {
	// parameters for fitting a polygon to the contour
	public int minSideLength = 10;
	public double cornerPenalty = 0.25;

	// binary image for finding regions with disparity
	final GrayU8 binary = new GrayU8(1, 1);

	/** All the found mesh */
	@Getter final DogArray<VertexMesh> meshes = new DogArray<>(VertexMesh::new, VertexMesh::reset);

	// finds the contour around regions in the image with known disparity
	BinaryContourFinderLinearExternal blobFinder = FactoryBinaryContourFinder.linearExternal();
	// Used to fit an approximate polygon around the contour
	PolylineSplitMerge fitPolygon = new PolylineSplitMerge();
	// Splits the polygon up into triangles
	TriangulateSimpleRemoveEars_F64 triangulator = new TriangulateSimpleRemoveEars_F64();

	// Workspace
	Point3D_F64 point3D = new Point3D_F64();
	DogArray<ThreeIndexes> triangles = new DogArray<>(ThreeIndexes::new);

	/**
	 * Processes the disparity image and returns all the found 3D meshes
	 *
	 * @param parameters Stereo parameters
	 * @param disparity disparity image
	 */
	public void process( DisparityParameters parameters, GrayF32 disparity ) {
		binary.reshapeTo(disparity);

		// Find all pixels with disparity values
		ThresholdImageOps.threshold(disparity, binary, parameters.disparityRange - 1, true);

		// Find all the blobs
		blobFinder.process(binary);

		List<ContourPacked> contours = blobFinder.getContours();
		DogArray<Point2D_I32> pixels = new DogArray<>(Point2D_I32::new);

		fitPolygon.setCornerScorePenalty(cornerPenalty);
		fitPolygon.setMinimumSideLength(minSideLength);
		fitPolygon.getMaxSideError().setFixed(Double.MAX_VALUE);
		fitPolygon.setMaxSides(1000);
		fitPolygon.setConvex(false);
		fitPolygon.setLoops(true);

		// TODO how to make a polygon simple?

		System.out.println("blobs.size=" + contours.size());
		for (int idxContour = 0; idxContour < 3; idxContour++) {
			ContourPacked cp = contours.get(idxContour);
			blobFinder.loadContour(cp.id, pixels);
			System.out.println("contour idx=" + idxContour + " pixels.size=" + pixels.size);


			if (!fitPolygon.process(pixels.toList())) {
				System.out.println("  Failed to fit polygon to contour!");
				continue;
			}

			PolylineSplitMerge.CandidatePolyline found = Objects.requireNonNull(fitPolygon.getBestPolyline());

			var polygon = new Polygon2D_F64(found.splits.size);
			for (int i = 0; i < polygon.size(); i++) {
				Point2D_I32 c = pixels.get(found.splits.get(i));
				polygon.get(i).setTo(c.x, c.y);
			}

			System.out.println("  finding triangles. poly.size=" + polygon.size()+" simple="+ UtilPolygons2D_F64.isSimple(polygon, null, 0.1).type);
			triangulator.process(polygon, triangles);

			// TODO split these triangles up even more by sampling at regular intervals and injecting triangles

			VertexMesh mesh = meshes.grow();
			for (int vertexIdx = 0; vertexIdx < polygon.size(); vertexIdx++) {
				Point2D_F64 pixel = polygon.get(vertexIdx);
				float value = disparity.get((int)(pixel.x + 0.5), (int)(pixel.y + 0.5));
				if (!parameters.pixelTo3D(pixel.x, pixel.y, value, point3D)) {
					throw new RuntimeException("Need to handle this");
				}
				mesh.vertexes.append(point3D);
			}

			mesh.triangles.reserve(triangles.size*3);
			for (int i = 0; i < triangles.size(); i++) {
				ThreeIndexes t = triangles.get(i);
				mesh.triangles.add(t.idx0);
				mesh.triangles.add(t.idx1);
				mesh.triangles.add(t.idx2);
			}
		}
	}
}
