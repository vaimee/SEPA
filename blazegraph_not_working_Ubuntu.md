# Java problems with Blazegraph on Ubuntu

openjdk may give some problems when running Blazegraph. To solve the issue, here's how to do
```
$ sudo add-apt-repository ppa:webupd8team/java
$ sudo apt-get update
$ sudo apt-get install oracle-java8-installer
$ sudo apt-get upgrade
$ sudo update-alternatives --config java
```
and choose the java8 from oracle (or whatever else version you downloaded.