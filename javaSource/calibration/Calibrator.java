package calibration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thermometry.MagnetometersReaderMain;

public class Calibrator {


	private static int parenthesisCounterMAX = 2 ; // All the .txt files have a # before the 
	List<Float> fTemperature = new ArrayList<Float>();
	List<Float> fResistance = new ArrayList<Float>();
	private static final Logger sLogger = Logger.getLogger(Calibrator.class.getName());


	private boolean endOfFile = false;
	InputStream is = null;
	int c = 0;
	byte[] buffer = new byte[1];



	public Calibrator(String fileName) throws IOException{

		OutputStream out = null;
		System.out.println("Opening callibration file"); 
		//fileName = calTest.txt
		URL pathFile = getClass().getResource("/CalibrationCurves/"+fileName);
		is = pathFile.openStream();
		int parenthesisCounter = 0;
		//Looking at the files there are only couple parenthesis 
		while(parenthesisCounter < parenthesisCounterMAX){
			c = this.is.read(buffer,0,1);

			if(c>0){

				if( buffer[0] == ')'){

					parenthesisCounter++;
				}
			}
			else{				
				endOfFile = true;
			}
		}
		//Now we have removed the 'header'
		while(endOfFile == false){
			readLine();
		}
		printValues();
		System.out.println("End of file reached !");

	}
	private void printValues() {

		System.out.println("Temperature Size = "+fTemperature.size()+"\t Resistance size = "+fResistance.size() );

		System.out.println("Temperature \t Resistance ");

		for (int i = 0; i < fTemperature.size(); i++) {
			System.out.println(" "+fTemperature.get(i)+"   \t "+fResistance.get(i));
		}
		/**
		 * Just used to copy the values in order to plot the graph on excel
		 * 
		 * 	for (int i = 0; i < fTemperature.size(); i++) {
			System.out.println(fTemperature.get(i));
		}
		System.out.println("RESITANCE !!!!!!!!!!!!!!");

		for (int i = 0; i < fResistance.size(); i++) {
			System.out.println(fResistance.get(i));

		}
		 * 
		 * */
	}
	private void readLine() {
		readTemperatureValue();
		readResistanceValue();
	}
	private void readTemperatureValue(){
		//Read until it finds a numeric value

		do{
			try {
				c = this.is.read(buffer,0,1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			if(c>0){//keep reading until we hit the first decimal value (or point)

			}
			else{
				endOfFile = true;
				break;
			}

		}while( (isANumber(buffer[0])) != true);

		//Read the float value of the temperature
		char[] temperatureValue =  new char[30]; 
		temperatureValue[0] =  (char) buffer[0];

		boolean endOfValueTemperature = false;
		int indexOfDigit = 1;

		while(endOfValueTemperature == false){
			try {
				c = this.is.read(buffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(c>0){ 

				boolean isANumber = isANumber(buffer[0]);
				boolean isAExp = isAExp(buffer[0]);
				if((isANumber == false) && (isAExp == false)){
					endOfValueTemperature = true;
				}
				else{
					temperatureValue[indexOfDigit] = (char) buffer[0];
					indexOfDigit++;
				}
			}
			else{
				endOfFile = true;
				break;
			}
		}
		if (endOfFile == false) {
			String temperatureString = String.valueOf(temperatureValue);
			float temperatureFloat = Float.parseFloat(temperatureString);
			fTemperature.add(temperatureFloat);
		}


		/**
		 * Just for some testing == WORKED !
		 * */
		/*
		String test = "19841.";
		float testFloat =  Float.parseFloat(test);


		String test2 = "4.724478E+07";
		float test2Float =  Float.parseFloat(test2);*/



	}

	private boolean isAExp(byte c){
		int valueOfC = c;
		//If value is +,-,E,e
		if((valueOfC == 43) || (valueOfC == 45) || (valueOfC == 69) || (valueOfC == 69)|| (valueOfC == 101)){

			return true;
		}

		else{

			return false;
		}


	}

	private boolean isANumber(byte c){

		int valueOfC = c;
		//If value is a point or a number : .,0,1,2,3,4,5,6,7,8,9,
		if((valueOfC > 47) && (valueOfC < 58)){

			return true;
		}

		if(valueOfC == 46){

			return true;
		}

		else{

			return false;
		}

	}



	private void readResistanceValue(){

		//Read until it finds a numeric value

		do{
			try {
				c = this.is.read(buffer,0,1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			if(c>0){

			}
			else{
				endOfFile = true;
				break;
			}

		}while(isANumber(buffer[0]) != true);

		//Read the float value of the temperature
		char[] resistanceValue =  new char[20]; 
		resistanceValue[0] =  (char) buffer[0];

		boolean endOfValueResistance = false;
		int indexOfDigit = 1;

		while(endOfValueResistance == false){
			try {
				c = this.is.read(buffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

			if(c>0){ 

				boolean isANumber = isANumber(buffer[0]);
				boolean isAExp = isAExp(buffer[0]);
				if((isANumber == false) && (isAExp == false)){
					endOfValueResistance = true;

				}

				else{

					resistanceValue[indexOfDigit] = (char) buffer[0];
					indexOfDigit++;

				}


			}

			else{

				endOfFile = true;
				break;
			}

		}

		if (endOfFile == false) {
			String resistanceString = String.valueOf(resistanceValue);
			float resistanceFloat = Float.parseFloat(resistanceString);
			fResistance.add(resistanceFloat);
		}
	}


	public List<Float> getfTemperature() {
		return fTemperature;
	}


	public List<Float> getfResistance() {
		return fResistance;
	}

	public float getTemperature(double res) {

		int firstIndex = 0;
		int secondIndex = 0;
		//If the value is not within the calibration curve limits

		float minValue = searchMinValue();
		float maxValue = searchMaxValue();

		if( res < minValue || res > maxValue){
			//sLogger.log(Level.INFO,"res VALUE out of limits ("+minValue+","+ maxValue+"), VALUE = "+res+" Ohms");
			return 0;
		}
		else{

			//We find between with 2 values is our value res
			for (int i = 0; i < fResistance.size(); i++) {
				if (res > fResistance.get(i)) {

					secondIndex =  i;
					firstIndex = i- 1;
					break;
				}
			}

			/**
			 * Logaritmic interpolation, we have (x1,y1) and (x2,y2), where x == temperature and y == resistance
			 * 
			 *  y = - (log x)/(log z) + b  
			 *  
			 *  b = y1 + (log x1)/(log z) 
			 *  z = (x1/x2)^(1/(y2-y1))
			 */
			float x1 = fTemperature.get(firstIndex);
			float y1 = fResistance.get(firstIndex);

			float x2 = fTemperature.get(secondIndex);
			float y2 = fResistance.get(secondIndex);

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


			//SECOND METHOD, I THINK THE FIRST ONE IS BETTER			
			//
			//			//Difference, in ohms, between the closest values to res
			//			float span = ThermometryReader.fResistance.get(firstIndex) - ThermometryReader.fResistance.get(secondIndex);
			//
			//			/**Example: We have these values on fResistance consecutive index:
			//			 * 				...
			//			 * firstIndex = 11600
			//			 * secondIndex = 11300
			//			 * 				...
			//			 * res = 11500
			//			 * 
			//			 * span = 300
			//			 * weightSecondIndex = 1/3
			//			 * weightFirstIndex = 2/3
			//			 * */
			//			double weightSecondIndex = (ThermometryReader.fResistance.get(firstIndex) - res)/span;
			//			double weightFirstIndex = (1-weightSecondIndex);
			//
			//
			//			/**The calibration curve is logaritmic with base z:
			//			 * 
			//			 * pow(z,Resistance) = Temperature;
			//			 * 
			//			 * We will find de z for each pair of value at index firstIndex and secondIndex
			//			 * Z = x^(1/y) where X  = temperature and Y =  Resistance
			//			 * 
			//			 * */
			//
			//			double zFirstIndex = Math.pow(ThermometryReader.fTemperature.get(firstIndex), 1/ThermometryReader.fResistance.get(firstIndex));
			//			double zSecondIndex = Math.pow(ThermometryReader.fTemperature.get(secondIndex), 1/ThermometryReader.fResistance.get(secondIndex));
			//
			//			double zRes = zFirstIndex * weightFirstIndex + zSecondIndex * weightSecondIndex;
			//
			//			return (float) Math.pow(zRes, res);
		}

	}

	private float searchMaxValue() {
		float max = fResistance.get(0);
		for (int i = 1; i < fResistance.size(); i++) {
			if(fResistance.get(i) > max){
				max = fResistance.get(i);				
			}
		}
		return max;
	}
	private float searchMinValue() {

		float min = fResistance.get(0);
		for (int i = 1; i < fResistance.size(); i++) {
			if(fResistance.get(i) < min){
				min = fResistance.get(i);				
			}
		}
		return min;		
	}
//	public static void main(String[] args) {
//		try {
//			Calibrator cal = new Calibrator("calTest.txt");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//
//	}
}
