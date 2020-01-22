#!/usr/bin/env bash

# Example usage within an action:
# # Temporarily save repository owner in an env variable (required for sbt login)
# - run: echo "::set-env name=GITHUB_OWNER::$(echo "${GITHUB_REPOSITORY}" | cut -d / -f1)"
#
# - name: Login to GPR
#   run: bash .scripts/authenticate-sbt.sh
#   env:
#     GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

CRED_FILE="credentials += Credentials(
  \"CodeOverflow GitHub Package Registry\",
  \"maven.pkg.github.com\",
  \"${GITHUB_OWNER}\",
  \"${GITHUB_TOKEN}\"
)"

mkdir -p ~/.sbt/1.0 && echo "${CRED_FILE}" >~/.sbt/1.0/gpr.sbt
