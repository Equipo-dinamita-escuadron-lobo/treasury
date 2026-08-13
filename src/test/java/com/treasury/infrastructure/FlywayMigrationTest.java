package com.treasury.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @ActiveProfiles({"test","flywaytest"})
class FlywayMigrationTest {
    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;
    @Test void migratesEmptyDatabaseAndHibernateValidatesSchema()throws Exception{assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");try(var connection=dataSource.getConnection();var result=connection.getMetaData().getTables(null,null,"PAYMENT_VOUCHERS",new String[]{"TABLE"})){assertThat(result.next()).isTrue();}}
}
