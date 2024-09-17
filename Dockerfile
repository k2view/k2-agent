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
    yum install -y --allowerasing glibc glibc-devel libstdc++ curl ca-certificates

# Copy the compiled binary from the build stage
COPY --from=build /app/target/K2v-Agent /usr/local/bin/

# Set the entry point for the application
CMD ["K2v-Agent"]