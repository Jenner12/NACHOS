#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main (int argc, char** argv){
	int file1FD = creat("File1.txt");
	int file2FD = creat("File2.txt");
	write(file1FD, "hola", 4);
	write(file2FD, "mundo", 5);
	close(file1FD);
	int file1 = open("File2.txt");
	int file2 = open("File1.txt");
	write(file1, "popo1", 5);
	write(file2, "popo2", 5);
	return 0;
}