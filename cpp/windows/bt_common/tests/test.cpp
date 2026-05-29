#include "bt_common_c_api.h"
#include <iostream>

int main(int argc, char* argv[]) {

    try {
        const auto creator = bluetooth_caller_register_listener(
            [](const bool isOn) { std::cout << "CURRENT BT STATE" << isOn << std::endl; });

        std::cout << "Press [ENTER] to stop monitoring and exit." << std::endl;
        std::cin.get();

        bluetooth_caller_unregister_listener(creator);
    } catch (...) {
        std::cerr << "FAILED TO EXECUTE THE CODE";
    }
}
