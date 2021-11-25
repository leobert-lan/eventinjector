

APP_CFLAGS += -Wno-error=format-security

NDK_TOOLCHAIN_VERSION := 4.8
# APP_STL := stlport_shared  --> does not seem to contain C++11 features
APP_STL := gnustl_shared

# Enable c++11 extentions in source code
APP_CPPFLAGS += -std=c++11

APP_PROJECT_PATH := $(call my-dir)
APP_MODULES      := EventInjector
