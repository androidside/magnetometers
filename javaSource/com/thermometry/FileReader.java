package com.thermometry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.lang.*;

import calibration.Calibrator;


public class FileReader implements Runnable
{

	InputStream in;
	private final boolean fDebug = true;
	BoardTypeHandler  handler = new BoardTypeHandler();;
	public Calibrator calibrators[] = new Calibrator[16]; //TRead has 16 channels


	public FileReader(InputStream inputStream, Calibrator calibrators[])
	{
		this.in = inputStream;
		this.calibrators = calibrators;

	}

	public void run()
	{
		byte[] buffer = new byte[1];
		List<Byte> packetListBytes = new ArrayList<Byte>();
		int len = -1;
		int indexOfPacket = 0;
		int c = 0;     
		boolean done = false, starDetected = false;

		System.out.println("Starting File Reader ...");
		while (done == false)
		{
			try
			{
				//If avaliable == 0 the method does block

				c = this.in.read(buffer,0,1);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//                System.out.println("C = "+c+" and readBuf = "+readBuf[0]) ;
			if (c > 0)
			{

				if(MagnetometersReaderMain.fDumpData == false){

					if(buffer[0] == '*'){
						starDetected = true;
						System.out.println("Star detected !");
					}

					if(starDetected == true){
						packetListBytes.add(buffer[0]);
					}

					if((buffer[0] == '\n') && (starDetected == true)){
						starDetected = false;

						System.out.println("First packet read, sending it to BoardTypeHandler...");

						/*
						 * Transform List to Array
						 * */ 
						Byte[] packetArrayBytes = new Byte[packetListBytes.size()];
						packetListBytes.toArray(packetArrayBytes);
						packetListBytes.clear();

						handler.detectHeader(packetArrayBytes,calibrators);

						/**
						 * If we wanna use threads, wich we don't**/
						//					Thread t =(new Thread(new BoardTypeHandler(packetArrayBytes)));
						//					t.start();
						//
						//					//For debugging purposes, let's wait until the thread is done so the different console messages don't overlap
						//
						//					if(fDebug == true){
						//						try {
						//							t.join();
						//						} catch (InterruptedException e) {
						//							// TODO Auto-generated catch block
						//							e.printStackTrace();
						//						}
						//					}


						/*
						 * Send packet/
						 */
					}
				}

				System.out.print((char)buffer[0]);
				System.out.flush();



			}
			else if (c < 0)

			{   
				done = true;

			}


		}
	}
}