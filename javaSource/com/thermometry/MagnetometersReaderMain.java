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
import calibration.Calibrator;
import com.google.common.base.Preconditions;

import jssc.SerialPort;

public class MagnetometersReaderMain
{
	private static final int BAUD_RATE = 19200;
	private static final Logger sLogger = Logger.getLogger(MagnetometersReaderMain.class.getName());
	//Need to set port
	private final String fPort = "COM7"; 
	private final boolean fReadingFile = false;
	public final static boolean fDumpData = false;
	public static List<Float> fResistance = new ArrayList<Float>();
	public static List<Float> fTemperature = new ArrayList<Float>();
	public Calibrator calibrators[] = new Calibrator[16]; //TRead has 16 channels

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

			readFile();
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
		(new Thread(new SerialWriter(serialPortOutputStream))).start();

	}


	private void readFile() throws FileNotFoundException
	{
		OutputStream out = null;
		System.out.println("Opening File");
		//URL pathFile = getClass().getResource("/TextFiles/Tread1.txt");
		URL pathFile = getClass().getResource("/TextFiles/AllData.txt");
		InputStream is = null;

		try {
			is = pathFile.openStream();
		} catch (IOException  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OutputStream os = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				System.out.println("Serial Port not Configured");
			}
		};

		(new Thread(new FileReader(is, calibrators))).start();
		(new Thread(new SerialWriter(os))).start();

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
