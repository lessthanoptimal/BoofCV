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

package boofcv.alg.fiducial.microqr;

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeCodecBitsUtils;
import boofcv.alg.fiducial.qrcode.ReedSolomonCodes_U8;
import boofcv.misc.BoofMiscOps;
import lombok.Getter;
import org.ddogleg.struct.DogArray_I8;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Set;

import static boofcv.alg.fiducial.qrcode.QrCodeCodecBitsUtils.flipBits8;

/**
 * After the data bits have been read this will decode them and extract a meaningful message.
 *
 * @author Peter Abeles
 */
public class MicroQrCodeDecoderBits implements VerbosePrint {
	// used to compute error correction
	ReedSolomonCodes_U8 rscodes = new ReedSolomonCodes_U8(8, 0b100011101, 0);
	// storage for the data message
	DogArray_I8 message = new DogArray_I8();
	// storage fot the message's ecc
	DogArray_I8 ecc = new DogArray_I8();

	@Getter QrCodeCodecBitsUtils utils;

	//------------------ Workspace
	PackedBits8 decodeBits = new PackedBits8();

	@Nullable PrintStream verbose = null;

	/**
	 * @param forceEncoding If null then the default byte encoding is used. If not null then the specified
	 * encoding is used.
	 */
	public MicroQrCodeDecoderBits( @Nullable String forceEncoding, String defaultEncoding ) {
		this.utils = new QrCodeCodecBitsUtils(forceEncoding, defaultEncoding);
	}

	/**
	 * Reconstruct the data while applying error correction.
	 */
	public boolean applyErrorCorrection( MicroQrCode qr ) {
//		System.out.println("decoder ver   "+qr.version);
//		System.out.println("decoder mask  "+qr.mask);
//		System.out.println("decoder error "+qr.error);

		MicroQrCode.VersionInfo info = MicroQrCode.VERSION_INFO[qr.version];
		MicroQrCode.DataInfo block = Objects.requireNonNull(info.levels.get(qr.error));

		int wordsEcc = info.codewords - block.dataCodewords;

		qr.corrected = new byte[block.dataCodewords];

		ecc.resize(wordsEcc);
		rscodes.generator(wordsEcc);
		message.resize(block.dataCodewords);

		// split the raw bits into data and ECC sections
		System.arraycopy(qr.rawbits, 0, message.data, 0, message.size);
		System.arraycopy(qr.rawbits, block.dataCodewords, ecc.data, 0, ecc.size);

		flipBits8(message);
		flipBits8(ecc);

		if (!rscodes.correct(message, ecc)) {
			return false;
		}

		qr.totalBitErrors = rscodes.getTotalErrors();

		flipBits8(message);
		System.arraycopy(message.data, 0, qr.corrected, 0, message.size);
		return true;
	}

