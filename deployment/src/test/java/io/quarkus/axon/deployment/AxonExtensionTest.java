package io.quarkus.axon.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.UUID;

import javax.inject.Inject;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.axon.deployment.aggregatetest.*;
import io.quarkus.axon.deployment.querytest.QueryTestHandler;
import io.quarkus.axon.deployment.querytest.RequestTestItemQuery;
import io.quarkus.axon.deployment.sagatest.StartSagaFlowCommand;
import io.quarkus.axon.deployment.sagatest.TestSaga;
import io.quarkus.axon.deployment.sagatest.TestSagaAggregate;
import io.quarkus.test.QuarkusUnitTest;

@Testcontainers
public class AxonExtensionTest {

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-axon.yml"))
                    .withExposedService("axon-server", 8024);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties")
                    .addClasses(TestItemAggregate.class, AnotherEventHandler.class,
                            TestService.class, TestRepository.class, TestSaga.class, TestSagaAggregate.class,
                            QueryTestHandler.class));

    @Inject
    private CommandGateway commandGateway;

    @Inject
    private QueryGateway queryGateway;

    @Test
    public void testAxonCommandAndEventHandling() {
        String randomAggregateIdentifier = UUID.randomUUID().toString();
        CreateTestItemCommand createTestItemCommand = new CreateTestItemCommand(randomAggregateIdentifier, "customer 2", 404.0);
        String aggregateId = commandGateway.sendAndWait(createTestItemCommand);
        assertEquals(randomAggregateIdentifier, aggregateId);
    }

    @Test
    public void testQueryHandling() {
        RequestTestItemQuery testQuery = new RequestTestItemQuery(11L);
        TestItem testItem = queryGateway.query(testQuery, TestItem.class).join();
        assertEquals(testItem.getId(), testQuery.getLookupId());
        assertEquals("some text from queryhandler", testItem.getSomeText());
    }

    @Test
    public void testSagaHandling() throws InterruptedException {
        String aggregateIdentifier = UUID.randomUUID().toString();
        String returnValue = commandGateway.sendAndWait(new StartSagaFlowCommand(aggregateIdentifier));
        assertEquals(aggregateIdentifier, returnValue);
        Thread.sleep(2000); // Give saga some time to process
    }
}
