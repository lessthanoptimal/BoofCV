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
import boofcv.abst.scene.nister2006.ConfigSceneRecognitionNister2006;
import boofcv.abst.scene.nister2006.SceneRecognitionNister2006;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.InvertedFile;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.PackedArray;
import boofcv.struct.feature.*;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.kmeans.TuplePointDistanceEuclideanSq;
import boofcv.struct.kmeans.TuplePointDistanceHamming;
import org.ddogleg.clustering.PointDistance;
import org.ddogleg.struct.BigDogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Reading and writing data structures related to recognition.
 *
 * @author Peter Abeles
 **/
public class RecognitionIO {
	/**
	 * Saves {@link SceneRecognitionNister2006} to disk inside of the specified directory
	 *
	 * @param def What is to be saved
	 * @param dir Direction that it is to be saved
	 */
	public static <TD extends TupleDesc<TD>>
	void saveNister2006( SceneRecognitionNister2006<?, TD> def, File dir ) {
		if (dir.exists() && !dir.isDirectory())
			throw new IllegalArgumentException("Destination must not exist or be a directory");
		if (!dir.exists())
			BoofMiscOps.checkTrue(dir.mkdirs());

		UtilIO.saveConfig(def.getConfig(), new File(dir, "config.yaml"));
		saveTreeBin(def.getDatabaseN(), new File(dir, "database.bin"));
		UtilIO.saveListStringYaml(def.getImageIds(), new File(dir, "image_ids.yaml"));
	}

	/**
	 * Loads {@link SceneRecognitionNister2006}
	 *
	 * @param dir Where it has been saved
	 * @param imageType The type of image it will process. This should match what it was trained on
	 * @return a new instance loaded from disk
	 */
	public static <Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
	SceneRecognitionNister2006<Image, TD> loadNister2006( File dir, ImageType<Image> imageType ) {
		if (!dir.exists())
			throw new IllegalArgumentException("Directory doesn't exist: " + dir.getPath());
		if (!dir.isDirectory())
			throw new IllegalArgumentException("Path is not a directory: " + dir.getPath());

		ConfigSceneRecognitionNister2006 config = UtilIO.loadConfig(new File(dir, "config.yaml"));
		var alg = new SceneRecognitionNister2006<Image, TD>(config, imageType);
		loadTreeBin(new File(dir, "database.bin"), alg.getDatabaseN());
		alg.getImageIds().addAll(UtilIO.loadListStringYaml(new File(dir, "image_ids.yaml")));

		// Need to do this so that the tree reference is correctly set up
		alg.setDatabase(alg.getDatabaseN());
		return alg;
	}

