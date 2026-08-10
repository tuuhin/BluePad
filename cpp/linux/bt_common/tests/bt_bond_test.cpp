#include "bt_common_c.h"
#include <gtest/gtest.h>

TEST(BT_BOND_TESTS, SHOULD_RETURN_TRUE_OR_FALSE) {
    init_logger();
    const auto  result = is_device_bonded("00:00:00:00:00:00");
    EXPECT_EQ(result, bt_bond_state::ERROR_INVALID_DEVICE);
}
