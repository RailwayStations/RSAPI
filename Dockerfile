FROM amazoncorretto:19-alpine@sha256:46be4d85ae0f45ee7144fcb0d785907b960ee273f6e3d759b6e0c39bf03e5387
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
