package com.thermometry.boardTypes;

import gov.nasa.gsfc.protobuf.ThermometryPacketProtobuf.TReadMessage;
import gov.nasa.gsfc.protobuf.ThermometryPacketProtobuf.ThermometryPacket;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thermometry.MagnetometersReaderMain;

import calibration.Calibrator;

public class TRead_LowRes {

	private static final Logger sLogger = Logger.getLogger(TRead_LowRes.class.getName());

	private final short TRead_Standard = 0;
	private final short TRead_HR = 1;
	private final short TRead_LR = 2;
	private final  short TRead_Diode = 3;
	private final double preAmpGain = 100.0;          // AD620 set to gain of 100 for all other modes
	private final double preGainDiode = 1.0;          // AD620 set to gain of 1 for diodes
	private final short numberOfChannels = 16 ;



	int DemodRaw[] = new int [numberOfChannels];
	float Demod[] = new float [numberOfChannels];

	int ADacRaw[] = new int[numberOfChannels];
	float ADac[] = new float [numberOfChannels]; 

	int GDac[] = new int [numberOfChannels]; 
	int Nsum;

	float [] Temperature =  new float [numberOfChannels] ;
	private double[] fVoltages = new double[numberOfChannels];

	char fDemodTemp []= new char[8];
	char fADACTemp []= new char[4];
	char fGDACTemp []= new char[4];
	char fnSumTemp [] = new char [2];

	private int Status;
	private short readMode;
	byte[] fPacketArraybytes;
	
	ThermometryPacket.Builder thermometryPacket = ThermometryPacket.newBuilder();
	TReadMessage.Builder tReadMessage = TReadMessage.newBuilder();


	public TRead_LowRes(){	}

	public void read(byte[] packetArraybytes, Calibrator calibrators[]){
		fPacketArraybytes=packetArraybytes;

		//Address
		char addressChar []= new char[2];
		for(int i=0;i<2;i++){
			addressChar[i] = (char)fPacketArraybytes[1+i];
		}		
		int Address = Integer.parseInt(String.valueOf(addressChar), 16) ; 
		thermometryPacket.setTr(tReadMessage.setAddress(Address));

		//Boardtype
		char BoardType = (char)packetArraybytes[3];
		thermometryPacket.setTr(tReadMessage.setBoardType(String.valueOf(BoardType)));


		char frameCounterChar []= new char[2];
		for(int i=0;i<2;i++){
			frameCounterChar[i] = (char)fPacketArraybytes[4+i];
		}		
		int FrameCounter = Integer.parseInt(String.valueOf(frameCounterChar), 16) ; 
		thermometryPacket.setTr(tReadMessage.setFrameCounter(FrameCounter));


		//Tmux
		char TMux = (char)packetArraybytes[6];
		thermometryPacket.setTr(tReadMessage.setTMux(String.valueOf(TMux)));


		//Status
		String status =Character.toString ((char) fPacketArraybytes[7]);
		Status = Integer.parseUnsignedInt(String.valueOf(status), 10);
		thermometryPacket.setTr(tReadMessage.setStatus(Status));

		if(Status == 2){
			readMode = TRead_LR;

		}
		else{
			sLogger.log(Level.SEVERE, "readMode not matching = " +Status );			
		}

		readChannels();
		CalcReducedData(calibrators);
		thermometryPacket.build();


		//Print Values
		System.out.println(" ----------------- TRead_LowRes VALUES -----------------  ");
		System.out.println("Address = "+thermometryPacket.getTr().getAddress());
		System.out.println("BoardType = "+thermometryPacket.getTr().getBoardType());
		System.out.println("FrameCounter = "+thermometryPacket.getTr().getFrameCounter());
		System.out.println("TMUX = "+thermometryPacket.getTr().getTMux());
		System.out.println("Status = "+thermometryPacket.getTr().getStatus());		
		for (int i = 0; i < Demod.length; i++) {
			System.out.println("Demod["+i+"] = "+thermometryPacket.getTr().getDemod(i)+" Ohms");			
		}
		for (int i = 0; i < ADac.length; i++) {
			System.out.println("ADac["+i+"] = "+thermometryPacket.getTr().getADac(i)+" nanoAmps");			
		}
		for (int i = 0; i < GDac.length; i++) {
			System.out.println("GDac["+i+"] = "+thermometryPacket.getTr().getGDac(i));			
		}
		System.out.println("Nsum = "+thermometryPacket.getTr().getNsum());	
		
		for (int i = 0; i < Temperature.length; i++) {
			System.out.println("Temperature["+i+"] = "+thermometryPacket.getTr().getTemperature(i)+" Kelvin");			
		}
		
		System.out.println(" ----------------- TRead_LowRes RAW VALUES -----------------  ");
		for (int i = 0; i < DemodRaw.length; i++) {
			System.out.println("DemodRaw["+i+"] = "+thermometryPacket.getTr().getDemodRaw(i));			
		}
		for (int i = 0; i < ADacRaw.length; i++) {
			System.out.println("ADacRaw["+i+"] = "+thermometryPacket.getTr().getADacRaw(i));			
		}
	}

