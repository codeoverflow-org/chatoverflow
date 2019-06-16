.PHONY: advanced_run simple_run bootstrap_deploy create deploy fetch version package_copy reload clean git_pull

advanced_run:
	sbt clean
	sbt compile
	sbt gui
	sbt fetch
	sbt reload
	sbt version
	sbt package copy

simple_run:
	sbt compile
	sbt package copy

bootstrap_deploy:
	sbt clean
	sbt compile
	sbt gui
	sbt package copy
	sbt bs "project bootstrapProject" assembly
	sbt deploy

create:
	sbt create

deploy:
	sbt deploy

fetch:
	sbt fetch

version:
	sbt version

package_copy:
	sbt package_copy

reload:
	sbt reload

clean:
	sbt clean

git_pull:
	git stash
	git pull
	git stash pop
	cd api
	git stash
	git pull
	git stash pop
	cd ..
	cd plugins-public
	git stash
	git pull
	git stash pop
	cd ..
	cd gui
	git stash
    git pull
    git stash pop