	public static <TD extends TupleDesc<TD>> void saveBin( HierarchicalVocabularyTree<TD> tree, File file ) {
		try {
			var out = new FileOutputStream(file);
			saveTreeBin(tree, out);
			out.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <TD extends TupleDesc<TD>>
	HierarchicalVocabularyTree<TD> loadTreeBin( File file, @Nullable HierarchicalVocabularyTree<TD> tree ) {
		try {
			var in = new FileInputStream(file);
			tree = loadTreeBin(in, tree);
			in.close();
			return tree;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves the tree in binary format. The format is described in an ascii header.
	 *
	 * @param tree (Input) Tree that's saved
	 * @param out (Output) Stream the tree is written to
	 */
	public static <TD extends TupleDesc<TD>> void saveTreeBin( HierarchicalVocabularyTree<TD> tree, OutputStream out ) {
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
		header += "point_type " + tree.descriptions.getElementType().getSimpleName() + "\n";
		header += "point_dof " + tree.descriptions.getTemp(0).size() + "\n";
		header += "distance.name " + tree.distanceFunction.getClass().getName() + "\n";
		header += "BEGIN_GRAPH\n";
		try {
			out.write(header.getBytes(StandardCharsets.UTF_8));

			DataOutputStream dout = new DataOutputStream(out);
			for (int nodeIdx = 0; nodeIdx < tree.nodes.size; nodeIdx++) {
				HierarchicalVocabularyTree.Node n = tree.nodes.get(nodeIdx);
				dout.writeInt(n.index);
				dout.writeInt(n.parent);
				dout.writeInt(n.branch);
				dout.writeInt(n.descIdx);
				dout.writeInt(n.userIdx);
				dout.writeDouble(n.weight);
				dout.writeInt(n.childrenIndexes.size);
				for (int i = 0; i < n.childrenIndexes.size; i++) {
					dout.writeInt(n.childrenIndexes.get(i));
				}
			}

			dout.writeUTF("BEGIN_DESCRIPTIONS");
			for (int nodeIdx = 0; nodeIdx < tree.descriptions.size(); nodeIdx++) {
				writeBin(tree.descriptions.getTemp(nodeIdx), dout);
			}
			dout.writeUTF("END_BOOFCV_HIERARCHICAL_VOCABULARY_TREE");
			dout.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <TD extends TupleDesc<TD>>
	HierarchicalVocabularyTree<TD> loadTreeBin( InputStream in, @Nullable HierarchicalVocabularyTree<TD> tree ) {
		var builder = new StringBuilder();
		try {
			String line = UtilIO.readLine(in, builder);
			if (!line.equals("BOOFCV_HIERARCHICAL_VOCABULARY_TREE"))
				throw new IOException("Unexpected first line. line.length=" + line.length());

			String pointType = "";
			String distanceClass = "";
			int dof = 0;
			int numDescriptions = 0;
			int branchFactor = 0;
			int maximumLevel = 0;
			int nodesSize = 0;

			while (true) {
				line = UtilIO.readLine(in, builder);
				if (line.equals("BEGIN_GRAPH"))
					break;
				if (line.startsWith("#"))
					continue;
				String[] words = line.split("\\s");
				if (words[0].equals("branch_factor")) {
					branchFactor = Integer.parseInt(words[1]);
				} else if (words[0].equals("maximum_level")) {
					maximumLevel = Integer.parseInt(words[1]);
				} else if (words[0].equals("nodes.size")) {
					nodesSize = Integer.parseInt(words[1]);
				} else if (words[0].equals("descriptions.size")) {
					numDescriptions = Integer.parseInt(words[1]);
				} else if (words[0].equals("point_type")) {
					pointType = words[1];
				} else if (words[0].equals("point_dof")) {
					dof = Integer.parseInt(words[1]);
				} else if (words[0].equals("distance.name")) {
					distanceClass = words[1];
				}
			}

			TD tuple;
			{
				PointDistance<TD> distanceFunction;
				PackedArray<TD> descriptions;

				switch (pointType) {
					case "TupleDesc_F64" -> {
						distanceFunction = (PointDistance)new TuplePointDistanceEuclideanSq.F64();
						descriptions = (PackedArray)new PackedTupleBigArray_F64(dof);
						tuple = (TD)new TupleDesc_F64(dof);
					}
					case "TupleDesc_F32" -> {
						distanceFunction = (PointDistance)new TuplePointDistanceEuclideanSq.F32();
						descriptions = (PackedArray)new PackedTupleBigArray_F32(dof);
						tuple = (TD)new TupleDesc_F32(dof);
					}
					case "TupleDesc_U8" -> {
						distanceFunction = (PointDistance)new TuplePointDistanceEuclideanSq.U8();
						descriptions = (PackedArray)new PackedTupleBigArray_U8(dof);
						tuple = (TD)new TupleDesc_U8(dof);
					}
					case "TupleDesc_S8" -> {
						distanceFunction = (PointDistance)new TuplePointDistanceEuclideanSq.S8();
						descriptions = (PackedArray)new PackedTupleBigArray_U8(dof);
						tuple = (TD)new TupleDesc_S8(dof);
					}
					case "TupleDesc_B" -> {
						distanceFunction = (PointDistance)new TuplePointDistanceHamming();
						descriptions = (PackedArray)new PackedTupleBigArray_B(dof);
						tuple = (TD)new TupleDesc_B(dof);
					}
					default -> throw new IOException("Unknown point type. " + pointType);
				}

				if (tree == null)
					tree = new HierarchicalVocabularyTree<>(distanceFunction, descriptions);
				else {
					tree.distanceFunction = distanceFunction;
				}

				if (!tree.distanceFunction.getClass().getName().equals(distanceClass))
					throw new IOException("Distance functions do not match: Expected=" + distanceClass);
			}

			tree.branchFactor = branchFactor;
			tree.maximumLevel = maximumLevel;
			tree.nodes.resize(nodesSize);

			DataInputStream input = new DataInputStream(in);
			for (int nodeIdx = 0; nodeIdx < tree.nodes.size; nodeIdx++) {
				HierarchicalVocabularyTree.Node n = tree.nodes.get(nodeIdx);
				n.index = input.readInt();
				n.parent = input.readInt();
				n.branch = input.readInt();
				n.descIdx = input.readInt();
				n.userIdx = input.readInt();
				n.weight = input.readDouble();
				n.childrenIndexes.resize(input.readInt());
				for (int i = 0; i < n.childrenIndexes.size; i++) {
					n.childrenIndexes.data[i] = input.readInt();
				}
			}

			readCheckUTF(input, "BEGIN_DESCRIPTIONS");

			for (int i = 0; i < numDescriptions; i++) {
				readBin(tuple, input);
				tree.descriptions.append(tuple);
			}

			readCheckUTF(input, "END_BOOFCV_HIERARCHICAL_VOCABULARY_TREE");

			return tree;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <TD extends TupleDesc<TD>> void writeBin( TD tuple, DataOutputStream dout ) throws IOException {
		if (tuple instanceof TupleDesc_F64) {
			var desc = (TupleDesc_F64)tuple;
			for (int i = 0; i < desc.size(); i++) {
				dout.writeDouble(desc.data[i]);
			}
		} else if (tuple instanceof TupleDesc_F32) {
			var desc = (TupleDesc_F32)tuple;
			for (int i = 0; i < desc.size(); i++) {
				dout.writeFloat(desc.data[i]);
			}
		} else if (tuple instanceof TupleDesc_I8) {
			var desc = (TupleDesc_I8)tuple;
			dout.write(desc.data, 0, desc.size());
		} else if (tuple instanceof TupleDesc_B) {
			var desc = (TupleDesc_B)tuple;
			for (int i = 0; i < desc.data.length; i++) {
				dout.writeInt(desc.data[i]);
			}
		} else {
			throw new IllegalArgumentException("Unknown type " + tuple.getClass().getSimpleName());
		}
	}

	public static <TD extends TupleDesc<TD>> void readBin( TD tuple, DataInputStream in ) throws IOException {
		if (tuple instanceof TupleDesc_F64) {
			var desc = (TupleDesc_F64)tuple;
			for (int i = 0; i < desc.size(); i++) {
				desc.data[i] = in.readDouble();
			}
		} else if (tuple instanceof TupleDesc_F32) {
			var desc = (TupleDesc_F32)tuple;
			for (int i = 0; i < desc.size(); i++) {
				desc.data[i] = in.readFloat();
			}
		} else if (tuple instanceof TupleDesc_I8) {
			var desc = (TupleDesc_I8)tuple;
			BoofMiscOps.checkEq(desc.data.length, in.read(desc.data, 0, desc.data.length));
		} else if (tuple instanceof TupleDesc_B) {
			var desc = (TupleDesc_B)tuple;
			for (int i = 0; i < desc.data.length; i++) {
				desc.data[i] = in.readInt();
			}
		} else {
			throw new IllegalArgumentException("Unknown type " + tuple.getClass().getSimpleName());
		}
	}

	public static <TD extends TupleDesc<TD>> void saveTreeBin( RecognitionVocabularyTreeNister2006<TD> db, File file ) {
		try {
			var out = new BufferedOutputStream(new FileOutputStream(file), 1024*1024);
			saveBin(db, out);
			out.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <TD extends TupleDesc<TD>>
	void loadTreeBin( File file, RecognitionVocabularyTreeNister2006<TD> db ) {
		try {
			var in = new BufferedInputStream(new FileInputStream(file), 1024*1024);
			loadBin(in, db);
			in.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Saves {@link RecognitionVocabularyTreeNister2006} to a binary format.
	 *
	 * @param db (Input) Structure to be encoded
	 * @param out Stream it's written to
	 */
	public static <TD extends TupleDesc<TD>> void saveBin( RecognitionVocabularyTreeNister2006<TD> db, OutputStream out ) {
		HierarchicalVocabularyTree<TD> tree = db.getTree();
		Objects.requireNonNull(tree, "Tree must be specified before it can be saved");

		String header = "BOOFCV_RECOGNITION_NISTER_2006\n";
		header += "# Image DB: id=int,descTermFreq.size=int,array[key=int,value=float]\n";
		header += "# Leaf Info: images.size=int,images.data=array[int]\n";
		header += "format_version 1\n";
		header += "boofcv_version " + BoofVersion.VERSION + "\n";
		header += "git_sha " + BoofVersion.GIT_SHA + "\n";
		header += "images_db.size " + db.getImagesDB().size + "\n";
		header += "BEGIN_TREE\n";

		try {
			out.write(header.getBytes(StandardCharsets.UTF_8));

			// Save the tree next since we need that structure to decode everything that comes after it
			saveTreeBin(tree, out);

			var dout = new DataOutputStream(out);
			dout.writeUTF("BEGIN_IMAGE_DB");
			BigDogArray_I32 imageDB = db.getImagesDB();
			for (int dbIdx = 0; dbIdx < imageDB.size; dbIdx++) {
				dout.writeInt(imageDB.get(dbIdx));
			}

			dout.writeUTF("BEGIN_INVERTED_FILES");
			BoofMiscOps.checkEq(db.invertedFiles.size(), tree.nodes.size);
			for (int nodeIdx = 0; nodeIdx < db.invertedFiles.size(); nodeIdx++) {
				InvertedFile node = db.invertedFiles.get(nodeIdx);
				BoofMiscOps.checkEq(node.size, node.weights.size);

				dout.writeInt(node.size());
				for (int i = 0; i < node.size; i++) {
					dout.writeInt(node.get(i));
				}
				for (int i = 0; i < node.weights.size; i++) {
					dout.writeFloat(node.weights.get(i));
				}
			}

			dout.writeUTF("END BOOFCV_RECOGNITION_NISTER_2006");
			dout.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Loads {@link RecognitionVocabularyTreeNister2006} from a binary format.
	 *
	 * @param in Input stream
	 * @param db (Ouput) Decoded structure
	 */
	public static <TD extends TupleDesc<TD>> void loadBin( InputStream in, RecognitionVocabularyTreeNister2006<TD> db ) {
		var builder = new StringBuilder();

		try {
			String line = UtilIO.readLine(in, builder);
			if (!line.equals("BOOFCV_RECOGNITION_NISTER_2006"))
				throw new IOException("Unexpected first line. line.length=" + line.length());

			BigDogArray_I32 imagesDB = db.getImagesDB();
			while (true) {
				line = UtilIO.readLine(in, builder);
				if (line.startsWith("BEGIN_TREE"))
					break;
				if (line.startsWith("#"))
					continue;
				String[] words = line.split("\\s");
				if (words[0].equals("images_db.size")) {
					imagesDB.resize(Integer.parseInt(words[1]));
				}
			}

			db.tree = loadTreeBin(in, null);

			var input = new DataInputStream(in);
			readCheckUTF(input, "BEGIN_IMAGE_DB");
			for (int i = 0; i < imagesDB.size; i++) {
				imagesDB.set(i, input.readInt());
			}

			readCheckUTF(input, "BEGIN_INVERTED_FILES");
			db.invertedFiles.reset();
			db.invertedFiles.resize(db.tree.nodes.size());
			for (int nodeIdx = 0; nodeIdx < db.invertedFiles.size(); nodeIdx++) {
				final InvertedFile node = db.invertedFiles.get(nodeIdx);
				final int N = input.readInt();
				node.resize(N);
				node.weights.resize(N);
				for (int i = 0; i < N; i++) {
					node.set(i, input.readInt());
				}
				for (int i = 0; i < N; i++) {
					node.weights.set(i, input.readFloat());
				}
			}

			readCheckUTF(input, "END BOOFCV_RECOGNITION_NISTER_2006");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void readCheckUTF( DataInputStream input, String expected ) throws IOException {
		String line = input.readUTF();
		if (!line.equals(expected))
			throw new IOException("Expected '" + expected + "' not '" + line + "'");
	}
}
