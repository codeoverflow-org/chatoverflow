#!/bin/bash
set -ev

# Update the java-/scaladocs on https://github.com/codeoverflow-org/chatoverflow-gh-pages

echo "Configuring git..."
git config user.email "<>"
git config user.name "Github Actions"

echo "Cloning docs repo..."
git clone https://${REPO_TOKEN}@github.com/codeoverflow-org/chatoverflow-gh-pages --branch master --single-branch --depth 1 docs

echo "Copying docs..."
cp -fR target/scala-*/api docs/chatoverflow
cp -fR api/target/scala-*/api docs/chatoverflow-api
cd docs

echo "Updating wiki..."
git add .
git commit --message "Deployed doc (Action build $GITHUB_ACTION)"
git push --force origin master
