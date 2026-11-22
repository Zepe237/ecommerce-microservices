# One image recipe for every service. Build the jars first (./mvnw -DskipTests package),
# then: docker build --build-arg MODULE=order-service -t ecommerce/order-service:local .
FROM eclipse-temurin:21-jre

ARG MODULE
WORKDIR /app

# Never run an application as root inside a container.
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY ${MODULE}/target/${MODULE}-*.jar app.jar

USER app

# The JVM sizes its heap from the container memory limit instead of the host memory.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
