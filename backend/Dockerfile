# 1단계: jar 빌드 (테스트는 CI에서 돌리므로 여기서는 생략)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# 2단계: 실행 이미지 (JRE만)
FROM eclipse-temurin:21-jre
WORKDIR /app
# 신청 마감 등 LocalDateTime.now() 기준 시각이 한국 시간이어야 한다 (기본 UTC면 9시간 어긋남)
ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
