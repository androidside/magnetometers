package com.thermometry;


import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.base.Preconditions;

import jssc.SerialPort;

public class MagnetometersReaderMain
{
	private static final int BAUD_RATE = 19200;
	private static final Logger sLogger = Logger.getLogger(MagnetometersReaderMain.class.getName());
	//Need to set port
	private final String fPort = "COM3"; 
	private final boolean fReadingFile = false;

	public MagnetometersReaderMain(){

	}

	protected void openCommunication() throws IOException
	{       

		if(fReadingFile == false){
			try
			{
				initialize(fPort);
			}
			catch (Exception e)
			{
				throw new IOException("Error opening port: " + fPort + ": " + e);
			}

		}

		else {

			//readFile();
		}

	}


	private void initialize(String portName) throws Exception
	{
		Preconditions.checkNotNull(portName);

		OutputStream out = null;
		SerialPort serialPort = new SerialPort (portName);

		if (serialPort.isOpened())
		{
			System.out.println("Error: Magnetometers Reader Serial Port is currently in use");
		}

		else{
			serialPort.openPort();
		}
		serialPort.setParams(BAUD_RATE, SerialPort.DATABITS_8,SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, false, false);

		System.out.println("Serial Port Configured. Opening inputStream and outputStream");

		SerialInputStream serialPortInputStream= new SerialInputStream(serialPort);
		SerialOutputStream serialPortOutputStream= new SerialOutputStream(serialPort);

		(new Thread(new SerialReader(serialPortInputStream))).start();
		//We don't need to write to the magnetometer
		//(new Thread(new SerialWriter(serialPortOutputStream))).start();

	}



	public static void main( String[] args ) {
		try {
			int publicInt = 0;
			MagnetometersReaderMain mag = new MagnetometersReaderMain();
			mag.openCommunication();
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}


}
