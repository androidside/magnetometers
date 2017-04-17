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

public class SerialReader2 implements Runnable {

	SerialInputStream in;

	public SerialReader2(SerialInputStream serialPortInputStream) {
		this.in = serialPortInputStream;
	}

	public void run() {
		byte[] buffer = new byte[1];
		List<Byte> packetListBytes = new ArrayList<Byte>();
		int len = -1;
		int indexOfPacket = 0;
		int c = 0;
		boolean done = false, starDetected = false, process = false;

		double azimuth = 0.0;
		double pitch = 0.0;
		double roll = 0.0;

		int count = 0;
		int startReading = 0;
		int flag[] = { 0, 0 };
		char raw_data[] = new char[50];
		int result[] = new int[50];
		int checksum_index = 0;
		int nb_bytes = 19;
		int verif_checksum = 0;
		int j = 0;
		int prev = 0;
		int ret = 0; // Get the character
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
			// System.out.println("C = " + c + " and readBuf = " + buffer[0]);
			if (c > 0) {
				if (buffer[0] == 10 && prev == 13) { // Header bytes LF+CR
					startReading = 1; // Start reading data
					count = 0;
				}
				prev = buffer[0];
				try {
					if (startReading == 1) { // Wait for the start sequence
						if (buffer[0] == 13 && count > 1) { // Until next header
							startReading = 0;
							roll = Double.parseDouble((new String(raw_data)).substring(0, count));
							// sscanf(raw_data, "%lf", &roll);
							// memset(raw_data, 0, sizeof(raw_data));
							count = 0;
							flag[0] = 0; // Reset flags
							flag[1] = 0; // Reset flags
							// flag[2] = 0; // Reset flags

							System.out.printf("\n********  Data   ************\n");
							System.out.printf("Roll: %.2f\n", roll);
							System.out.printf("Pitch: %.2f\n", pitch);
							System.out.printf("Azimuth: %.2f\n", azimuth);
							System.out.printf("*****************************\n");

							j++;
							process = true;

						} else { // Store data in array
							if (buffer[0] == 44 && count > 1) { // If comma
								if (flag[0] == 1 && flag[1] == 0) {
									pitch = Double.parseDouble((new String(raw_data)).substring(0, count));
									// sscanf(raw_data, "%lf", &pitch);
									// memset(raw_data, 0, sizeof(raw_data));
									count = 0;
									flag[1] = 1;
								}

								if (flag[0] == 0) {
									azimuth = Double.parseDouble((new String(raw_data)).substring(0, count));
									// sscanf(raw_data, "%lf", &azimuth);
									// memset(raw_data, 0, sizeof(raw_data));
									count = 0;
									flag[0] = 1;
								}

							} else if (buffer[0] != 10) {
								raw_data[count] = (char) buffer[0];
								count++;
							}
						}

						if (process == true) {
							count = 0;
							startReading = 0;
							process = false;
						} // End of process loop

					}
				} catch (NumberFormatException nfe) {
					System.out.println("Bad number parsing.");
					count = 0;
					startReading = 0;
					process = false;
				}
			} else if (c < 0) {
				done = true;
				try {
					throw new IOException("Stream closed");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} // finish reading header

		}
		System.out.println("Done");

	}

}