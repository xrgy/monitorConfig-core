FROM centos


VOLUME /tmp
#-Dcom.sun.management.jmxremote.username=admin
ADD jdk-8u191-linux-x64.tar.gz /usr/local/
#C:\Program Files\Java\jdk1.8.0_20\jre\lib\management\jmxremote.password
ENV JAVA_HOME /usr/local/jdk1.8.0_191
ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
ENV PATH $PATH:$JAVA_HOME/bin
COPY lib /lib
COPY template /template
ADD monitorConfig-core-1.0.jar /monitorConfig-core.jar

EXPOSE 8081
EXPOSE 30007
#"-Djava.rmi.server.hostname=47.94.157.199",\
#ENTRYPOINT ["java",\
#            "-Djava.rmi.server.hostname=47.105.64.176",\
#            "-Dcom.sun.management.jmxremote=true",\
#            "-Dcom.sun.management.jmxremote.port=30007",\
#            "-Dcom.sun.management.jmxremote.rmi.port=30007",\
#            "-Dcom.sun.management.jmxremote.ssl=false",\
#            "-Dcom.sun.management.jmxremote.authenticate=false",\
#            "-Dcom.sun.management.jmxremote.local.only=false",\
#            "-jar","/monitorConfig-core.jar"]
