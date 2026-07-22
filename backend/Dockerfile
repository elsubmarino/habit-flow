FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

COPY src src

RUN ./gradlew clean bootJar -x test --no-daemon && \
    cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar' | head -n 1)" app.jar


FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-XX:MaxRAMPercentage=70.0","-Duser.timezone=Asia/Seoul","-jar","app.jar"]