	public void CalcReducedData(Calibrator calibrators[])
	{


		if (readMode == TRead_LR)           // DT-470 diode mode
		{
			CalcReducedResistanceModes(calibrators);
		}
		else
		{
			sLogger.log(Level.SEVERE, "readMode unknown = " +readMode );			

		}
	}



	private void readChannels(){

		for (int j = 0; j < numberOfChannels; j++) {
			// First 8 HEX characters are asigned to Demod[j]
			for(int i=0;i<8;i++){
				fDemodTemp[i] = (char)fPacketArraybytes[8+16*j+i];
			}

			DemodRaw[j] = (int)Long.parseLong(String.valueOf(fDemodTemp), 16) ; 
			thermometryPacket.setTr(tReadMessage.addDemodRaw(DemodRaw[j]));			


			//Following 4 HEX characters are asigned to ADACTemp[j]
			for(int i=0;i<4;i++){
				fADACTemp[i] = (char)fPacketArraybytes[16+16*j+i];
			}
			ADacRaw[j] = Integer.parseInt(String.valueOf(fADACTemp), 16) ; 
			ADac[j] = (float) (ADacRaw[j] * 31.7259);
			thermometryPacket.setTr(tReadMessage.addADacRaw(ADacRaw[j]));	
			thermometryPacket.setTr(tReadMessage.addADac(ADac[j]));	


			//Following 4 HEX characters are asigned to GDACTemp[j]
			for(int i=0;i<4;i++){
				fGDACTemp[i] = (char)fPacketArraybytes[20+16*j+i];
			}
			GDac[j] = Integer.parseInt(String.valueOf(fGDACTemp), 16) ; 
			thermometryPacket.setTr(tReadMessage.addGDac(GDac[j]));			


		}

		//Channels bytes start at the 8th byte, each channel is 16 bytes of size
		fnSumTemp[0] = (char)fPacketArraybytes[8+16*numberOfChannels];
		fnSumTemp [1] =  (char)fPacketArraybytes[8+16*numberOfChannels+1];
		Nsum = Integer.parseInt(String.valueOf(fnSumTemp),16);
		thermometryPacket.setTr(tReadMessage.setNsum(Nsum));			


	}


