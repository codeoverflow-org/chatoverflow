.PHONY: advanced_run simple_run bootstrap_deploy bootstrap_deploy_dev create deploy deploy_dev fetch version package_copy reload clean git_pull

advanced_run:
	sbt clean fetch reload compile version package copy

simple_run:
	sbt package copy

bootstrap_deploy:
	sbt clean package launcherProject/assembly deploy

bootstrap_deploy_dev:
	sbt clean package buildProject/package deployDev

create:
	sbt create fetch reload

deploy:
	sbt deploy

deploy_dev:
	sbt deployDev

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
