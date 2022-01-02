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
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.ReidSolomonCodes;
import org.ddogleg.struct.DogArray_I8;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

import static boofcv.alg.fiducial.qrcode.EciEncoding.guessEncoding;
import static boofcv.alg.fiducial.qrcode.QrCode.Failure.JIS_UNAVAILABLE;
import static boofcv.alg.fiducial.qrcode.QrCode.Failure.KANJI_UNAVAILABLE;
import static boofcv.alg.fiducial.qrcode.QrCodeEncoder.valueToAlphanumeric;

/**
 * After the data bits have been read this will decode them and extract a meaningful message.
 *
 * @author Peter Abeles
 */
public class MicroQrCodeDecoderBits {
	// used to compute error correction
	ReidSolomonCodes rscodes = new ReidSolomonCodes(8, 0b100011101);
	// storage for the data message
	DogArray_I8 message = new DogArray_I8();
	// storage fot the message's ecc
	DogArray_I8 ecc = new DogArray_I8();

	StringBuilder workString = new StringBuilder();

	// If null the encoding of byte messages will attempt to be automatically determined, with a default
	// of UTF-8. Otherwise, this is the encoding used.
	@Nullable String forceEncoding;

	//------------------ Workspace
	PackedBits8 decodeBits = new PackedBits8();

	/**
	 * @param forceEncoding If null then the default byte encoding is used. If not null then the specified
	 * encoding is used.
	 */
	public MicroQrCodeDecoderBits( @Nullable String forceEncoding ) {
		this.forceEncoding = forceEncoding;
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

		qr.corrected = new byte[info.codewords];

		ecc.resize(wordsEcc);
		rscodes.generator(wordsEcc);
		message.resize(block.dataCodewords);

		// split the raw bits into data and ECC sections
		System.arraycopy(qr.rawbits, 0, message.data, 0, message.size);
		System.arraycopy(qr.rawbits, block.dataCodewords, ecc.data, 0, ecc.size);

		QrCodeEncoder.flipBits8(message);
		QrCodeEncoder.flipBits8(ecc);

		if (!rscodes.correct(message, ecc)) {
			return false;
		}

		QrCodeEncoder.flipBits8(message);
		System.arraycopy(message.data, 0, qr.corrected, 0, message.size);
		return true;
	}

	public boolean decodeMessage( MicroQrCode qr ) {
		decodeBits.data = qr.corrected;
		decodeBits.size = qr.corrected.length*8;

		workString.setLength(0);

		// Used to indicate when there's no more messages
		int terminatorBits = qr.terminatorBits();

		// if there isn't enough bits left to read the mode it must be done
		int location = 0;
		while (location + terminatorBits < decodeBits.size) {
			// see if the terminator is all zeros and encoded data is at an end
			int terminator = decodeBits.read(location, terminatorBits, true);
			if (terminator == 0)
				break;

			int modeLength = MicroQrCode.modeIndicatorBits(qr.version);
			MicroQrCode.Mode mode;
			if (modeLength >= 0) {
				int modeBits = decodeBits.read(location, modeLength, true);
				location += modeLength;
				mode = MicroQrCode.Mode.lookup(modeBits);

				// See if something went really wrong
				if (mode == MicroQrCode.Mode.UNKNOWN) {
					return false;
				}
			} else {
				mode = MicroQrCode.Mode.NUMERIC;
			}
			qr.mode = updateModeLogic(qr.mode, mode);

			switch (mode) {
				case NUMERIC -> location = decodeNumeric(qr, decodeBits, location);
				case ALPHANUMERIC -> location = decodeAlphanumeric(qr, decodeBits, location);
				case BYTE -> location = decodeByte(qr, decodeBits, location);
				case KANJI -> location = decodeKanji(qr, decodeBits, location);
				default -> {
					qr.failureCause = QrCode.Failure.UNKNOWN_MODE;
					return false;
				}
			}

			if (location < 0) {
				// cause is set inside of decoding function
				return false;
			}
		}

		// NOTE: We could check padding for correctness here as an additional sanity check
		//       I don't think this has ever caught an error with regular QR codes

		qr.message = workString.toString();
		return true;
	}

