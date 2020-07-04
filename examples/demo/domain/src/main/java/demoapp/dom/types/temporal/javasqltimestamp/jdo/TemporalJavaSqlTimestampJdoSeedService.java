package demoapp.dom.types.temporal.javasqltimestamp.jdo;

import java.sql.Timestamp;

import javax.inject.Inject;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.core.runtime.events.app.AppLifecycleEvent;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScript;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScripts;

import demoapp.dom.types.Samples;
import demoapp.dom.types.temporal.javasqltimestamp.TemporalJavaSqlTimestampsStream;

@Service
public class TemporalJavaSqlTimestampJdoSeedService {

    @EventListener(AppLifecycleEvent.class)
    public void onAppLifecycleEvent(AppLifecycleEvent event) {

        if (event.getEventType() == AppLifecycleEvent.EventType.appPostMetamodel) {
            fixtureScripts.run(new TemporalJavaSqlTimestampJdoEntityFixture());
        }
    }

    @Inject
    FixtureScripts fixtureScripts;

    static class TemporalJavaSqlTimestampJdoEntityFixture extends FixtureScript {

        @Override
        protected void execute(ExecutionContext executionContext) {
            samples.stream()
                    .map(TemporalJavaSqlTimestampJdo::new)
                    .forEach(repositoryService::persist);
        }

        @Inject
        RepositoryService repositoryService;

        @Inject
        Samples<Timestamp> samples;
    }
}
