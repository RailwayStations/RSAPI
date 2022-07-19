FROM amazoncorretto:18-alpine@sha256:1b1467a9ff0bf19f1b9718b9af69c1428b64bda76524286fbb33a30a8161fe09
ENV RSAPI_HOME=/opt/services
ENV RSAPI_WORK=/var/rsapi
ENV ARTIFACT_NAME=rsapi-0.0.1-SNAPSHOT.jar
ENV SPRING_PROFILES_ACTIVE=""
WORKDIR $RSAPI_WORK

COPY ./build/libs/${ARTIFACT_NAME} ${RSAPI_HOME}/

RUN apk add --no-cache libsodium

EXPOSE 8080
EXPOSE 8081
CMD [ "sh", "-c", "java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar ${RSAPI_HOME}/${ARTIFACT_NAME}"]
