package io.quarkus.axon.deployment.aggregatetest;

import javax.enterprise.context.Dependent;

import org.jboss.logging.Logger;

@Dependent
public class TestRepository {
    private Logger log = Logger.getLogger(TestRepository.class);

    public void save() {
        log.info("Test repository save");
    }
}
