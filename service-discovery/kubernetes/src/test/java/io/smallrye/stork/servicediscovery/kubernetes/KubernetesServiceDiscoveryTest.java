package io.smallrye.stork.servicediscovery.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.smallrye.stork.Service;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryTest {

    KubernetesClient client;

    String k8sMasterUrl;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = client.getMasterUrl().toString();
    }

    @Test
    void shouldGetServiceFromK8sDefaultNamespace() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        setUpKubernetesService(serviceName, "default", "10.96.96.231", "10.96.96.232", "10.96.96.233");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");
    }

    @Test
    void shouldGetServiceFromK8sTestNamespace() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", client.getNamespace()));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        setUpKubernetesService(serviceName, client.getNamespace(), "10.96.96.231", "10.96.96.232", "10.96.96.233");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");
    }

    @Test
    void shouldGetServiceFromK8sAllNamespace() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "all"));
        Stork stork = StorkTestUtils.getNewStorkInstance();
        String serviceName = "svc";

        setUpKubernetesService(serviceName, "ns1", "10.96.96.231", "10.96.96.232", "10.96.96.233");
        setUpKubernetesService(serviceName, "ns2", "10.99.99.241", "10.99.99.242", "10.99.99.243");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(6);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233", "10.99.99.241", "10.99.99.242", "10.99.99.243");
    }

    @Test
    void shouldNotFetchWhenRefreshPeriodNotReached() throws InterruptedException {
        //Given a service `my-service` registered in k8s and a refresh-period of 5 minutes
        // 1- services instance are gathered form k8s
        // 2- we remove the service
        // when the k8s service discovery is called before the end of refreshing period
        // Then stork returns the instances from the cache
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        setUpKubernetesService(serviceName, "default", "10.96.96.231", "10.96.96.232", "10.96.96.233");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

        client.endpoints().inNamespace("default").withName(serviceName).delete();

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "refresh-period", "3"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        setUpKubernetesService(serviceName, "default", "10.96.96.231", "10.96.96.232", "10.96.96.233");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

        client.endpoints().inNamespace("default").withName(serviceName).delete();

        Thread.sleep(5000);

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).isEmpty();

    }

    private void setUpKubernetesService(String serviceName, String namespace, String... ipAdresses) {
        List<EndpointAddress> endpoints = Arrays.stream(ipAdresses)
                .map(ipAdress -> new EndpointAddressBuilder().withIp(ipAdress).build()).collect(Collectors.toList());
        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName(serviceName).endMetadata()
                .addToSubsets(new EndpointSubsetBuilder().withAddresses(endpoints)
                        .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                        .build())
                .build();

        client.endpoints().inNamespace(namespace).withName(serviceName).create(endpoint);

    }

}
