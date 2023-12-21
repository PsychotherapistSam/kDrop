# Use a base image that has the Java Runtime Environment
FROM eclipse-temurin:21_35-jre-alpine

# Install PostgreSQL to get pg_dump and pg_restore
RUN apk update \
    && apk add postgresql-client \
    && rm -rf /var/cache/apk/*

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY KopimiShare-1.0-SNAPSHOT-all.jar /app

# Define volumes for logs and files
VOLUME ["/app/logs", "/app/files", "/app/temp"]

# Run the Java application
CMD ["java", "-jar", "KopimiShare-1.0-SNAPSHOT-all.jar"]
