package demoapp.dom.types.primitive.shorts.jdo;

import javax.inject.Inject;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.core.runtime.events.app.AppLifecycleEvent;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScript;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScripts;

import demoapp.dom.types.Samples;
import demoapp.dom.types.wrapper.shorts.WrapperShortsStream;

@Service
public class PrimitiveShortJdoSeedService {

    @EventListener(AppLifecycleEvent.class)
    public void onAppLifecycleEvent(AppLifecycleEvent event) {

        if (event.getEventType() == AppLifecycleEvent.EventType.appPostMetamodel) {
            fixtureScripts.run(new PrimitiveShortJdoEntityFixture());
        }
    }

    @Inject
    FixtureScripts fixtureScripts;

    static class PrimitiveShortJdoEntityFixture extends FixtureScript {

        @Override
        protected void execute(ExecutionContext executionContext) {
            stream.stream()
                    .map(PrimitiveShortJdo::new)
                    .forEach(repositoryService::persist);
        }

        @Inject
        RepositoryService repositoryService;

        @Inject
        Samples<Short> samples;
    }
}
