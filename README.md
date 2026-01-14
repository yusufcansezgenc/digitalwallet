### Digital Wallet Case
A digital wallet service that allows users to create their wallets and deposit or withdraw money from them.

Uses Java 25 and Spring Boot 4.

### Capabilities & Development Features
* Transaction system that allows employees to accept or decline deposit or withdrawal requests.
* Wallet creation and exploring wallets.
* Basic authentication and authorization system with JWT using Spring Security.
* Comprehensive logging.
* Unit tests for main business logic.

### Running & Testing
You can use Maven or run the app straight with Docker under digitalwallet folder using command (Must have Docker or Maven installed):
```
mvn spring-boot:run
or
docker-compose up --build
```

You can also run the existing unit tests by using Maven:
```
mvn test
```

After setting the container up or running the application with Maven you can check the Swagger at: http://localhost:8080/swagger-ui/index.html - root path also redirects to Swagger.

You can also check h2 Console with: http://localhost:8080/h2-console
h2 Console uses basic authentication with JDBC Url:
```
JDBC Url: jdbc:h2:mem:digitalwalletdb
username: sa
password: password
```

You will require to login and get your JWT token for basic security, you can either use a client (Postman, Insomnia etc.) or use 'Authorize' button on Swagger page to use the JWT provided by login, there is no registration capability so you need to use existing accounts:
```
username: Customer1User
password: password

username: Employee1User
password: password
```

APPLICATION.md provides additional information such as business flow, additonal guidance to build/run the project and acts as a basic documentation for this project.