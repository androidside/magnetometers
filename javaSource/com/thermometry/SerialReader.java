package com.thermometry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.*;

import com.thermometry.SerialInputStream;
import com.thermometry.MagnetometersReaderMain;

public class SerialReader implements Runnable {

	SerialInputStream in;

	public SerialReader(SerialInputStream serialPortInputStream) {
		this.in = serialPortInputStream;
	}

	public void run() {
		byte[] buffer = new byte[1];
		List<Byte> packetListBytes = new ArrayList<Byte>();
		int len = -1;
		int indexOfPacket = 0;
		int c = 0;
		boolean done = false, starDetected = false;

		int count = 0;
		int startReading = 0;
		int flag[] = { 0, 0, 0, 0, 0 };
		int raw_data[] = new int[50];
		int result[] = new int[50];
		int checksum_index = 0;
		int nb_bytes = 19;
		int verif_checksum = 0;
		int j = 0;

		int ret = c; // Get the character
		// counter++; //Allow to avoid taking into account the leftover values
		// in the serial port
		System.out.println("Starting ...");

		while (done == false) {
			try {
				// If avaliable == 0 the method does block
				c = this.in.read(buffer, 0, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("C = " + c + " and readBuf = " + buffer[0]);
			if (c > 0) {
				// Display readout //

				if (flag[4] == 1 && buffer[0] != 0x0d) {
					raw_data[count] = buffer[0];
					// System.out.println("Raw data[%d]: %d\n", count,raw_data[count]);
					count++;
				}

				// Checksum verification //

				if (count == 19 && buffer[0] != 0x0d) {
					checksum_index = raw_data[18];

					int sum = 0;
					if (checksum_index >= nb_bytes) { // If checksum bigger than
														// number of bytes
						for (int i = 0; i < nb_bytes - 1; i++) {
							sum += raw_data[i];
						}
					} else { // If checksum smaller than number of bytes
						for (int i = 0; i < checksum_index; i++) {
							sum += raw_data[i];
						}
					}

					verif_checksum = (sum + 279) % 256;
					// System.out.println("Verif = %d\n", verif_checksum);
				}

				// If checksum is verified, process the data //

				if (raw_data[18] == verif_checksum && buffer[0] == 0x0d) {
					j++;

					System.out.println("******************************************************\n");
					System.out.println("********	    Process data	    **********\n");
					System.out.println("******************************************************\n");

					int nb = 0;

					for (int n = 0; n < 18; n = n + 2) {

						// char info_1[9];
						// char info_2[9];
						// char info_bin[20];
						// convert2Bin(int(raw_data[0+n]), info_1);
						// convert2Bin(raw_data[1+n], info_2);
						// snprintf(info_bin, sizeof(info_bin), "%s%s", info_1,
						// info_2); //concatenate the 2 values
						// int info_dec = convert2Dec(info_bin);

						result[nb] = raw_data[1 + n] << 8 | raw_data[0 + n];
						nb++;
					}

					float roll = (float) (result[0] / 65536.0) * 360;
					float pitch = (float) (result[1] / 65536.0) * 360;
					float azimuth = (float) (result[2] / 65536.0) * 360;
					int X_accel = (short) result[3];
					int Y_accel = (short) result[4];
					;
					int Z_accel = (short) result[5];
					int X_mag = result[6];
					int Y_mag = result[7];
					int Z_mag = result[8];

					System.out.println("Roll (X): " + roll + " degrees\n");
					System.out.println("Pitch (Y): " + pitch + " degrees\n");
					System.out.println("Azimuth: " + azimuth + " degrees\n");
					System.out.println("X accel: " + X_accel + "\n");
					System.out.println("Y accel: " + Y_accel + "\n");
					System.out.println("Z accel: " + Z_accel + "\n");
					System.out.println("X mag: " + X_mag + "\n");
					System.out.println("Y mag: " + Y_mag + "\n");
					System.out.println("Z mag: " + Z_mag + "\n");

					count = 0;
					startReading = 0;

				}

				// System.out.print((char)buffer[0]);

				if (raw_data[18] != verif_checksum && buffer[0] == 0x0d) {
					count = 0;
					startReading = 0;
					System.out.println("\nData corrupted: checksum not verified\n");
				}

				if (startReading == 0) { // Wait for the start sequence
					if (buffer[0] == 0x0d) { // Header Byte1
						flag[0] = 1;
						flag[1] = 0;
						flag[2] = 0;
						flag[3] = 0;
						flag[4] = 0;
					} else if (buffer[0] == 0x0a && flag[0] == 1) { // Header
																	// Byte2
						flag[1] = 1;
					} else if (buffer[0] == 0x7e && flag[1] == 1) { // Header
																	// Byte3
						flag[2] = 1;
					} else if (buffer[0] == 0x70 && flag[2] == 1) { // ID =
																	// DORIENT
						flag[3] = 1;

					} else if (buffer[0] == 0x12 && flag[3] == 1) { // Count =
																	// 18 Bytes
						flag[4] = 1;
						startReading = 1;
					} else {
						if (flag[4] != 1) {
							flag[0] = 0;
							flag[1] = 0;
							flag[2] = 0;
							flag[3] = 0;
						} else {
							// Do nothing
							// We start to store useful informations in data[]
							// during next iteration
							System.out.println("Issue");
						}
					}
				}

				else if (c < 0) {
					done = true;
					try {
						throw new IOException("Stream closed");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			} // finish reading header

		}
		System.out.println("Done");
	}
}