FROM openjdk:13-jdk-slim as stage0
WORKDIR /opt/docker
COPY target/bin /opt/docker/bin
COPY target/lib /opt/docker/lib
USER root
RUN ["chmod", "-R", "u=rX,g=rX", "/opt/docker"]
RUN ["chmod", "u+x,g+x", "/opt/docker/bin/hackathon-backend"]

FROM openjdk:13-jdk-slim
USER root
RUN id -u root 1>/dev/null 2>&1 || (( getent group 0 1>/dev/null 2>&1 || ( type groupadd 1>/dev/null 2>&1 && groupadd -g 0 root || addgroup -g 0 -S root )) && ( type useradd 1>/dev/null 2>&1 && useradd --system --create-home --uid 1001 --gid 0 root || adduser -S -u 1001 -G root root ))
WORKDIR /opt/docker
COPY --from=stage0 --chown=root:root /opt/docker /opt/docker
EXPOSE 8080
USER 1001:0
ENTRYPOINT ["/opt/docker/bin/hackathon-backend"]
CMD []
