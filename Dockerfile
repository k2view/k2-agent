# Use Ubuntu as the base image
FROM vegardit/graalvm-maven:latest-java21 as build

# Set the working directory
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /app/src/
RUN mvn package -Pnative -DskipTests

## Run Time Image
FROM amazonlinux:latest

# Install required packages
RUN yum update -y && \
    yum install -y --allowerasing glibc glibc-devel libstdc++ curl ca-certificates procps-ng shadow-utils && \
    yum upgrade -y && \
    yum clean all

# Create application user and directories
RUN mkdir -p /opt/apps && groupadd -g 1000 k2view && useradd -m -d /opt/apps/k2view-agent -s /bin/bash -g k2view k2view-agent
WORKDIR /opt/apps/k2view-agent

# Copy the compiled binary from the build stage
COPY --from=build /app/target/K2v-Agent /opt/apps/k2view-agent
RUN chown -R k2view-agent:k2view /opt/apps/k2view-agent

# Set the user to the application user
USER k2view-agent

# Set the entry point for the application
CMD ["/opt/apps/k2view-agent/K2v-Agent"]
