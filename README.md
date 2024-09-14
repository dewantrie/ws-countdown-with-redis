# ws-countdown-with-redis

This project uses Quarkus. To scale the countdown timer across multiple backend instances, all instances must be aware of the timers and maintain consistency in real-time.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.
