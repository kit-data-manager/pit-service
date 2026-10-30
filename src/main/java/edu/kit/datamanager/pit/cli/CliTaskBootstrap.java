package edu.kit.datamanager.pit.cli;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ConfigurableApplicationContext;

import edu.kit.datamanager.entities.messaging.PidRecordMessage;
import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.PidNotFoundException;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.elasticsearch.PidRecordElasticRepository;
import edu.kit.datamanager.pit.elasticsearch.PidRecordElasticWrapper;
import edu.kit.datamanager.pit.pidlog.KnownPid;
import edu.kit.datamanager.pit.pidlog.KnownPidsDao;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;

public class CliTaskBootstrap implements ICliTask {

    private static final Logger LOG = LoggerFactory.getLogger(CliTaskBootstrap.class);

    protected Stream<String> pids;
    protected ApplicationProperties appProps;
    protected KnownPidsDao knownPids;
    protected IMessagingService messagingService;
    protected Optional<PidRecordElasticRepository> elastic;
    protected ITypingService typingService;

    public CliTaskBootstrap(ConfigurableApplicationContext context, Stream<String> pids) {
        this.pids = pids;
        this.appProps = context.getBean(ApplicationProperties.class);
        this.knownPids = context.getBean(KnownPidsDao.class);
        this.messagingService = context.getBean(IMessagingService.class);
        this.elastic = this.getBeanOptional(context, PidRecordElasticRepository.class);
        this.typingService = context.getBean(ITypingService.class);
    }

    private <T> Optional<T> getBeanOptional(ConfigurableApplicationContext context, Class<T> clazz) {
        try {
            return Optional.of(context.getBean(clazz));
        } catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean process() throws IOException, InvalidConfigException {
        Instant unknownTime = Instant.ofEpochMilli(0);
        pids
            .map(pid -> new KnownPid(pid, unknownTime, unknownTime))
            .forEach(known -> {
                // store PIDs in the local database of known PIDs
                LOG.info("Store PID {} in the local database of known PIDs.", known.getPid());
                boolean exists = knownPids.findByPid(known.getPid()).isPresent();
                if (!exists) {
                    knownPids.save(known);
                }
                // send to message broker
                PidRecordMessage message = PidRecordMessage.creation(
                    known.getPid(),
                    "",
                    AuthenticationHelper.getPrincipal(),
                    ControllerUtils.getLocalHostname()
                );
                LOG.info("Send PID {} to message broker.", known.getPid());
                messagingService.send(message);
                // store in Elasticsearch
                elastic.ifPresent(elastic -> {
                    try {
                        PIDRecord rec = typingService.queryPid(known.getPid());
                        LOG.info("Store PID {} in Elasticsearch.", known.getPid());
                        PidRecordElasticWrapper wrapper = new PidRecordElasticWrapper(rec, typingService.getOperations());
                        elastic.save(wrapper);
                    } catch (PidNotFoundException | ExternalServiceException e) {
                        LOG.error("Failed to query PID {}.", known.getPid(), e);
                    }
                });
            });
        knownPids.flush();
        return false;
    }

}
