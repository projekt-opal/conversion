FROM openjdk:8
COPY target/conversion-tool-1.0.0.jar .
RUN mkdir /var/log/convertion-tool
ENV LOG_HOME /var/log/convertion-tool
EXPOSE 8080/tcp
CMD java -Xms4g -Xmx8g -jar /conversion-tool-1.0.0.jar
