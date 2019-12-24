#!/usr/bin/env bash

# Example usage within an action:
# - name: Login to GPR
#   run: bash .scripts/authenticate-npm.sh
#   env:
#     GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

echo "//npm.pkg.github.com/:_authToken=${GITHUB_TOKEN}" > ~/.npmrc