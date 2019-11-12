/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

/**
 * @author Peter Abeles
 */
public class GenerateDisparityScoreBM_SAD extends CodeGeneratorBase {

	String typeInput;
	String dataAbr;
	String bitWise;
	String sumType;

	public GenerateDisparityScoreBM_SAD() {
		super(false);
	}

	@Override
	public void generate() throws FileNotFoundException {
		createFile(AutoTypeImage.U8);
		createFile(AutoTypeImage.U16);
		createFile(AutoTypeImage.S16);
		createFile(AutoTypeImage.F32);
	}

	public void createFile( AutoTypeImage image ) throws FileNotFoundException {
		this.className = null;
		setOutputFile("ImplDisparityScoreBM_SAD_"+image.getAbbreviatedType());
		typeInput = image.getSingleBandName();
		bitWise = image.getBitWise();
		sumType = image.getSumType();

		dataAbr = image.isInteger() ? "S32" : "F32";

		printPreamble();
		printWorkspace();
		printComputeBlock();
		printComputeFirstRow();
		printComputeRemainingRows();
		printTheRest();

		out.println("}");
	}

	private void printPreamble() {
		out.print(
				"import boofcv.alg.InputSanityCheck;\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.alg.feature.disparity.DisparityBlockMatch;\n" +
				"import boofcv.alg.feature.disparity.DisparitySelect;\n" +
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.concurrency.IntRangeObjectConsumer;\n" +
				"import boofcv.struct.image."+typeInput+";\n" +
				"import boofcv.struct.image.ImageGray;\n" +
				"import org.ddogleg.struct.FastQueue;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Implementation of {@link boofcv.alg.feature.disparity.DisparityScoreSadRect} for processing\n" +
				" * input images of type {@link "+typeInput+"}.\n" +
				" * </p>\n" +
				generateDocString() +
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				generatedAnnotation() +
				"public class "+className+"<DI extends ImageGray<DI>>\n" +
				"\textends DisparityBlockMatch<"+typeInput+",DI>\n" +
				"{\n" +
				"\t// Computes disparity from scores. Concurrent code copies this\n" +
				"\tDisparitySelect<"+sumType+"[], DI> disparitySelect0;\n" +
				"\n" +
				"\t// reference to input images;\n" +
				"\t"+typeInput+" left, right;\n" +
				"\tDI disparity;\n" +
				"\n" +
				"\tFastQueue workspace = new FastQueue<>(WorkSpace.class, WorkSpace::new);\n" +
				"\tComputeBlock computeBlock = new ComputeBlock();\n" +
				"\n" +
				"\tpublic "+className+"( int minDisparity , int maxDisparity,\n" +
				"\t\t\t\t\t\t\t\t\t\tint regionRadiusX, int regionRadiusY,\n" +
				"\t\t\t\t\t\t\t\t\t\tDisparitySelect<"+sumType+"[], DI> computeDisparity) {\n" +
				"\t\tsuper(minDisparity,maxDisparity,regionRadiusX,regionRadiusY);\n" +
				"\n" +
				"\t\tthis.disparitySelect0 = computeDisparity;\n" +
				"\t\tworkspace.grow();\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic void _process("+typeInput+" left , "+typeInput+" right , DI disparity ) {\n" +
				"\t\tInputSanityCheck.checkSameShape(left,right);\n" +
				"\t\tdisparity.reshape(left.width,left.height);\n" +
				"\t\tthis.left = left;\n" +
				"\t\tthis.right = right;\n" +
				"\t\tthis.disparity = disparity;\n" +
				"\n" +
				"\t\tif( BoofConcurrency.USE_CONCURRENT ) {\n" +
				"\t\t\tBoofConcurrency.loopBlocks(0,left.height,regionHeight,workspace,computeBlock);\n" +
				"\t\t} else {\n" +
				"\t\t\tcomputeBlock.accept((WorkSpace)workspace.get(0),0,left.height);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printWorkspace() {
		out.print("\tclass WorkSpace {\n" +
				"\t\t// stores the local scores for the width of the region\n" +
				"\t\t"+sumType+"[] elementScore;\n" +
				"\t\t// scores along horizontal axis for current block\n" +
				"\t\t// To allow right to left validation all disparity scores are stored for the entire row\n" +
				"\t\t// size = num columns * maxDisparity\n" +
				"\t\t// disparity for column i is stored in elements i*maxDisparity to (i+1)*maxDisparity\n" +
				"\t\t"+sumType+"[][] horizontalScore = new "+sumType+"[0][0];\n" +
				"\t\t// summed scores along vertical axis\n" +
				"\t\t// This is simply the sum of like elements in horizontal score\n" +
				"\t\t"+sumType+"[] verticalScore = new "+sumType+"[0];\n" +
				"\n" +
				"\t\tDisparitySelect<"+sumType+"[], DI> computeDisparity;\n" +
				"\n" +
				"\t\tpublic void checkSize() {\n" +
				"\t\t\tif( horizontalScore.length != regionHeight || horizontalScore[0].length != lengthHorizontal ) {\n" +
				"\t\t\t\thorizontalScore = new "+sumType+"[regionHeight][lengthHorizontal];\n" +
				"\t\t\t\tverticalScore = new "+sumType+"[lengthHorizontal];\n" +
				"\t\t\t\telementScore = new "+sumType+"[ left.width ];\n" +
				"\t\t\t}\n" +
				"\t\t\tif( computeDisparity == null ) {\n" +
				"\t\t\t\tcomputeDisparity = disparitySelect0.concurrentCopy();\n" +
				"\t\t\t}\n" +
				"\t\t\tcomputeDisparity.configure(disparity,minDisparity,maxDisparity,radiusX);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printComputeBlock() {
		out.print("\tprivate class ComputeBlock implements IntRangeObjectConsumer<WorkSpace> {\n" +
				"\t\t@Override\n" +
				"\t\tpublic void accept(WorkSpace workspace, int minInclusive, int maxExclusive) {\n" +
				"\n" +
				"\t\t\tworkspace.checkSize();\n" +
				"\n" +
				"\t\t\t// The image border will be skipped, so it needs to back track some\n" +
				"\t\t\tint row0 = Math.max(0,minInclusive-radiusY);\n" +
				"\t\t\tint row1 = Math.min(left.height,maxExclusive+radiusY);\n" +
				"\n" +
				"\t\t\t// initialize computation\n" +
				"\t\t\tcomputeFirstRow(row0, workspace.computeDisparity,\n" +
				"\t\t\t\t\tworkspace.elementScore, workspace.horizontalScore, workspace.verticalScore);\n" +
				"\n" +
				"\t\t\t// efficiently compute rest of the rows using previous results to avoid repeat computations\n" +
				"\t\t\tcomputeRemainingRows(row0,row1, workspace.computeDisparity,\n" +
				"\t\t\t\t\tworkspace.elementScore, workspace.horizontalScore, workspace.verticalScore);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printComputeFirstRow() {
		out.print("\t/**\n" +
				"\t * Initializes disparity calculation by finding the scores for the initial block of horizontal\n" +
				"\t * rows.\n" +
				"\t */\n" +
				"\tprivate void computeFirstRow(int row0 , DisparitySelect<"+sumType+"[], DI> computeDisparity,\n" +
				"\t\t\t\t\t\t\t\t final "+sumType+"[] elementScore, final "+sumType+"[][] horizontalScore, final "+sumType+"[] verticalScore) {\n" +
				"\t\tfinal "+typeInput+" left = this.left, right = this.right;\n" +
				"\t\t// compute horizontal scores for first row block\n" +
				"\t\tfor( int row = 0; row < regionHeight; row++ ) {\n" +
				"\t\t\tfinal "+sumType+"[] scores = horizontalScore[row];\n" +
				"\t\t\tUtilDisparityScore.computeSadDispRow(left, right,row0+row, scores,\n" +
				"\t\t\t\t\tminDisparity,maxDisparity,regionWidth,elementScore);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// compute score for the top possible row\n" +
				"\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t"+sumType+" sum = 0;\n" +
				"\t\t\tfor( int row = 0; row < regionHeight; row++ ) {\n" +
				"\t\t\t\tsum += horizontalScore[row][i];\n" +
				"\t\t\t}\n" +
				"\t\t\tverticalScore[i] = sum;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// compute disparity\n" +
				"\t\tcomputeDisparity.process(row0+radiusY, verticalScore);\n" +
				"\t}\n\n");
	}

	private void printComputeRemainingRows() {
		out.print("\t/**\n" +
				"\t * Using previously computed results it efficiently finds the disparity in the remaining rows.\n" +
				"\t * When a new block is processes the last row/column is subtracted and the new row/column is\n" +
				"\t * added.\n" +
				"\t */\n" +
				"\tprivate void computeRemainingRows(int row0 , int row1,\n" +
				"\t\t\t\t\t\t\t\t\t  DisparitySelect<"+sumType+"[], DI> computeDisparity,\n" +
				"\t\t\t\t\t\t\t\t\t  final "+sumType+"[] elementScore, final "+sumType+"[][] horizontalScore, final "+sumType+"[] verticalScore )\n" +
				"\t{\n" +
				"\t\tfinal "+typeInput+" left = this.left, right = this.right;\n" +
				"\t\tfor( int row = row0+regionHeight; row < row1; row++ ) {\n" +
				"\t\t\tint oldRow = (row-row0)%regionHeight;\n" +
				"\n" +
				"\t\t\t// subtract first row from vertical score\n" +
				"\t\t\tfinal "+sumType+"[] scores = horizontalScore[oldRow];\n" +
				"\t\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t\tverticalScore[i] -= scores[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tUtilDisparityScore.computeSadDispRow(left, right, row, scores,\n" +
				"\t\t\t\t\tminDisparity,maxDisparity,regionWidth,elementScore);\n" +
				"\n" +
				"\t\t\t// add the new score\n" +
				"\t\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t\tverticalScore[i] += scores[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// compute disparity\n" +
				"\t\t\tcomputeDisparity.process(row - regionHeight + 1 + radiusY, verticalScore);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printTheRest() {
		out.print("\t@Override\n" +
				"\tpublic ImageType<"+typeInput+"> getInputType() {\n" +
				"\t\treturn ImageType.single("+typeInput+".class);\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic Class<DI> getDisparityType() {\n" +
				"\t\treturn disparitySelect0.getDisparityType();\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateDisparityScoreBM_SAD gen = new GenerateDisparityScoreBM_SAD();

		gen.generate();
	}
}
