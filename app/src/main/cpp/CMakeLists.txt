# SPDX-License-Identifier: GPL-3.0-only
cmake_minimum_required(VERSION 3.22.1)

project("termux" C)

add_library(termux SHARED
    hosttriangle_pty.c
)

# Enforce 16 KB page-size alignment for modern Android 15+ devices
target_link_options(termux PRIVATE "-Wl,-z,max-page-size=16384")

find_library(log-lib log)

target_link_libraries(termux PRIVATE
    ${log-lib}
)
