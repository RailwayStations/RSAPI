FROM openjdk:17-alpine
ENV RSAPI_HOME=/opt/services
ENV RSAPI_WORK=/var/rsapi
ENV ARTIFACT_NAME=rsapi-0.0.1-SNAPSHOT.jar
ENV SPRING_PROFILE=prod
WORKDIR $RSAPI_WORK

COPY ./build/libs/${ARTIFACT_NAME} ${RSAPI_HOME}/

RUN apk add --no-cache libsodium

EXPOSE 8080
EXPOSE 8081
CMD [ "sh", "-c", "java -Dspring.profiles.active=${SPRING_PROFILE} -jar ${RSAPI_HOME}/${ARTIFACT_NAME}"]
