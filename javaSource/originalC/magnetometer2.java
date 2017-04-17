/*
 * Performs autofocus of star camera
 */
#include <sys/stat.h>
#include <zmq.h>
#include <pwd.h>
#include <time.h>
#include <stdio.h>

#include <javaNativeMessaging.hpp>

#include "main.hpp"

#include "bettiiGlobal.h"
#include "zmq.hpp"
#include "javaNativeMessage.pb.h"
//#include "magnet_zmq.hpp"

using namespace FlyCapture2;

// ZeroMQ stuff
static void *fZContext;
static void *fMagnetometerZSub;
static void *f422ZPush;
static char *fSettingsDir;
static int fNeedToInit = 1;
static void *fZSender;


static struct java_native_msg_ctx* java_ctx;

static int openPort() {
	int err = 0;

	printf("Preparing ZeroMQ Yo! (push %s)\n", ZMQ_PATH_SERIAL_422_OUT);

	fZContext = zmq_ctx_new();
	if (fZContext == 0) {
		perror("zmq_ctx_new");
		return -1;
	}

	fZSender = zmq_socket(fZContext, ZMQ_PUSH);
	if (fZSender == 0) {
		perror("zmq_socket");
		return -1;
	}

	if ((err = checkReturn(zmq_connect(fZSender, ZMQ_PATH_SERIAL_422_OUT),
			"zmq_socket")) == 0) {
		fNeedToInit = 0;
	}

	return err;
}

//int zmqClose() {
//
//	zmq_close(fZSender);
//	zmq_ctx_destroy(fZContext);
//	fNeedToInit = 1;
//	return 0;
//}

void mag_free(void *buf, void *hint)
{
	free(buf);
}


