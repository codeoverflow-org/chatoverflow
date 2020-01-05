#!/usr/bin/env bash

echo "Removing old docs..."
find docs/code -maxdepth 1 -mindepth 1 -exec rm -rf {} +

echo "Copying docs..."
cp -fR target/scala-*/api docs/code/chatoverflow
cp -fR api/target/api docs/code/chatoverflow-api