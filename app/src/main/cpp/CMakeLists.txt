cmake_minimum_required(VERSION 3.22.1)

project("qwen_llm_jni")

add_library(
        qwen_llm_jni
        SHARED
        qwen_llm_jni.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        qwen_llm_jni
        ${log-lib}
)
