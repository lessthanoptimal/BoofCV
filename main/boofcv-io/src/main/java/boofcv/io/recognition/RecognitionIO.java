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

package boofcv.io.recognition;

import boofcv.BoofVersion;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.io.UtilIO;
import boofcv.struct.feature.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

/**
 * Reading and writing data structures related to recognition.
 *
 * @author Peter Abeles
 **/
public class RecognitionIO {
	/**
	 * Saves the tree in binary format. The format is described in an ascii header.
	 *
	 * @param tree (Input) Tree that's saved
	 * @param out (Output) Stream the tree is written to
	 */
	public static <TD extends TupleDesc<TD>> void saveBin( HierarchicalVocabularyTree<TD, ?> tree, OutputStream out ) {
		String header = "BOOFCV_HIERARCHICAL_VOCABULARY_TREE\n";
		header += "# Graph format: id=int,parent=int,branch=int,descIdx=int,dataIdx=int,weight=double,children.size=int,children=int[]\n";
		header += "# Description format: raw array used internally\n";
		header += "format_version 1\n";
		header += "boofcv_version " + BoofVersion.VERSION + "\n";
		header += "git_sha " + BoofVersion.GIT_SHA + "\n";
		header += "branch_factor " + tree.branchFactor + "\n";
		header += "maximum_level " + tree.maximumLevel + "\n";
		header += "nodes.size " + tree.nodes.size + "\n";
		header += "descriptions.size " + tree.descriptions.size() + "\n";
		header += "data.size " + tree.listData.size + "\n";
		header += "point_type " + tree.descriptions.getElementType().getSimpleName() + "\n";
		header += "point_dof " + tree.descriptions.getTemp(0).size() + "\n";
		header += "distance.name " + tree.distanceFunction.getClass().getName() + "\n";
		header += "BEGIN_GRAPH\n";
		try {
			out.write(header.getBytes(StandardCharsets.UTF_8));

			DataOutputStream dout = new DataOutputStream(out);
			for (int nodeIdx = 0; nodeIdx < tree.nodes.size; nodeIdx++) {
				HierarchicalVocabularyTree.Node n = tree.nodes.get(nodeIdx);
				dout.writeInt(n.id);
				dout.writeInt(n.parent);
				dout.writeInt(n.branch);
				dout.writeInt(n.descIdx);
				dout.writeInt(n.dataIdx);
				dout.writeDouble(n.weight);
				dout.writeInt(n.childrenIndexes.size);
				for (int i = 0; i < n.childrenIndexes.size; i++) {
					dout.writeInt(n.childrenIndexes.get(i));
				}
			}

			dout.writeUTF("BEGIN_DESCRIPTIONS\n");
			for (int nodeIdx = 0; nodeIdx < tree.nodes.size; nodeIdx++) {
				writeBin(tree.descriptions.getTemp(nodeIdx), dout);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <TD extends TupleDesc<TD>> void loadBin( InputStream in, HierarchicalVocabularyTree<TD, ?> tree ) {
		var builder = new StringBuilder();
		try {
			String line = UtilIO.readLine(in, builder);
			if (!line.equals("BOOFCV_HIERARCHICAL_VOCABULARY_TREE"))
				throw new IOException("Unexpected first line. line.length=" + line.length());

			String pointType = "";
			int dof = 0;
			int numDescriptions = 0;
			while (true) {
				line = UtilIO.readLine(in, builder);
				if (line.startsWith("BEGIN_"))
					break;
				if (line.startsWith("#"))
					continue;
				String[] words = line.split("\\s");
				if (words[0].equals("branch_factor")) {
					tree.branchFactor = Integer.parseInt(words[1]);
				} else if (words[0].equals("maximum_level")) {
					tree.maximumLevel = Integer.parseInt(words[1]);
				} else if (words[0].equals("nodes.size")) {
					tree.nodes.resize(Integer.parseInt(words[1]));
				} else if (words[0].equals("descriptions.size")) {
					numDescriptions = Integer.parseInt(words[1]);
				} else if (words[0].equals("point_type")) {
					pointType = words[1];
				} else if (words[0].equals("point_dof")) {
					dof = Integer.parseInt(words[1]);
				}
			}

			if (!pointType.equals(tree.descriptions.getElementType().getSimpleName()))
				throw new IllegalArgumentException("Unexpected point type: expected=" + pointType +
						" found=" + tree.descriptions.getElementType().getSimpleName());

			TD tuple = tree.descriptions.getElementType().getConstructor(int.class).newInstance(dof);

			DataInputStream input = new DataInputStream(in);
			for (int nodeIdx = 0; nodeIdx < tree.nodes.size; nodeIdx++) {
				HierarchicalVocabularyTree.Node n = tree.nodes.get(nodeIdx);
				n.id = input.readInt();
				n.parent = input.readInt();
				n.branch = input.readInt();
				n.descIdx = input.readInt();
				n.dataIdx =input.readInt();;
				n.weight = input.readDouble();
				n.childrenIndexes.resize(input.readInt());
				for (int i = 0; i < n.childrenIndexes.size; i++) {
					n.childrenIndexes.data[i] = input.readInt();
				}
			}

			line = input.readUTF();
			if (!line.startsWith("BEGIN_"))
				throw new IOException("Expected BEGIN_");

			for (int i = 0; i < numDescriptions; i++) {
				readBin(tuple, input);
				tree.descriptions.addCopy(tuple);
			}
		} catch (IOException | NoSuchMethodException | IllegalAccessException |
				InstantiationException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static <TD extends TupleDesc<TD>> void writeBin( TD tuple, DataOutputStream dout ) throws IOException {
		if (tuple instanceof TupleDesc_F64) {
			var desc = (TupleDesc_F64)tuple;
			for (int i = 0; i < desc.size(); i++) {
				dout.writeDouble(desc.value[i]);
			}
		} else if (tuple instanceof TupleDesc_F32) {
			var desc = (TupleDesc_F32)tuple;
			for (int i = 0; i < desc.size(); i++) {
				dout.writeFloat(desc.value[i]);
			}
		} else if (tuple instanceof TupleDesc_I8) {
			var desc = (TupleDesc_I8)tuple;
			dout.write(desc.value, 0, desc.size());
		} else if (tuple instanceof TupleDesc_B) {
			var desc = (TupleDesc_B)tuple;
			for (int i = 0; i < desc.value.length; i++) {
				dout.writeInt(desc.value[i]);
			}
		} else {
			throw new IllegalArgumentException("Unknown type " + tuple.getClass().getSimpleName());
		}
	}

	public static <TD extends TupleDesc<TD>> void readBin(TD tuple, DataInputStream in ) throws IOException {
		if (tuple instanceof TupleDesc_F64) {
			var desc = (TupleDesc_F64)tuple;
			for (int i = 0; i < desc.value.length; i++) {
				desc.value[i] = in.readDouble();
			}
		} else if (tuple instanceof TupleDesc_F32) {
			var desc = (TupleDesc_F32)tuple;
			for (int i = 0; i < desc.value.length; i++) {
				desc.value[i] = in.readFloat();
			}
		} else if (tuple instanceof TupleDesc_B) {
			var desc = (TupleDesc_B)tuple;
			for (int i = 0; i < desc.value.length; i++) {
				desc.value[i] = in.readInt();
			}
		} else {
			throw new IllegalArgumentException("Unknown type " + tuple.getClass().getSimpleName());
		}
	}

	public static <TD extends TupleDesc<TD>> void saveBin( RecognitionVocabularyTreeNister2006<TD> db, OutputStream out ) {
		HierarchicalVocabularyTree<TD, RecognitionVocabularyTreeNister2006.LeafData> tree = db.getTree();

		String header = "BOOFCV_RECOGNITION_NISTER_2006\n";
		header += "format_version 1\n";
		header += "boofcv_version " + BoofVersion.VERSION + "\n";
		header += "git_sha " + BoofVersion.GIT_SHA + "\n";
		header += "images_db.size " + db.getImagesDB().size + "\n";
		header += "leaf_data.size " + tree.listData.size + "\n";
		header += "BEGIN_IMAGE_DB\n";
		// TODO save imagesDB
		header += "BEGIN_LEAF_INFO\n";
		// TODO save leafInfo
		saveBin(tree, out);
	}
}
