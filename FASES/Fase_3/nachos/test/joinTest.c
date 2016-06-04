#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main (int argc, char** argv){
	///*
	printf("JoinTest: comenzando ejecucion de matame.coff\n");
	int pid1 = exec("matame.coff", 0, "");
	printf("JoinTest: comenzando delay de 5 segundos...\n");
	join(exec("delay5sec.coff", 0, ""),1);
	printf("JoinTest: haciendo join a pid = 5\n");
	join(20, 2);
	//*/
	/*
	creat("archivito.java");
	int x = 0;
	while (x++ < 20){
		open("archivito.java");
	}
	*/
	return 0;
}