	public boolean decodeMessage( MicroQrCode qr ) {
		if (verbose != null)
			verbose.println("decode: version=" + qr.version + " error=" + qr.error + " corrected.length=" + qr.corrected.length);
		decodeBits.data = qr.corrected;
		decodeBits.size = qr.getMaxDataBits();

		utils.workString.setLength(0);

		// Used to indicate when there's no more messages
		int terminatorBits = qr.terminatorBits();

		// Which encoding it used in BYTE mode
		String byteEncoding = "";
		// if there isn't enough bits left to read the mode it must be done
		int location = 0;
		while (location <= decodeBits.size) {
			// see if the terminator is all zeros and encoded data is at an end
			int adjustedTerminatorBits = Math.min(decodeBits.size - location, terminatorBits);
			int terminator = decodeBits.read(location, adjustedTerminatorBits, false);
			if (terminator == 0) {
				location += terminatorBits;
				break;
			}

			int modeLength = MicroQrCode.modeIndicatorBitCount(qr.version);
			if (location + modeLength > decodeBits.size) {
				if (verbose != null) verbose.println("reading mode overflowed");
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return false;
			}
			QrCode.Mode mode;
			if (modeLength >= 0) {
				int modeBits = decodeBits.read(location, modeLength, true);
				location += modeLength;
				mode = MicroQrCode.valueToMode(modeBits);

				// See if something went really wrong
				if (mode == QrCode.Mode.UNKNOWN) {
					if (verbose != null) verbose.println("mode=UNKNOWN Bad encoding?");
					return false;
				}
			} else {
				mode = QrCode.Mode.NUMERIC;
			}
			qr.mode = updateModeLogic(qr.mode, mode);
			if (verbose != null) verbose.println("_ mode=" + mode);

			try {
				switch (mode) {
					case NUMERIC -> location = decodeNumeric(qr, decodeBits, location);
					case ALPHANUMERIC -> location = decodeAlphanumeric(qr, decodeBits, location);
					case BYTE -> {
						location = decodeByte(qr, decodeBits, location);
						if (byteEncoding.isEmpty())
							byteEncoding = utils.selectedByteEncoding;
					}
					case KANJI -> location = decodeKanji(qr, decodeBits, location);
					default -> {
						if (verbose != null) verbose.println("Bad mode. mode=" + mode);
						qr.failureCause = QrCode.Failure.UNKNOWN_MODE;
						return false;
					}
				}
			} catch (RuntimeException e) {
				if (verbose != null)
					verbose.println("Exception decoding: type=" + e.getClass().getSimpleName() +
							" message='" + e.getMessage() + "'");
				qr.failureCause = QrCode.Failure.DECODING_MESSAGE;
			}

			if (verbose != null)
				verbose.println("_ work.length=" + utils.workString.length() + " location=" + location);

			if (location < 0) {
				if (verbose != null) verbose.println("_ Failed: cause=" + utils.failureCause);
				qr.failureCause = utils.failureCause;
				return false;
			}
		}

		if (location < decodeBits.size && !checkPadding(location)) {
			if (verbose != null) verbose.println("_ bad padding");
			qr.failureCause = QrCode.Failure.READING_PADDING;
			return false;
		}

		// NOTE: We could check padding for correctness here as an additional sanity check
		//       I don't think this has ever caught an error with regular QR codes

		qr.byteEncoding = byteEncoding;
		qr.message = utils.workString.toString();
		return true;
	}

	/**
	 * Make sure the expected values have been filled in
	 */
	private boolean checkPadding( int location ) {
		// We should handle this case but we don't
		if (decodeBits.size - location < 8)
			return true;

		// Read until it's 8-bit aligned
		int fillerBits = (8 - (location%8))%8;
		if (fillerBits > 0) {
			int filler = decodeBits.read(location, fillerBits, false);
			if (filler != 0)
				return false;
		}
		location += fillerBits;

		// read the remaining full 8-bit padding. We should also handle the special 4-bit padding in some messages...
		boolean even = true;
		for (int bit = location + 8; bit <= decodeBits.size; bit += 8) {
			int padding = decodeBits.read(bit - 8, 8, false);
			if (even) {
				if (padding != 0b00110111)
					return false;
			} else {
				if (padding != 0b10001000)
					return false;
			}
			even = !even;
		}
		return true;
	}

	/**
	 * If only one mode then that mode is used. If more than one mode is used then set to multiple
	 */
	private QrCode.Mode updateModeLogic( QrCode.Mode current, QrCode.Mode candidate ) {
		if (current == candidate)
			return current;
		else if (current == QrCode.Mode.UNKNOWN) {
			return candidate;
		} else {
			return QrCode.Mode.MIXED;
		}
	}

	/**
	 * Decodes a numeric message
	 *
	 * @param qr QR code
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	private int decodeNumeric( MicroQrCode qr, PackedBits8 data, int bitLocation ) {
		int lengthBits = MicroQrCodeEncoder.getLengthBitsNumeric(qr.version);
		return utils.decodeNumeric(data, bitLocation, lengthBits);
	}

	/**
	 * Decodes alphanumeric messages
	 *
	 * @param qr QR code
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	private int decodeAlphanumeric( MicroQrCode qr, PackedBits8 data, int bitLocation ) {
		int lengthBits = MicroQrCodeEncoder.getLengthBitsAlphanumeric(qr.version);
		return utils.decodeAlphanumeric(data, bitLocation, lengthBits);
	}

	/**
	 * Decodes byte messages
	 *
	 * @param qr QR code
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	private int decodeByte( MicroQrCode qr, PackedBits8 data, int bitLocation ) {
		int lengthBits = MicroQrCodeEncoder.getLengthBitsBytes(qr.version);
		return utils.decodeByte(data, bitLocation, lengthBits);
	}

	/**
	 * Decodes Kanji messages
	 *
	 * @param qr QR code
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	private int decodeKanji( MicroQrCode qr, PackedBits8 data, int bitLocation ) {
		int lengthBits = MicroQrCodeEncoder.getLengthBitsKanji(qr.version);
		return utils.decodeKanji(data, bitLocation, lengthBits);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, utils);
	}
}
