#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int atoi(const char *s) {
  int result=0, sign=1;

  if (*s == -1) {
    sign = -1;
    s++;
  }

  while (*s >= '0' && *s <= '9')
    result = result*10 + (*(s++)-'0');

  return result*sign;
}


int main (int argc, char** argv){
	while (1){
		printf("\n **** Process Creator ****\n\nEscriba el nombre del archivo.coff a ejecutar: ");
		char archivo[40];
		readline(archivo, 40);
		//s = getchar();
		printf("\n Cuantos argumentos desea enviar para la ejecucion de %s : ", archivo);
		char cantChar[2];
		readline(cantChar, 2);
		int cant = atoi(cantChar);
		///*
		if (cant != 0){
			printf("\n Ingrese uno por uno los %d argumentos: \n", cant);
			char* args[cant];
			int i;
			for (i = 0; i < cant; i++){
				printf("\n	* ");
				readline(args[i], 40);
			}
			printf("\nComenzando ejecucion de: %s ...\n\n", archivo);
			int pid1 = exec(archivo, cant, args);
			join(pid1, 1);
		}
		else{
			int pid1 = exec(archivo, 0, "");
			join(pid1, 1);
		}

		//int pid1 = exec("prueba.coff", 0, "");
		//*/
	}
	return 0;
}