	/**
	 * If only one mode then that mode is used. If more than one mode is used then set to multiple
	 */
	private MicroQrCode.Mode updateModeLogic( MicroQrCode.Mode current, MicroQrCode.Mode candidate ) {
		if (current == candidate)
			return current;
		else if (current == MicroQrCode.Mode.UNKNOWN) {
			return candidate;
		} else {
			return MicroQrCode.Mode.MIXED;
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

		int length = data.read(bitLocation, lengthBits, true);
		bitLocation += lengthBits;

		while (length >= 3) {
			if (data.size < bitLocation + 10) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation, 10, true);
			bitLocation += 10;

			int valA = chunk/100;
			int valB = (chunk - valA*100)/10;
			int valC = chunk - valA*100 - valB*10;

			workString.append((char)(valA + '0'));
			workString.append((char)(valB + '0'));
			workString.append((char)(valC + '0'));

			length -= 3;
		}

		if (length == 2) {
			if (data.size < bitLocation + 7) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation, 7, true);
			bitLocation += 7;

			int valA = chunk/10;
			int valB = chunk - valA*10;
			workString.append((char)(valA + '0'));
			workString.append((char)(valB + '0'));
		} else if (length == 1) {
			if (data.size < bitLocation + 4) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int valA = data.read(bitLocation, 4, true);
			bitLocation += 4;
			workString.append((char)(valA + '0'));
		}
		return bitLocation;
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

		int length = data.read(bitLocation, lengthBits, true);
		bitLocation += lengthBits;

		while (length >= 2) {
			if (data.size < bitLocation + 11) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation, 11, true);
			bitLocation += 11;

			int valA = chunk/45;
			int valB = chunk - valA*45;

			workString.append(valueToAlphanumeric(valA));
			workString.append(valueToAlphanumeric(valB));
			length -= 2;
		}

		if (length == 1) {
			if (data.size < bitLocation + 6) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int valA = data.read(bitLocation, 6, true);
			bitLocation += 6;
			workString.append(valueToAlphanumeric(valA));
		}
		return bitLocation;
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

		int length = data.read(bitLocation, lengthBits, true);
		bitLocation += lengthBits;

		if (length*8 > data.size - bitLocation) {
			qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
			return -1;
		}

		byte[] rawdata = new byte[length];

		for (int i = 0; i < length; i++) {
			rawdata[i] = (byte)data.read(bitLocation, 8, true);
			bitLocation += 8;
		}

		// If ECI encoding is not specified use the default encoding. Unfortunately the specification is ignored
		// by most people here and UTF-8 is used. If an encoding is specified then that is used.
		String encoding = forceEncoding != null ? forceEncoding : guessEncoding(rawdata);
		try {
			workString.append(new String(rawdata, encoding));
		} catch (UnsupportedEncodingException ignored) {
			qr.failureCause = JIS_UNAVAILABLE;
			return -1;
		}
		return bitLocation;
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

		int length = data.read(bitLocation, lengthBits, true);
		bitLocation += lengthBits;

		byte[] rawdata = new byte[length*2];

		for (int i = 0; i < length; i++) {
			if (data.size < bitLocation + 13) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int letter = data.read(bitLocation, 13, true);
			bitLocation += 13;

			letter = ((letter/0x0C0) << 8) | (letter%0x0C0);

			if (letter < 0x01F00) {
				// In the 0x8140 to 0x9FFC range
				letter += 0x08140;
			} else {
				// In the 0xE040 to 0xEBBF range
				letter += 0x0C140;
			}
			rawdata[i*2] = (byte)(letter >> 8);
			rawdata[i*2 + 1] = (byte)letter;
		}

		// Shift_JIS may not be supported in some environments:
		try {
			workString.append(new String(rawdata, "Shift_JIS"));
		} catch (UnsupportedEncodingException ignored) {
			qr.failureCause = KANJI_UNAVAILABLE;
			return -1;
		}

		return bitLocation;
	}
}
