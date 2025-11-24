//test read system file

//#include <iostream>
//#include <fstream>
//#include <string>
//
//int main() {
//    // Thử đọc file nhạy cảm
//    std::ifstream sensitiveFile("/etc/shadow");
//
//    if (sensitiveFile.is_open()) {
//        std::cout << "DANGER: I can read /etc/shadow!" << std::endl;
//        std::string line;
//        while (getline(sensitiveFile, line)) {
//            std::cout << line << std::endl;
//        }
//        sensitiveFile.close();
//    } else {
//        std::cout << "SAFE: Cannot open /etc/shadow (Permission denied)." << std::endl;
//    }
//
//    // Thử tạo file mới (Ghi file)
//    std::ofstream testWrite("/test_hack.txt");
//    if (testWrite.is_open()) {
//        std::cout << "DANGER: I can write to root directory!" << std::endl;
//        testWrite << "Hacked";
//        testWrite.close();
//    } else {
//        std::cout << "SAFE: Cannot write to root (Read-only file system)." << std::endl;
//    }
//
//    return 0;
//}

//test fork attack
//#include <unistd.h>
//#include <iostream>
//
//int main() {
//    std::cout << "Starting Fork Bomb..." << std::endl;
//    int count = 0;
//    while(true) {
//        pid_t pid = fork();
//        if (pid == -1) {
//            // Không thể tạo thêm tiến trình -> Docker đã chặn thành công
//            std::cout << "\nLimit reached after " << count << " processes. (Test Passed!)" << std::endl;
//            break;
//        } else if (pid == 0) {
//            // Child process: ngủ để giữ PID sống
//            while(1) sleep(1);
//        } else {
//            // Parent process
//            count++;
//        }
//    }
//    return 0;
//}


//network access test
//#include <iostream>
//#include <sys/socket.h>
//#include <arpa/inet.h>
//#include <unistd.h>
//#include <cstring>
//
//int main() {
//    int sock = socket(AF_INET, SOCK_STREAM, 0);
//    if (sock == -1) {
//        std::cout << "SAFE: Cannot create socket." << std::endl;
//        return 0;
//    }
//
//    struct sockaddr_in server;
//    server.sin_addr.s_addr = inet_addr("8.8.8.8"); // Google DNS
//    server.sin_family = AF_INET;
//    server.sin_port = htons(53);
//
//    std::cout << "Trying to connect to 8.8.8.8..." << std::endl;
//
//    if (connect(sock, (struct sockaddr *)&server, sizeof(server)) < 0) {
//        std::cout << "SAFE: Connect failed (Network unreachable)." << std::endl;
//    } else {
//        std::cout << "DANGER: Connected to internet!" << std::endl;
//        close(sock);
//    }
//
//    return 0;
//}

//infinite loop
//#include <iostream>
//
//int main() {
//    std::cout << "I will sleep forever..." << std::endl;
//    while(true) {
//        // Vòng lặp vô tận
//    }
//    return 0;
//}