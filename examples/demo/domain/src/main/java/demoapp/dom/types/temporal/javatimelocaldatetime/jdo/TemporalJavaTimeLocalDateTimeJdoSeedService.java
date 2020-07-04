package demoapp.dom.types.temporal.javatimelocaldatetime.jdo;

import java.time.LocalDateTime;

import javax.inject.Inject;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.core.runtime.events.app.AppLifecycleEvent;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScript;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScripts;

import demoapp.dom.types.Samples;
import demoapp.dom.types.temporal.javatimelocaldatetime.TemporalJavaTimeLocalDateTimesStream;

@Service
public class TemporalJavaTimeLocalDateTimeJdoSeedService {

    @EventListener(AppLifecycleEvent.class)
    public void onAppLifecycleEvent(AppLifecycleEvent event) {

        if (event.getEventType() == AppLifecycleEvent.EventType.appPostMetamodel) {
            fixtureScripts.run(new TemporalJavaTimeLocalDateTimeJdoEntityFixture());
        }
    }

    @Inject
    FixtureScripts fixtureScripts;

    static class TemporalJavaTimeLocalDateTimeJdoEntityFixture extends FixtureScript {

        @Override
        protected void execute(ExecutionContext executionContext) {
            samples.stream()
                    .map(TemporalJavaTimeLocalDateTimeJdo::new)
                    .forEach(repositoryService::persist);
        }

        @Inject
        RepositoryService repositoryService;

        @Inject
        Samples<LocalDateTime> samples;
    }
}