	private void CalcReducedResistanceModes(Calibrator calibrators[]) {
		double Rbridge = 0, R52_DivideFactor=0;

		if (readMode == TRead_LR)          // High current mode (low resistance)
		{
			Rbridge = 3940.0;      // Total bridge resistance in series with thermometer
			R52_DivideFactor = 1.0;
		}
		else{
			try {
				sLogger.log(Level.SEVERE, "readMode unknown = " +readMode );			
				throw new Exception("Unknown TRead mode");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Calculate the resistance of RuOx thermometer using Demod, ADAC, GDAC values
		for (int i = 0; i < numberOfChannels; i++)
		{
			double adac = ADacRaw[i];
			double gdac = GDac[i];
			double demod = DemodRaw[i];

			// Ensure GDAC != 0.0, in which case we can't compute a resistance
			if (gdac == 0.0)
			{
				gdac=-1;  // Set to an invalid value if gdac==0
				continue;
			}

			double X = preAmpGain * 65536.0 / 4.99 * (adac / gdac) * 4 / R52_DivideFactor;
			double delta = demod / Nsum;
			double res = Rbridge * delta / (X - delta);
			Demod[i] = (float) res;
			Temperature[i] = calibrators[i].getTemperature(Demod[i]);
			
			thermometryPacket.setTr(tReadMessage.addDemod(Demod[i]));
			thermometryPacket.setTr(tReadMessage.addTemperature(Temperature[i]));	


		}

	}





	private float getTemperatureTest(double res) {

		//Just used for debugging purposes, introducing known values and see if the result match

		/**
		 * Logaritmic interpolation, we have (x1,y1) and (x2,y2), where x == temperature and y == resistance
		 * 
		 *  y = - (log x)/(log z) + b 
		 *  
		 *  b = y1 + (log x1)/(log z) 
		 *  z = (x1/x2)^(1/(y2-y1))
		 */

		res = (float) 11600.1;
		float x1 = (float) 26.985;
		float y1 = (float) 11648.1;

		float x2 = (float) 35.0825;
		float y2 = (float) 11309.9;

		float z = (float) Math.pow( (x1/x2), (1/(y2-y1)));
		float b = (float) (y1 + (Math.log(x1))/(Math.log(z))); 

		/**
		 *  y = - (log x)/(log z) + b 
		 *  res =  - (log temperature)/(log z) + b 
		 *  temperature = z^-(res-b)
		 *  temperature = z^(b-res)
		 *  
		 * 
		 * */
		return (float) Math.pow(z, (b-res));
	}
}

/*
Pos Len	Meaning
0	1   '*'
1	2	Address
3	1	Card type
4	2	Frame counter	
6	1	TMux		// Usually set to '@' for all.
7	1	Status		// 0=standard mode, 1=high_res mode, 2=low_res mode

8	8	Demod[0]
16	4	ADAC[0]
20	4	GDAC[0]

24	8	Demod[1]
32	4	ADAC[1]
36	4	GDAC[1]

40	8	Demod[2]
48	4	ADAC[2]
52	4	GDAC[2]

56	8	Demod[3]
64	4	ADAC[3]
68	4	GDAC[3]

72	8	Demod[4]
80	4	ADAC[4]
84	4	GDAC[4]

88	8	Demod[5]
96	4	ADAC[5]
100	4	GDAC[5]

104	8	Demod[6]
112	4	ADAC[6]
116	4	GDAC[6]

120	8	Demod[7]
128	4	ADAC[7]
132	4	GDAC[7]

136	8	Demod[8]
144	4	ADAC[8]
148	4	GDAC[8]

152	8	Demod[9]
160	4	ADAC[9]
164	4	GDAC[9]

168	8	Demod[10]
176	4	ADAC[10]
180	4	GDAC[10]

184	8	Demod[11]
192	4	ADAC[11]
196	4	GDAC[11]

200	8	Demod[12]
208	4	ADAC[12]
212	4	GDAC[12]

216	8	Demod[13]
224	4	ADAC[13]
228	4	GDAC[13]

232	8	Demod[14]
240	4	ADAC[14]
244	4	GDAC[14]

248	8	Demod[15]
256	4	ADAC[15]
260	4	GDAC[15]

264 2	N_SUM 	// How many samples per half reading

266 1	'\r'
267 1	'\n'
268 1 	'\0'
 */