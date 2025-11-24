#include <stdio.h>
//
//int main(){
//    printf("Test docker");
//    return 0;
//}
//check system file
//#include <stdio.h>
//#include <stdlib.h>
//
//int main(int argc, char *argv[]) {
//    const char *path = (argc > 1) ? argv[1] : "/etc/passwd";
//    FILE *f = fopen(path, "r");
//    if (!f) {
//        perror("fopen");
//        return 2;
//    }
//    char buf[512];
//    size_t n;
//    while ((n = fread(buf, 1, sizeof(buf), f)) > 0) {
//        if (fwrite(buf, 1, n, stdout) != n) {
//            perror("fwrite");
//            fclose(f);
//            return 3;
//        }
//    }
//    fclose(f);
//    return 0;
//}

//check read write file permission
//#include <stdio.h>
//#include <errno.h>
//int main(void) {
//    const char *path = "/tmp/test_write_from_container.txt";
//    FILE *f = fopen(path, "w");
//    if (!f) {
//        perror("fopen");
//        return 2;
//    }
//    if (fprintf(f, "hello-from-container\n") < 0) {
//        perror("fprintf");
//        fclose(f);
//        return 3;
//    }
//    fclose(f);
//    printf("WROTE_OK %s\n", path);
//    return 0;
//}