# HRMS API Gateway

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Cloud-2025.1.2-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
</p>

API Gateway for my HRMS project — sits in front of the monolith and will route traffic to microservices as they get extracted.

---

## Stack

| | |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.0 |
| Spring Cloud | 2025.1.2 |
| Gateway | Spring Cloud Gateway (WebFlux / Netty) |
| Cache | Redis (reactive) |
| Build | Gradle (Groovy DSL) |


## environment needed: (dev/stage/prod)

```
SPRING_PROFILES_ACTIVE=prod
REDIS_HOST=thehost
REDIS_PORT=theport
REDIS_PASSWORD=thepassword
HRMS_URI=https://theroute.com
```

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## Config rn (updates underway):

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: hrms-service
              uri: ${HRMS_URI}
              predicates:
                - Path=/**
```

---

## Planned

- JWT filter — validate token, inject `X-User-Id` and `X-User-Role` headers downstream
- Path-based routing per service (`/user/**`, `/form/**`, etc.)
- Rate limiting via Redis
- Spring Boot Actuator
- Eureka service discovery as services get split out
