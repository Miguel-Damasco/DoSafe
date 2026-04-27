package com.miguel_damasco.DoSafe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// @TestPropertySource ADDS properties on top of application.properties — it does
// not replace the file. This lets us override the datasource to H2 in memory
// without losing jwt.secret, app.*, or any other property defined in the main file.
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "aws.s3.bucket=dosafe-ci",
    "aws.region=us-east-2",
    "aws.ses.sender-email=ci@dosafe.com",
})
class DoSafeApplicationTests {

    @Test
    void contextLoads() {
    }

}
