#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
  int file_d1 = open("demo.txt");
  int file_d2 = open("D_helloWorld.c");
  printf("El archivo demo.txt tiene el FD: %d\n", file_d1);
  printf("El archivo closeFile.c tiene el FD: %d\n", file_d2);
  
  printf("Cerrando archivo demo.txt que tiene el FD: %d\n", file_d1 );
  close(file_d1);

  int file_d3 = open("D_openReadFile.c");
  printf("El archivo openReadFile.c tiene el FD: %d\n", file_d3);
  return 0;
}
