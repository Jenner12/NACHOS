#include "syscall.h"
#include "stdio.h"

// PROGRAMA QUE DA IMPRESION DE QUE SE SUSPENDE EJECUCIÓN DURANTE 10 SEGUNDOS.
int main (int argc, char** argv){
	int x = 0;
	while (x < 0X047FFFFF)
		x++;
	printf("\nDelay de 10 segundos terminado\n");

	return 0;
}