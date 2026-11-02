# =========================================
# Stage 1: Build với Maven 3.9 + Java 17
# =========================================
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml trước để tận dụng Docker layer cache (tránh tải lại dependency mỗi lần build)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ source code và build
COPY src ./src
RUN mvn clean package -DskipTests -B

# =========================================
# Stage 2: Runtime với JRE 17 nhẹ hơn
# =========================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Tạo user non-root để bảo mật
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR từ stage build
# artifactId=Homnayangi, version=0.0.1-SNAPSHOT
COPY --from=builder /app/target/Homnayangi-0.0.1-SNAPSHOT.jar app.jar

# Đổi ownership cho user non-root
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# Tối ưu JVM cho container
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