int writeMagnetometerOutputToZeroMQ(struct java_native_msg_ctx* ctx, uint32_t frameNumber, float Qr, float Qi, float Qj, float Qk, float roll, float pitch, float azimuth, float Ra, float Dec, float X_mag, float Y_mag, float Z_mag, float TimeLST, float Latitude, int mag_ID)
{


	if (fNeedToInit == 1) {
		// Warning - not thread safe so two overlaping
		// calls may cause trouble
		openPort();
	}

	// NOTE!!!  Something bigger than we need
	char *buf = (char *)malloc(500);
	int ret = 0;


	uint32_t qr, qi, qj, qk, Roll, Pitch, Azimuth, ra, dec, X_MagSwap, Y_MagSwap, Z_MagSwap,TimeLSTSwap,LatitudeSwap,MagIDSwap;

	qr = byteSwap(doubleToFixedPoint(32,3, Qr));
	qi = byteSwap(doubleToFixedPoint(32,3, Qi));
	qj = byteSwap(doubleToFixedPoint(32,3, Qj));
	qk = byteSwap(doubleToFixedPoint(32,3, Qk));

	ra = byteSwap(doubleToFixedPoint(32,10, Ra));
	dec = byteSwap(doubleToFixedPoint(32,10, Dec));

	Roll = byteSwap(doubleToFixedPoint(32,10, roll));
	Pitch = byteSwap(doubleToFixedPoint(32,10, pitch));
	Azimuth = byteSwap(doubleToFixedPoint(32,10, azimuth));

	X_MagSwap = byteSwap(X_mag);
	Y_MagSwap = byteSwap(Y_mag);
	Z_MagSwap = byteSwap(Z_mag);

	TimeLSTSwap = byteSwap(doubleToFixedPoint(32,6, TimeLST));
 	LatitudeSwap= byteSwap(doubleToFixedPoint(32,8, Latitude));
MagIDSwap= byteSwap(mag_ID);

	int byteNum = 0;

	buf[byteNum++] = SYNC_BYTE;
	buf[byteNum++] = MAGN_TC;

	unsigned int fnSwap = byteSwap(frameNumber);

	memcpy(buf + byteNum, &fnSwap, sizeof(uint32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &qr, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &qi, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &qj, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &qk, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

memcpy(buf + byteNum, &Roll, sizeof(int32_t));
        byteNum+=sizeof(uint32_t);

memcpy(buf + byteNum, &Pitch, sizeof(int32_t));
        byteNum+=sizeof(uint32_t);

memcpy(buf + byteNum, &Azimuth, sizeof(int32_t));
        byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &ra, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &dec, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

        memcpy(buf + byteNum, &X_MagSwap, sizeof(int32_t));
                byteNum+=sizeof(uint32_t);
        memcpy(buf + byteNum, &Y_MagSwap, sizeof(int32_t));
                byteNum+=sizeof(uint32_t);
        memcpy(buf + byteNum, &Z_MagSwap, sizeof(int32_t));
                byteNum+=sizeof(uint32_t);

memcpy(buf + byteNum, &TimeLSTSwap, sizeof(int32_t));
        byteNum+=sizeof(uint32_t);
memcpy(buf + byteNum, &LatitudeSwap, sizeof(int32_t));
        byteNum+=sizeof(uint32_t);
memcpy(buf + byteNum, &MagIDSwap, sizeof(int32_t));
        byteNum+=sizeof(uint32_t);

	/*memcpy(buf + byteNum, &Y_Acc, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &Z_Acc, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &X_Mag, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &Y_Mag, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);

	memcpy(buf + byteNum, &Z_Mag, sizeof(int32_t));
	byteNum+=sizeof(uint32_t);*/
	zmq_msg_t msg;


	ret = checkReturn(zmq_msg_init_data(&msg, buf, byteNum, mag_free, NULL), __FILE__ ":msg_init");
	ret = zmq_msg_send(&msg, fZSender, 0);
	if (ret > 0)
	{
		log(ctx, LOG_INFO,__FILE__, "Sent solution", MAGNETOMETER2_SUBSYS_NAME);
	}
	else{
		log(ctx, LOG_WARNING,__FILE__, "Error on message send", MAGNETOMETER2_SUBSYS_NAME);
	}
	return ret;
}

static long getCurrentMillis() {

	struct timeval tv;
	gettimeofday(&tv, NULL);
	return (tv.tv_sec) * 1000 + (tv.tv_usec) / 1000;

}


static int initComm() {

	fZContext = zmq_ctx_new();
	if (fZContext == 0) {
		checkAndLogSysError(java_ctx, -1, __FUNCTION__, "zmq_ctx_new",
				MAGNETOMETER2_SUBSYS_NAME);
		return -1;
	}

	/*
	 * Angle Sensor Sub
	 */
	printf("Preparing ZeroMQ (sub %s..\n", ZMQ_PATH_FROM_MAGNETOMETER);

	fMagnetometerZSub = zmq_socket(fZContext, ZMQ_SUB);
	if (fMagnetometerZSub == 0) {
		checkAndLogSysError(java_ctx, -1, __FUNCTION__,
				"zmq_socket for magnetometer sub",
				MAGNETOMETER2_SUBSYS_NAME);

		return -1;
	}

	checkAndLogSysError(java_ctx,
			zmq_connect(fMagnetometerZSub, ZMQ_PATH_FROM_MAGNETOMETER), __FUNCTION__,
			"zmq_bind for magnetometer sub", MAGNETOMETER2_SUBSYS_NAME);

	checkAndLogSysError(java_ctx,
			zmq_setsockopt(fMagnetometerZSub, ZMQ_SUBSCRIBE, "", 0), __FUNCTION__,
			"zmq_sock_opt for magnetometer sub",
			MAGNETOMETER2_SUBSYS_NAME);

	/*
	 * 422 push
	 */

	f422ZPush = zmq_socket(fZContext, ZMQ_PUSH);
	if (f422ZPush == 0) {
		checkAndLogSysError(java_ctx, -1, __FUNCTION__,
				"zmq_socket for rs422 push",
				MAGNETOMETER2_SUBSYS_NAME);
		return -1;
	}

	checkAndLogSysError(java_ctx,
			zmq_connect(f422ZPush, ZMQ_PATH_SERIAL_422_OUT), __FUNCTION__,
			"zmq_connect for rs422 push", MAGNETOMETER2_SUBSYS_NAME);

	return 0;
}


static int closeComm() {

	zmq_close(fMagnetometerZSub);
	zmq_close(f422ZPush);
	zmq_ctx_destroy(fZContext);
	return 0;
}

void sigint_handler(int sig) {
	puts("MAGNETOMETER2_SUBSYS_NAME: SIGINT recv'd - closing\n");
	closeComm();
	exit(-1);
}

void * javaCtxThreadFun(void *fdVoid) {
	printf("Starting java Ctx thread\n");

	while (1) {
		java_native_messaging::JavaNativeMessage* mesg = JNM_block_for_msg(
				java_ctx,
				MAGNETOMETER2_SUBSYS_NAME);

		if (mesg != NULL) {

			//int dataSizeBytes = CAM_WIDTH * CAM_HEIGHT * sizeof(uint16_t);
			//uint16_t *rawImage = (uint16_t *) malloc(dataSizeBytes);
			//unsigned int fn = 0;
			//pthread_mutex_lock(&fLastImageDataMutex);
			//{
			//	memcpy(rawImage, fLastImage.GetData(), dataSizeBytes);
			//	fn = fLastImageFrameNumber;
			//}
			//pthread_mutex_unlock(&fLastImageDataMutex);

			//printf("Sending star tracker image...fn=%u\n", fn);
			//if (mesg->send_startracker_image()) {

//				if (simulationMode == 0) {
			//		send_star_cam_img(java_ctx, rawImage, CAM_WIDTH, CAM_HEIGHT,
			//				2, fn);
//				} else {
//					send_star_cam_img(java_ctx, pixsFake, CAM_WIDTH, CAM_HEIGHT,
//							2, fn);
//				}
			//}
			//free(rawImage);
			//printf("Done sending star tracker image\n");

			delete mesg;
		}
	}

	return NULL;

}

static int openSerialPort(const char *pathname, speed_t baud,
		java_native_msg_ctx* ctx) {
	int err = 0;
	int fd = -1;
	char str[5000];

	sprintf(str, "Magnetometer2 opening motor serial port at %s",
			pathname);
	log(ctx, LOG_INFO, __FILE__, __FUNCTION__, str,
			"Magnetometer2");

	fd = open(pathname, O_RDWR | O_NDELAY);

	if (fd < 0) {
		sprintf(str, "Magnetometer2 serial port could not be opened at %s",
				pathname);
		log(ctx, LOG_SEVERE, __FILE__, __FUNCTION__, str,
				MAGNETOMETER2_SUBSYS_NAME);
		fd = -1;
	} else {
		struct termios tios;

		// Turn off O_NDELAY now that we have the port open
		int file_status = fcntl(fd, F_GETFL, 0);
		fcntl(fd, F_SETFL, file_status & ~O_NDELAY);
		tcgetattr(fd, &tios);
		//tios.c_iflag = IGNBRK;
		tios.c_iflag = IXOFF | IXON;

		tios.c_oflag = 0;
		tios.c_cflag = CS8 | CREAD | CLOCAL;
		tios.c_lflag &= ~(ICANON | ISIG | ECHO);
		cfsetospeed(&tios, baud);
		cfsetispeed(&tios, baud); // Set ispeed = ospeed
		err = tcsetattr(fd, TCSANOW, &tios);

		if (err < 0) {
			sprintf(str, "Magnetometer2 could not set interface attributes for %s",
					pathname);
			log(ctx, LOG_SEVERE, __FILE__, __FUNCTION__, str,
					"Magnetometer2");
			fd = -1;
		}


	}
	return fd;
}


void readSerial(int serialPortFD, char *message){

	fflush(stdout);

	int i = 0;
	//printf("Length = %d",(int)strlen(message));
	for (i=0;i<(int)strlen(message);i++){
		write_all(serialPortFD, &message[i], 1);
		printf("%c", message[i]);
		fflush(stdout);

		usleep (10000);
	}
	char endCommand= '\r';
	printf("\n");
	fflush(stdout);
	write_all(serialPortFD, &endCommand, 1);
	usleep (10000);
}



int main(/*int argc, char *argv[]*/ void) {

	struct sigaction sa;

	java_ctx = new_ctx();

	log(java_ctx, LOG_WARNING, __FILE__, __FUNCTION__, "Starting",
			MAGNETOMETER2_SUBSYS_NAME);

//Initialize SIGINT structure
	sa.sa_handler = sigint_handler;
	sa.sa_flags = 0; // or SA_RESTART
	sigemptyset(&sa.sa_mask);

	if (sigaction(SIGINT, &sa, NULL) == -1) {
		puts("Fatal Error: SIGINT\n");
		exit(-1);
	}

	sa.sa_handler = SIG_DFL;
	sa.sa_flags = SA_NOCLDWAIT;
	sigemptyset(&sa.sa_mask);
	sigaction(SIGCHLD, &sa, NULL);


	long lastGotSettingsMillis = getCurrentMillis();

	initComm();

	printf("\nCommunication successfully initialized\n");

	pthread_t javaCtxThread;

	/*
	 * Init zeroMQ
	 */
	if (checkAndLogSysError(java_ctx,
			pthread_create(&javaCtxThread, NULL, javaCtxThreadFun, NULL),
			__FUNCTION__, "pthread_create javaCtxThread",
			MAGNETOMETER2_SUBSYS_NAME) < 0) {
		exit(-1);
	}



	printf("Calling readMagneto...\n");
	fflush(stdout);

	log(java_ctx, LOG_INFO, __FILE__, __FUNCTION__, "Starting readMagneto()", "HMR 3300");
	unsigned char buffer[1];
	int serialPortFD = openSerialPort(MAGNETOMETER2_PORT_PATH, MAGNETOMETER2_PORT_PATH_BAUD_RATE , java_ctx);

	log(java_ctx, LOG_INFO, __FILE__, __FUNCTION__, "Serial Port Opened", MAGNETOMETER2_SUBSYS_NAME);

	printf("Starting readSerial()\n\n");

	int count = 0;
	int startReading = 0;
	int flag[] = {0,0,0};
	int prev = 0;
	char raw_data[20];
	double azimuth = 0.0;
	double pitch = 0.0;
	double roll = 0.0;
	int process = 0;

	int j=0;
	while (1) {
		int ret = read(serialPortFD, &buffer[0], 1); //Get the character
		//counter++; //Allow to avoid taking into account the leftover values in the serial port

				if (buffer[0] == 10 && prev == 13) { // Header bytes
					startReading = 1; // Start reading data
					count = 0;
				}
				prev = buffer[0];


				if (startReading == 1) { // Read data
					if (buffer[0] == 13) { // Until next header
						startReading = 0;
						sscanf(raw_data, "%lf", &roll);
						memset(raw_data, 0, sizeof(raw_data));
						count = 0;
						flag[0] = 0; // Reset flags
						flag[1] = 0; // Reset flags
						flag[2] = 0; // Reset flags

						printf("\n********  Data   ************\n");
						printf("Roll: %.2f\n", roll);
						printf("Pitch: %.2f\n", pitch);
						printf("Azimuth: %.2f\n", azimuth);
						printf("\n*****************************\n");

						j++;
						process = 1;

					} else { // Store data in array
						if (buffer[0] == 44) { // If comma
							if (flag[0] == 1 && flag[1] == 0) {
								sscanf(raw_data, "%lf", &pitch);
								memset(raw_data, 0, sizeof(raw_data));
								count = 0;
								flag[1] = 1;
							}

							if (flag[0] == 0) {
								sscanf(raw_data, "%lf", &azimuth);
								memset(raw_data, 0, sizeof(raw_data));
								count = 0;
								flag[0] = 1;
							}

						} else {
							raw_data[count] = buffer[0];
							count++;
						}
					}

				}

				if (process == 1) {

					count = 0;
					startReading = 0;

					if (j == 50) {
						char* settingsDir = getSettingsDir();
						int error = 0;

						const char * key = "gpsLonDeg";
						float gpsLonDeg = readFloatSetting(settingsDir, key, &error);
						//float gpsLonDeg = -104.2455;
						key = "gpsUtcTime";
  						float gpsUtcTime = readFloatSetting(settingsDir,key, &error);

						key = "cardiff_latDegrees";
						float gpsLatDeg = readFloatSetting(settingsDir, key, &error);
						//float gpsLatDeg = 34.4717;

						// Calling Python to convert Horizontal coords to Celestial coords
						char python_code[] = "/home/mce/git/bettii/grable/starTrackerPG/magnetometer/celestialConv.py";
						FILE *file = fopen(python_code, "r");
						char command[200];
						snprintf(command, sizeof(command), "/home/mce/anaconda2/bin/python %s %f %f %f %f %f %f", python_code, gpsLonDeg, gpsLatDeg, pitch, azimuth,roll,gpsUtcTime);
						command[199] = 0; // safety
						system(command);
						//sleep(2);

						// Retrieving RA and DEC
						float RA = 0.0;
						float DEC = 0.0;

						FILE *fp = NULL;
						fp = fopen("/home/mce/git/bettii/grable/starTrackerPG/magnetometer/celestial_coord.txt", "r");
						float Qr = 0.0;
						float Qi = 0.0;
						float Qj = 0.0;
						float Qk = 0.0;
						float TimeLST = 0.0;
						float Latitude = 0.0;

						fscanf(fp, "%f", &RA);
						fscanf(fp, "%f", &DEC);
						fscanf(fp, "%f", &Qr);
						fscanf(fp, "%f", &Qi);
						fscanf(fp, "%f", &Qj);
						fscanf(fp, "%f", &Qk);
                    				fscanf(fp, "%f", &TimeLST);
                    				fscanf(fp, "%f", &Latitude);


						printf("\n**********	Celestial coords	***************\n");
						printf("%f\n", RA);
						printf("%f\n", DEC);
						printf("%f\n", Qr);
						printf("%f\n", Qi);
						printf("%f\n", Qj);
						printf("%f\n", Qk);
						printf("%f\n", TimeLST);
						printf("%f\n", Latitude);

						fclose(fp);

						// Convert char array to float
						/*double Ra_value;
						sscanf(RA, "%lf", &Ra_value);
						printf("Ra_value: %.15f\n", Ra_value);
						double Dec_value;
						sscanf(DEC, "%lf", &Dec_value);
						printf("Dec_value: %.15f\n", Dec_value);
						double qr;
						sscanf(Qr, "%lf", &qr);
						printf("Dec_value: %.15f\n", qr);
						double qi;
						sscanf(Qi, "%lf", &qi);
						printf("Dec_value: %.15f\n", qi);
						double qj;
						sscanf(Qj, "%lf", &qj);
						printf("Dec_value: %.15f\n", qj);
						double qk;
						sscanf(Qk, "%lf", &qk);
						printf("Dec_value: %.15f\n", qk);*/

						printf("\n*********************************************************\n");

						writeMagnetometerOutputToZeroMQ(java_ctx, count++, Qr, Qi, Qj, Qk, roll, pitch, azimuth, RA, DEC, 0,0,0, TimeLST, Latitude, 1);
						printf("\nSent\n");

						j = 0;

					}
		/* Create an empty ØMQ message */

	/*	zmq_msg_t msg;
		printf("\ntest\n");
		int rc = zmq_msg_init(&msg);
		assert(rc == 0);
		printf("\ntest0\n");
		checkAndLogSysError(java_ctx, zmq_msg_recv(&msg, fMagnetometerZSub, 0),
				__FUNCTION__, "zmq_msg_recv", MAGNETOMETER2_SUBSYS_NAME);
		printf("\ntest1\n");
		char *data = (char *) zmq_msg_data(&msg);
		int numBytes = zmq_msg_size(&msg);
		printf("\ntest2\n");
		if (numBytes != 5) {
			char mesg[100];
			sprintf(mesg, "Bad num bytes received! %d\n", numBytes);
			log(java_ctx, LOG_WARNING, __FILE__, __FUNCTION__, mesg,
			MAGNETOMETER2_SUBSYS_NAME);
			continue;
		}
		printf("\ntest3\n");
		int typeCode = *((unsigned char *) data);
		unsigned int fn = *((unsigned int *) (data + 1));
		printf("\ntest4\n");

		// Release message
		zmq_msg_close(&msg);
*/


		// If some time has passed, reread the settings
		if (getCurrentMillis()
				- lastGotSettingsMillis> MILLIS_BETWEEN_GETTING_SETTINGS) {
			//getAllSettings(false);
			lastGotSettingsMillis = getCurrentMillis();
			fflush(stdout);
			fflush(stderr);
		}

	process = 0;
	} // End of process loop

	} // end while loop
	close(serialPortFD);
	close_ctx(java_ctx);
	closeComm();

}

