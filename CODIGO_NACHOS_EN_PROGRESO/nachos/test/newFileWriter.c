#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define STR_FIN "STOP_INPUT"

int main (int argc, char** argv){
	printf("\n **** New File Writer ****\n\n * Escriba el nombre del archivo que desea crear (incluya extension): ");
	char* archivo;
	readline(archivo, 40);
	///*
	int archivoFD = creat(archivo);
	if (archivoFD == -1){
		printf("\n\n *** No fue posible crear el archivo, terminando ejecucion...");
		exit(0);
	}
	printf("\n\n * Ahora ingrese todo el texto que desea que %s contenga...", archivo);
	printf("\n   Para finalizar la escritura, ingrese %s\n", STR_FIN);
	int primera = 0;
	while(true){
		printf("\n->");
		char* input;
		readline(archivo, 256);
		if (strcmp(input,STR_FIN)==00)
			break;
		int buffSize = strlen(input);
		if (buffSize != write(archivoFD, input, buffSize)){
			printf("\n\n - Ocurrio un error al momento de escribir en %s\n", archivo);
			break;
		}
		write(archivoFD,"\n",1);
	}
	close(archivoFD);
	//*/
	return 0;
}