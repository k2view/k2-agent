# Use Ubuntu as the base image
FROM vegardit/graalvm-maven:latest-java21 AS build

# Set the working directory
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /app/src/
RUN mvn package -Pnative -DskipTests

## Run Time Image
# Use specific version of Amazon Linux for reproducible builds
FROM amazonlinux:2023

# Install required packages
# procps-ng is required for "ps aux | grep K2v-Agent | grep -v grep" (liveness probe)
# shadow-utils is required for useradd and groupadd, curl for debugging, ca-certificates for SSL certificate validation
RUN yum update -y && yum upgrade -y && \
    yum install -y --allowerasing ca-certificates shadow-utils procps-ng curl && \
    yum clean all && rm -rf /var/cache/yum

# Create application user and directories
RUN mkdir -p /opt/apps && groupadd -g 1000 k2view && useradd -u 1000 -m -d /opt/apps/k2view-agent -s /bin/bash -g k2view k2view-agent
WORKDIR /opt/apps/k2view-agent

# Copy the compiled binary from the build stage
COPY --from=build /app/target/K2v-Agent /opt/apps/k2view-agent
# Create a symlink to the binary for backward compatibility
RUN ln -s /opt/apps/k2view-agent/K2v-Agent /usr/local/bin/K2v-Agent
RUN chown -R k2view-agent:k2view /opt/apps/k2view-agent /usr/local/bin/K2v-Agent

# Set the user to the application user
USER k2view-agent

# Set the entry point for the application
CMD ["/opt/apps/k2view-agent/K2v-Agent"]
