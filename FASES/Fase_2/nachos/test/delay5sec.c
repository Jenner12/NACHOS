#include "syscall.h"
#include "stdio.h"

// PROGRAMA QUE DA IMPRESION DE QUE SE SUSPENDE EJECUCIÓN DURANTE 5 SEGUNDOS.
int main (int argc, char** argv){
	int x = 0;
	while (x < 0X023FFFFF)
		x++;
	printf("\nDelay de 5 segundos terminado\n");

	return 0;
}