package edu.kit.datamanager.pit.cli;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import edu.kit.datamanager.entities.messaging.PidRecordMessage;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;

public class CliTaskBootstrap implements ICliTask {

    private static final Logger LOG = LoggerFactory.getLogger(CliTaskBootstrap.class);

    Stream<String> pids;
    ApplicationProperties appProps;
    KnownPidsDao knownPids;
    IMessagingService messagingService;

    public CliTaskBootstrap(ConfigurableApplicationContext context, Stream<String> pids) {
        this.pids = pids;
        this.appProps = context.getBean(ApplicationProperties.class);
        this.knownPids = context.getBean(KnownPidsDao.class);
        this.messagingService = context.getBean(IMessagingService.class);
    }

    @Override
    public void process() throws IOException, InvalidConfigException {
        Instant unknownTime = Instant.ofEpochMilli(0);
        pids
            .map(pid -> new KnownPid(pid, unknownTime, unknownTime))
            .forEach(known -> {
                // store PIDs in the local database of known PIDs
                LOG.info("Store PID {} in the local database of known PIDs.", known.getPid());
                knownPids.save(known);
                // send to message broker
                PidRecordMessage message = PidRecordMessage.creation(
                    known.getPid(),
                    "",
                    AuthenticationHelper.getPrincipal(),
                    ControllerUtils.getLocalHostname()
                );
                LOG.info("Send PID {} to message broker.", known.getPid());
                messagingService.send(message);
            });
        knownPids.flush();
    }

}
