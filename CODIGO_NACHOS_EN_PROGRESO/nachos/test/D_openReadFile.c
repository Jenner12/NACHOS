#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
  int file_d = open("demo.txt");
  printf("el fd es: %d", file_d);
  if(file_d != -1) {
  	char *str[20]; 
  	read(file_d, str, 20);
  	printf("%s\n", str);
  	read(file_d, str, 20);
  	printf("%s\n", str);
  } else {
  	printf("El archivo 'demo.txt' no se pudo abrir\n");
  }
  return 0;
}
