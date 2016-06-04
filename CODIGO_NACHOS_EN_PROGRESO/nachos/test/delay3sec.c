#include "syscall.h"
#include "stdio.h"

// PROGRAMA QUE DA IMPRESION DE QUE SE SUSPENDE EJECUCIÓN DURANTE 3 SEGUNDOS.
int main (int argc, char** argv){
	int x = 0;
	while (x < 0X015FFFFF)
		x++;
	printf("\nDelay de 3 segundos terminado\n");

	return 0;
}