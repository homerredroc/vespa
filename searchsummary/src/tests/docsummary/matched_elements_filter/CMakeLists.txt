# Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
find_package(GTest REQUIRED)
vespa_add_executable(searchsummary_matched_elements_filter_test_app TEST
    SOURCES
    matched_elements_filter_test.cpp
    DEPENDS
    searchsummary
    GTest::GTest
)
vespa_add_test(NAME searchsummary_matched_elements_filter_test_app COMMAND searchsummary_matched_elements_filter_test_app)
