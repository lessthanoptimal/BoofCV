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

package boofcv.alg.feature.disparity.block.score;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

public class GenerateDisparitySparseScoreSadRect extends CodeGeneratorBase {

	String typeInput;
	String dataAbr;
	String bitWise;
	String sumType;

	@Override
	public void generateCode() throws FileNotFoundException {
		createFile(AutoTypeImage.U8);
		createFile(AutoTypeImage.S16);
		createFile(AutoTypeImage.F32);
	}

	public void createFile( AutoTypeImage image ) throws FileNotFoundException {
		setOutputFile("ImplDisparitySparseScoreSadRect_"+image.getAbbreviatedType());
		typeInput = image.getSingleBandName();
		bitWise = image.getBitWise();
		sumType = image.getSumType();

		dataAbr = image.isInteger() ? "S32" : "F32";

		printPreamble();
		printProcess();

		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect;\n" +
				"import boofcv.struct.image."+typeInput+";\n" +
				"\n" +
				"import java.util.Arrays;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Implementation of {@link DisparitySparseScoreSadRect} that processes images of type {@link "+typeInput+"}.\n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * DO NOT MODIFY. Generated by {@link "+getClass().getSimpleName()+"}.\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" extends DisparitySparseScoreSadRect<"+sumType+"[],"+typeInput+"> {\n" +
				"\n" +
				"\t// scores up to the maximum baseline\n" +
				"\t"+sumType+" scores[];\n" +
				"\n" +
				"\tpublic "+className+"( int minDisparity , int maxDisparity, int radiusX, int radiusY) {\n" +
				"\t\tsuper(minDisparity,maxDisparity,radiusX, radiusY);\n" +
				"\n" +
				"\t\tscores = new "+sumType+"[ maxDisparity ];\n" +
				"\t}\n\n");
	}

	private void printProcess() {
		out.print("\t@Override\n" +
				"\tpublic boolean process( int x , int y ) {\n" +
				"\t\t// adjust disparity for image border\n" +
				"\t\tlocalMaxDisparity = Math.min(rangeDisparity,x-radiusX+1-minDisparity);\n" +
				"\n" +
				"\t\tif( localMaxDisparity <= 0 || x >= left.width-radiusX || y < radiusY || y >= left.height-radiusY )\n" +
				"\t\t\treturn false;\n" +
				"\n" +
				"\t\tArrays.fill(scores,0);\n" +
				"\n" +
				"\t\t// sum up horizontal errors in the region\n" +
				"\t\tfor( int row = 0; row < regionHeight; row++ ) {\n" +
				"\t\t\t// pixel indexes\n" +
				"\t\t\tint startLeft = left.startIndex + left.stride*(y-radiusY+row) + x-radiusX;\n" +
				"\t\t\tint startRight = right.startIndex + right.stride*(y-radiusY+row) + x-radiusX-minDisparity;\n" +
				"\n" +
				"\t\t\tfor( int i = 0; i < localMaxDisparity; i++ ) {\n" +
				"\t\t\t\tint indexLeft = startLeft;\n" +
				"\t\t\t\tint indexRight = startRight-i;\n" +
				"\n" +
				"\t\t\t\t"+sumType+" score = 0;\n" +
				"\t\t\t\tfor( int j = 0; j < regionWidth; j++ ) {\n" +
				"\t\t\t\t\t"+sumType+" diff = (left.data[ indexLeft++ ]"+bitWise+") - (right.data[ indexRight++ ]"+bitWise+");\n" +
				"\n" +
				"\t\t\t\t\tscore += Math.abs(diff);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tscores[i] += score;\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn true;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic "+sumType+"[] getScore() {\n" +
				"\t\treturn scores;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic Class<"+typeInput+"> getImageType() {\n" +
				"\t\treturn "+typeInput+".class;\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateDisparitySparseScoreSadRect gen = new GenerateDisparitySparseScoreSadRect();

		gen.generateCode();
	}
}
