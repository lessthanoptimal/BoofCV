/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.io.points.impl;

import boofcv.struct.mesh.MeshPolygonAccess;
import georegression.fitting.plane.FitPlane3D_F64;
import georegression.struct.GeoTuple3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Writes a file that's in STL format when given a data structure that's wrapped by {@link MeshPolygonAccess} as input.
 */
public class StlFileWriter {
	// Use to find the normal for the surface
	FitPlane3D_F64 fitPlane = new FitPlane3D_F64();
	Point3D_F64 center = new Point3D_F64();
	Vector3D_F64 normal = new Vector3D_F64();

	// Storage for vectors from two of the sides
	Vector3D_F64 va = new Vector3D_F64();
	Vector3D_F64 vb = new Vector3D_F64();
	Vector3D_F64 tempN = new Vector3D_F64();

	// Workspace
	final byte[] line = new byte[80];

	/** Specifies number of digits in printf it should print the float. ASCII only */
	public String precision = "%.10f";

	/** With a bad polygon is detected this function is called. Replace with custom logic */
	public HandleBadPolygon errorHandler = ( w, e ) -> System.err.println("Bad Polygon: " + w + " " + e);

	/**
	 * Generates an STL file in ASCII format given a mesh in {@link MeshPolygonAccess} format.
	 *
	 * @param mesh (Input) Mesh accessor
	 * @param name (Input) What you want to call this mesh
	 * @param writer (Output) where it's written to
	 */
	public void writeAscii( MeshPolygonAccess mesh, String name, Writer writer ) throws IOException {
		// Massage the name to make it compatible with this format
		String nameMassaged = name.trim().replaceAll("\\s", "");
		// Add the name line for the solid. Remove all white spaces to avoid any issues
		writer.write("solid " + nameMassaged + "\n");

		// Pre-generate format strings at the desired precision
		String formatNorm = String.format("  facet normal %s %s %s\n", precision, precision, precision);
		String formatVert = String.format("      vertex %s %s %s\n", precision, precision, precision);

		var poly = new DogArray<>(Point3D_F64::new);
		for (int polygonIdx = 0; polygonIdx < mesh.size(); polygonIdx++) {
			mesh.getPolygon(polygonIdx, poly);

			if (!computeNormal(poly, polygonIdx))
				continue;

			// The polygon must lie on a plane and be convex. So we can use a simple algorithm to convert
			// it into triangles. Now these triangles might not be "optimal" in that some might be very skinny
			// when that could be avoided.

			Point3D_F64 p0 = poly.get(0);
			for (int idx1 = 3; idx1 <= poly.size; idx1++) {
				// All the triangles have the same normal vector
				writer.write(String.format(formatNorm, normal.x, normal.y, normal.z));
				writer.write("    outer loop\n");
				writer.write(String.format(formatVert, p0.x, p0.y, p0.z));
				int idx0 = idx1 - 2;
				for (int i = idx0; i < idx1; i++) {
					Point3D_F64 p = poly.get(i);
					writer.write(String.format(formatVert, p.x, p.y, p.z));
				}
				writer.write("    endloop\n");
				writer.write("  endfacet\n");
			}
		}

		writer.write("endsolid " + nameMassaged + "\n");
	}

	/**
	 * Generates an STL file in binary format given a mesh in {@link MeshPolygonAccess} format.
	 *
	 * @param mesh (Input) Mesh accessor
	 * @param name (Input) What you want to call this mesh
	 * @param output (Output) where it's saved to
	 */
	public void writeBinary( MeshPolygonAccess mesh, String name, OutputStream output ) throws IOException {
		// Zero the array so that old rand data isn't written
		Arrays.fill(line, (byte)0);

		final ByteBuffer bb = ByteBuffer.wrap(line);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.mark();

		var poly = new DogArray<>(Point3D_F64::new);

		// Massage the name to make it compatible with this format
		String nameMassaged = name.trim().replaceAll("\\s", "");

		// Copy the array into the 80 byte array then write it
		byte[] nameBytes = nameMassaged.getBytes(StandardCharsets.UTF_8);
		System.arraycopy(nameBytes, 0, line, 0, Math.min(nameBytes.length, 80));
		output.write(line, 0, 80);

		// Count number of triangles there are
		int totalTriangles = 0;
		for (int polygonIdx = 0; polygonIdx < mesh.size(); polygonIdx++) {
			mesh.getPolygon(polygonIdx, poly);
			totalTriangles += poly.size - 2;
		}

		// Save the number of triangles
		bb.putInt(totalTriangles);
		output.write(line, 0, 4);

		// Save all the triangles
		for (int polygonIdx = 0; polygonIdx < mesh.size(); polygonIdx++) {
			mesh.getPolygon(polygonIdx, poly);

			if (!computeNormal(poly, polygonIdx))
				continue;

			// The polygon must lie on a plane and be convex. So we can use a simple algorithm to convert
			// it into triangles. Now these triangles might not be "optimal" in that some might be very skinny
			// when that could be avoided.

			Point3D_F64 p0 = poly.get(0);
			for (int idx1 = 3; idx1 <= poly.size; idx1++) {
				// All the triangles have the same normal vector
				bb.reset();
				putGeoTuple3(bb, normal);
				putGeoTuple3(bb, p0);
				putGeoTuple3(bb, poly.get(idx1 - 2));
				putGeoTuple3(bb, poly.get(idx1 - 1));
				// Attribute byte count. spec says to set to zero
				bb.putShort((short)0);
				output.write(line, 0, bb.position());
			}
		}
	}

	/** Convenience function for writing a vector or point to the buffer */
	private void putGeoTuple3( ByteBuffer bb, GeoTuple3D_F64<?> n ) {
		bb.putFloat((float)n.x);
		bb.putFloat((float)n.y);
		bb.putFloat((float)n.z);
	}

	/**
	 * Carefully compute the normal of the polygon. Return false if the polygon is bad and should be skipped.
	 */
	boolean computeNormal( DogArray<Point3D_F64> poly, int polygonIdx ) {
		// Find the normal angle for the plane.
		if (poly.size == 3) {
			// Use a cross product since we can't do better than that
			va.minus(poly.get(1), poly.get(0));
			vb.minus(poly.get(2), poly.get(0));
			normal.crossSetTo(va, vb);
		} else {
			// If the N>3 case we can avoid degenerate situations by fitting a plane using all the points
			if (!fitPlane.svd(poly.toList(), center, normal)) {
				errorHandler.handleError(polygonIdx, "Failed to find normal");
				return false;
			}

			// Make sure the sign is correct
			// NOTE: If the first triangle is bad this will generate bad results. Look into doing a more
			//       stable approach, but it needs to still be fast
			va.minus(poly.get(1), poly.get(0));
			vb.minus(poly.get(2), poly.get(0));
			tempN.crossSetTo(va, vb);
			if (tempN.dot(normal) < 0) {
				normal.scale(-1);
			}
		}

		// Sanity check
		if (normal.isNaN()) {
			errorHandler.handleError(polygonIdx, "Normal has NaN");
			return false;
		}
		return true;
	}

	/**
	 * Specifies how errors are handled.
	 */
	@FunctionalInterface
	public interface HandleBadPolygon {
		void handleError( int which, String explanation );
	}
}
