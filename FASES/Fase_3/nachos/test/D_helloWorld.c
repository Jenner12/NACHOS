#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

/*
* Este test prueba que funcione el printf el cual
* en nachos se implmenta por el sistema de archivos
* con el syscall write
*/
int main(int argc, char** argv)
{
  printf("Hola mundo\n");
  printf("Test de printf para NACHOS\n");
  return 0;
}
