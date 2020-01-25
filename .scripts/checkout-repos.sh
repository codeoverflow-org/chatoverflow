#!/usr/bin/env bash
set -e

FALLBACK_BRANCH="develop"
ORG="codeoverflow-org"
REPO_OWNER=$(echo "$GITHUB_REPOSITORY" | cut -d "/" -f 1)

# filters reference path and lets tag/branch remain
BRANCH=${GITHUB_REF##refs/heads/}
BRANCH=${BRANCH##refs/tags/}


function clone() {
  # I don't know if Github Actions has a tty or not, but just to be save we ignore any missing authentication and instead fail
  GIT_TERMINAL_PROMPT=0 git clone "https://github.com/$1" -b "$2" "$3" --quiet && echo "$4" # if successfully, this message will be printed
}

function cloneRepo() {
  REPO="$1"
  LOCATION="$2"
  DESIRED_BRANCH=$BRANCH

  echo "Trying to clone $REPO to $LOCATION"

  # if the branch doesn't exist on other repos we try first to get it from the codeoverflow org (if we are on a fork)
  # and otherwise use the fallback branch (develop).

  # This is needed for simple PRs, where people don't have forked all chatoverflow related repositories
  # and if a change of a branch doesn't require a change in a other repository and the branch hasn't been pushed.

  ([ "$ORG" != "$REPO_OWNER" ] && clone "$REPO_OWNER/$REPO" "$DESIRED_BRANCH" "$LOCATION" "Cloned from fork of $REPO_OWNER") ||
    (clone "$ORG/$REPO" "$DESIRED_BRANCH" "$LOCATION" "Cloned from codeoverflow-org repo") ||
    ([ "$ORG" != "$REPO_OWNER" ] && clone "$REPO_OWNER/$REPO" "$FALLBACK_BRANCH" "$LOCATION" "Cloned from fork of $REPO_OWNER with fallback branch ($FALLBACK_BRANCH)") ||
    (clone "$ORG/$REPO" "$FALLBACK_BRANCH" "$LOCATION" "Cloned from codeoverflow-org repo with fallback branch ($FALLBACK_BRANCH)")
}

cloneRepo "chatoverflow-api" "api"
cloneRepo "chatoverflow-plugins" "plugins-public"
cloneRepo "chatoverflow-gui" "gui"
cloneRepo "chatoverflow-wiki" "wiki"
cloneRepo "chatoverflow-setup" "setup"
cloneRepo "chatoverflow-launcher" "launcher"
# Add newly created repos that you also whish to clone here...
