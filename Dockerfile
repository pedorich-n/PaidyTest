FROM openjdk:8-jre-alpine

ARG USER=executor
ARG UID=2000
ARG GID=2000

RUN addgroup \
    --system \
    --gid "$GID" \
    "$USER"

RUN adduser \
    --disabled-password \
    --gecos "" \
    --ingroup "$USER" \
    --uid "$UID" \
    "$USER"

WORKDIR /app

COPY target/scala-2.12/forex-assembly-1.0.1.jar ./api-assembly.jar
COPY src/main/resources/application.conf ./application.conf

RUN chown -R "$UID":"$GID" /app

USER "$USER"
ENTRYPOINT ["java", "-Dconfig.file=/app/application.conf", "-jar", "/app/api-assembly.jar"]