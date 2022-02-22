FROM tomcat:8-jre8

# Disable the default Tomcat webapps
RUN rm -rf $CATALINA_HOME/webapps/*
# and install ours
COPY dist/IceNLPWeb.war $CATALINA_HOME/webapps/


