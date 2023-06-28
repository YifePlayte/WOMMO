cmake_minimum_required(VERSION 3.22.1)

project("wommo")

add_library(
        classhelper
        SHARED
        native-lib.cpp
)
