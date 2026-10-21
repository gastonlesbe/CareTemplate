#!/bin/bash
# Script para iniciar Appium Server

cd "$(dirname "$0")"

# Configurar ANDROID_HOME - Priorizar Android Studio SDK
if [ -z "$ANDROID_HOME" ]; then
    # Priorizar Android Studio SDK
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME=$HOME/Android/Sdk
    elif [ -d "/usr/lib/android-sdk" ]; then
        export ANDROID_HOME=/usr/lib/android-sdk
    else
        # Intentar obtener desde adb
        ADB_PATH=$(which adb)
        if [ -n "$ADB_PATH" ]; then
            export ANDROID_HOME=$(dirname $(dirname "$ADB_PATH"))
        fi
    fi
fi

# Agregar build-tools al PATH (necesario para apksigner)
if [ -n "$ANDROID_HOME" ]; then
    # Buscar la versión más reciente de build-tools
    LATEST_BUILD_TOOLS=$(ls -d "$ANDROID_HOME/build-tools"/* 2>/dev/null | sort -V | tail -1)
    if [ -n "$LATEST_BUILD_TOOLS" ]; then
        export PATH=$PATH:"$LATEST_BUILD_TOOLS"
    fi
    export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
fi

echo "ANDROID_HOME: $ANDROID_HOME"
echo "Build Tools: $LATEST_BUILD_TOOLS"

echo "ANDROID_HOME: $ANDROID_HOME"
npx appium

