FROM debian:8


RUN apt-get update &&  apt-get install -y --no-install-recommends \
    jflex                       \
    ant                         \
    ant-optional                \
    openjdk-7-jdk               \
    texlive-latex-extra         \
    texlive-fonts-recommended


### Install tomcat ###
RUN apt-get install -y wget
# Put the newest version of tomcat, can be found:  https://downloads.apache.org/tomcat/tomcat-8/
ENV TOMCAT_VERSION=8.5.72 
RUN wget https://downloads.apache.org/tomcat/tomcat-8/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz
RUN mkdir /opt/tomcat
RUN tar xf apache-tomcat-${TOMCAT_VERSION}.tar.gz -C /opt/tomcat
RUN cd /opt/tomcat && mv apache-tomcat-${TOMCAT_VERSION} tomcat

ENV JAVA_TOOL_OPTIONS "-Dfile.encoding=UTF8"
ENV CATALINA_HOME=/opt/tomcat/tomcat
ENV CATALINA_BASE=/opt/tomcat/tomcat
ENV JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64


### build IceNLP COre ###
RUN apt-get install -y git
RUN cd / && git clone https://github.com/sverrirab/icenlp.git
WORKDIR /icenlp

WORKDIR /icenlp/core/flex/icetagger
RUN ./compileRules.sh

WORKDIR /icenlp/core/flex/iceparser
RUN ./flexAll.sh

WORKDIR /icenlp/core
RUN ant


# install org.simple.json
RUN cd $CATALINA_HOME/lib && wget https://repo1.maven.org/maven2/org/json/json/20210307/json-20210307.jar

### build INLP-web ##

#RUN rm -rf $CATALINA_HOME/webapps/ROOT/*
#ADD . $CATALINA_HOME/webapps/ROOT/

RUN cd $CATALINA_HOME/webapps/ROOT && rm * -r

WORKDIR /icenlp-web
COPY ./ .
RUN cp /icenlp/core/dist/IceNLPCore.jar /icenlp-web/lib/

RUN ant
RUN cp /icenlp-web/dist/IceNLPWeb.war $CATALINA_HOME/webapps/

ENTRYPOINT ./start.sh


