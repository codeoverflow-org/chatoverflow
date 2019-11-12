#!/bin/bash
set -ev
SCALA_MAJOR=$(echo $TRAVIS_SCALA_VERSION | egrep '^[0-9]+\.[0-9]+' -o)

# Update the java-/scaladocs on https://github.com/codeoverflow-org/chatoverflow-gh-pages

echo "Configuring git..."
git config --global user.email "travis@travis-ci.org"
git config --global user.name "Travis CI"

echo "Cloning docs repo..."
git clone https://${GH_TOKEN}@github.com/codeoverflow-org/chatoverflow-gh-pages --branch master --single-branch --depth 1 docs

echo "Copying docs..."
cp -fR target/scala-$SCALA_MAJOR/api docs/chatoverflow
cp -fR api/target/scala-$SCALA_MAJOR/api docs/chatoverflow-api
cd docs

echo "Updating wiki..."
git add .
git commit --message "Deployed doc (Travis build #$TRAVIS_BUILD_NUMBER)"
git push --force origin master