package com.treasury.infrastructure;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class FlywayPreexistingDatabaseTest {
    @Test void baselineZeroMigratesAWorkingPreexistingDatabase()throws Exception{
        String url="jdbc:h2:mem:treasury_preexisting;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try(var connection=DriverManager.getConnection(url,"sa","");var statement=connection.createStatement()){statement.execute("create table legacy_health_check(id bigint primary key)");}
        Flyway flyway=Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration").baselineOnMigrate(true).baselineVersion(MigrationVersion.fromVersion("0")).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        try(var connection=DriverManager.getConnection(url,"sa","");var result=connection.getMetaData().getTables(null,null,"PAYMENT_VOUCHERS",new String[]{"TABLE"})){assertThat(result.next()).isTrue();}
    }
}
