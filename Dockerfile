# Use Ubuntu as the base image
FROM vegardit/graalvm-maven:latest-java21 AS build

# Set the working directory
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /app/src/
RUN mvn package -Pnative -DskipTests

## Run Time Image
FROM public.ecr.aws/amazonlinux/amazonlinux:2023-minimal

# ---- OCI signature metadata ----
ARG SIGNATURE_PUBLIC_KEY
ARG SIGNATURE_BUNDLE_URL

LABEL org.opencontainers.image.signature.key="${SIGNATURE_PUBLIC_KEY}"
LABEL org.opencontainers.image.signature.bundle="${SIGNATURE_BUNDLE_URL}"

# Install required packages
RUN microdnf upgrade -y --releasever=latest && \
    microdnf install -y --nodocs --setopt=install_weak_deps=0 ca-certificates procps-ng shadow-utils && \
    microdnf clean all

# Create application user and directories
RUN mkdir -p /opt/apps && groupadd -g 1000 k2view && useradd -u 1000 -m -d /opt/apps/k2view-agent -s /bin/bash -g k2view k2view-agent
WORKDIR /opt/apps/k2view-agent

# Copy the compiled binary from the build stage
COPY --from=build --chown=k2view-agent:k2view /app/target/K2v-Agent /opt/apps/k2view-agent
# Create a symlink to the binary for backward compatibility
RUN ln -s /opt/apps/k2view-agent/K2v-Agent /usr/local/bin/K2v-Agent

# Set the user to the application user
USER k2view-agent

# Set the entry point for the application
CMD ["/opt/apps/k2view-agent/K2v-Agent"]
