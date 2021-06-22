# SmallRye Dux

SmallRye Dux, Dux in short, is an extensible Service Discovery and Client-Side 
Load Balancer implementation designed to work well with reactive libraries.
Dux APIs use [SmallRye Mutiny](https://smallrye.io/smallrye-mutiny/).

Dux maps service names to addresses. 

Required `ServiceDiscovery` provides a list of addresses (`ServiceInstance`'s)
for a given service name.
Optional `LoadBalancer` selects a single `ServiceInstance` for a call.  

## Configuring user applications
The project's core aims to be vendor neutral. Aside from using Mutiny for the APIs,
it has no dependencies. As such, it provides a universal abstraction of configuration 
and vendors can choose their own way of providing Dux configuration.

MicroProfile based frameworks can use MicroProfile Config to configure Dux. 
Below is an example snippet of such a configuration:
```properties
dux.my-service.service-discovery=consul
dux.my-service.service-discovery.some-service-discovery-property=property-value
dux.my-service.load-balancer=round-robin
dux.my-service.load-balancer.some-load-balancer-property=some-prop-value

dux.my-other-service.service-discovery=test-sd-1
dux.my-other-service.load-balancer=test-lb-1
```

MicroProfile Config support for configuring SmallRye Dux is provided by the 
`smallrye-dux-microprofile` artifact.

## Integrating client libraries with Dux
Client libraries would most often use a `LoadBalancer` to get a single address
for a call.
`LoadBalancer` for a service can be retrieved from `Dux` singleton:

```java
LoadBalancer loadBalancer = Dux.getInstance().getLoadBalancer(serviceName);
Uni<ServiceInstance> serviceInstance = loadBalancer.selectServiceInstace();

```

Alternatively, one can retrieve a stream of all available `ServiceInstance`'s for a 
given service from `ServiceDiscovery`:

```java
ServiceDiscovery serviceDiscovery = Dux.getInstance().getServiceDiscovery(serviceName);
Multi<ServiceInstance> serviceInstances = serviceDiscovery.getServiceInstances();

```

## Extending the functionality
In Dux, extensibility is based on the 
[Service Provider Interface](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) mechanism.

### Configuration provider
To add a custom configuration provider, create an implementation of `DuxConfigProvider`
and register it with the Service Provider Interfaces mechanism.

SmallRye Dux comes with MicroProfile Config based configuration provider. 
To use it, add `smallrye-dux-microprofile` to your project.

### Service Discovery
To implement a custom service discovery for Dux, implement the `ServiceDiscoveryProvider`
interface and register it with the Service Provider Interface mechanism.

Please note that the `ServiceDiscovery` implementation must be non-blocking.

### Load Balancer
To implement a custom load balancer for Dux, implement the `LoadBalancerProvider`
interface and register it with the Service Provider Interface mechanism.

Please note that the `LoadBalancer` implementation, similarly to `ServiceDiscovery` 
must be non-blocking.

## Requirements
The project needs Java 11+. To build the project locally, you need Maven 3.6.3+.

