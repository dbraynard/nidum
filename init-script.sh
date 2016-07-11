#!/bin/bash

echo "Beginning init-script.sh..."

 # Install Docker
wget -q -O - https://get.docker.io/gpg | apt-key add -
echo deb http://get.docker.io/ubuntu docker main > /etc/apt/sources.list.d/docker.list
apt-get update -qq
apt-get install -q -y --force-yes lxc-docker
usermod -a -G docker vagrant

#get cassandra docker image and startup
docker pull cassandra:3.5
docker run --name nidum-cassandra -d cassandra:3.5

#install java
sudo apt-get install -y software-properties-common python-software-properties
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
sudo apt-get install oracle-java8-installer
echo "Setting environment variables for Java 8.."
sudo apt-get install -y oracle-java8-set-default


#build and start http and node service
cd /vagrant
./gradlew build
./gradlew runNode &> runNode.out &
./gradlew runHttp &> runHttp.out &

echo "init-script.sh is complete."
date > /etc/vagrant_provisioned_at