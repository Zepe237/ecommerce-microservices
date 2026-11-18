# Circuit breaker sur les appels Feign

Ce document explique le circuit breaker mis en place sur les appels HTTP entre services, et les étapes pour le reproduire dans un autre projet.

## Le problème

L'order-service appelle le cart-service et l'inventory-service à chaque commande. Si l'inventory-service tombe ou devient très lent :

- chaque commande attend jusqu'au délai d'expiration avant d'échouer ;
- les threads de l'order-service restent bloqués pendant cette attente ;
- l'order-service finit par ralentir à son tour, puis ses propres appelants : la panne se propage en cascade.

## Le principe

Le circuit breaker se place devant chaque client Feign, comme un disjoncteur électrique devant un appareil.

| État | Comportement |
|---|---|
| Fermé (`CLOSED`) | Les appels passent normalement. Les échecs sont comptés. |
| Ouvert (`OPEN`) | Trop d'échecs récents : les appels ne partent plus, l'erreur est immédiate. |
| Semi-ouvert (`HALF_OPEN`) | Après un délai, quelques appels d'essai passent. S'ils réussissent, le circuit se referme ; sinon il se rouvre. |

Le circuit ouvert ne répare pas le service en panne : il évite d'attendre pour rien et laisse au service le temps de redémarrer.

## Configuration retenue

| Paramètre | Valeur | Effet |
|---|---|---|
| `sliding-window-size` | 10 | Le taux d'échec est calculé sur les 10 derniers appels |
| `minimum-number-of-calls` | 5 | Pas de décision avant 5 appels |
| `failure-rate-threshold` | 50 | Le circuit s'ouvre à 50 % d'échecs |
| `wait-duration-in-open-state` | 10s | Durée pendant laquelle le circuit reste ouvert |
| `permitted-number-of-calls-in-half-open-state` | 3 | Appels d'essai en semi-ouvert |
| `ignore-exceptions` | `FeignClientException` | Les réponses 4xx ne comptent pas comme des pannes |
| `timelimiter.timeout-duration` | 4s | Délai maximal d'un appel protégé |
| Feign `connect-timeout` / `read-timeout` | 2 s / 3 s | Délais de connexion et de lecture HTTP |

Un 404 du product-service signifie « produit inconnu » : le service fonctionne et donne une réponse métier. Seuls les erreurs de connexion, les délais dépassés et les réponses 5xx sont comptés comme des pannes.

## Comportement en cas de panne

Chaque client Feign déclare une *fallback factory*. Elle ne fabrique pas de fausse réponse (un stock ou un prix inventé serait pire qu'une erreur) : elle transforme l'échec en `DependencyUnavailableException`, traduite en réponse 503.

```json
{
  "type": "about:blank",
  "title": "Service Unavailable",
  "status": 503,
  "detail": "inventory-service is temporarily unavailable, please retry later",
  "instance": "/api/orders",
  "service": "inventory-service"
}
```

| Appel protégé | Effet d'une panne |
|---|---|
| order-service vers cart-service | `POST /api/orders` répond 503 |
| order-service vers inventory-service | `POST /api/orders` répond 503, aucune commande n'est enregistrée |
| cart-service vers product-service | `POST /api/carts/{id}/items` répond 503 |
| notification-service vers customer-service | le message Kafka est retenté par le gestionnaire d'erreurs du consommateur |

## Étapes pour l'ajouter à un service

### 1. Dépendances

Dans le `pom.xml` du service (la version vient du BOM Spring Cloud déclaré dans le POM parent) :

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Actuator n'est pas obligatoire, mais il permet de voir l'état des circuits.

### 2. Activer le circuit breaker pour Feign

Dans `application.yml`, sous `spring:` :

```yaml
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
      client:
        config:
          default:
            connect-timeout: 2000
            read-timeout: 3000
```

À partir de là, chaque méthode de chaque client Feign passe par un circuit breaker.

### 3. Régler Resilience4j

Toujours dans `application.yml`, au premier niveau :

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        register-health-indicator: true
        ignore-exceptions:
          - feign.FeignException$FeignClientException
  timelimiter:
    configs:
      default:
        timeout-duration: 4s

management:
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,circuitbreakerevents
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
```

Le time limiter doit laisser plus de temps que le `read-timeout` de Feign, sinon il coupe l'appel avant que Feign ait pu échouer proprement.

### 4. L'exception métier

Dans le package `exception` :

```java
@Getter
public class DependencyUnavailableException extends RuntimeException {

    private final String service;

    public DependencyUnavailableException(String service, Throwable cause) {
        super(service + " is temporarily unavailable", cause);
        this.service = service;
    }
}
```

### 5. La règle commune des fallbacks

Dans le package `client`, une classe utilitaire décide ce qu'est une panne :

```java
@Slf4j
final class FeignFallbacks {

    private FeignFallbacks() {
    }

    static RuntimeException translate(String service, Throwable cause) {
        if (cause instanceof FeignException.FeignClientException clientError) {
            return clientError;          // 4xx : réponse métier, on la laisse passer
        }
        log.warn("{} unavailable: {}", service, cause.toString());
        return new DependencyUnavailableException(service, cause);
    }
}
```

### 6. Une fallback factory par client Feign

```java
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    private static final String SERVICE = "inventory-service";

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public AvailabilityResponse checkAvailability(List<StockItemRequest> items) {
                throw FeignFallbacks.translate(SERVICE, cause);
            }
        };
    }
}
```

`FallbackFactory` (et non `fallback`) donne accès à la cause de l'échec, ce qui permet de distinguer un 404 d'une panne.

### 7. Brancher la fallback factory sur le client

```java
@FeignClient(name = "inventory-service", url = "${app.clients.inventory-service.url}",
        fallbackFactory = InventoryClientFallbackFactory.class)
public interface InventoryClient { ... }
```

### 8. Traduire l'exception en 503

Dans le `GlobalExceptionHandler` du service :

```java
@ExceptionHandler(DependencyUnavailableException.class)
public ProblemDetail handleDependencyUnavailable(DependencyUnavailableException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
            ex.getMessage() + ", please retry later");
    problem.setProperty("service", ex.getService());
    return problem;
}
```

## Récapitulatif par service

Les étapes 1 à 7 s'appliquent aux trois services qui appellent d'autres services ; l'étape 8 seulement à ceux qui exposent une API appelée par un client.

| Service | Étapes 1 à 5 | Fallback factories (étapes 6 et 7) | Étape 8 |
|---|---|---|---|
| order-service | oui | `CartClientFallbackFactory`, `InventoryClientFallbackFactory` | oui |
| cart-service | oui | `ProductClientFallbackFactory` | oui |
| notification-service | oui | `CustomerClientFallbackFactory` | non : l'erreur remonte au consommateur Kafka, qui retente le message |

Chaque service garde sa propre copie de `DependencyUnavailableException` et de `FeignFallbacks`, dans ses propres packages : comme pour les DTO, aucun service n'importe le code d'un autre.

Pour tester la règle sans démarrer Spring, un test unitaire par fallback factory suffit. On construit les exceptions Feign à la main :

```java
FeignException notFound = FeignException.errorStatus("ProductClient#getProduct", response(404));
assertThatThrownBy(() -> factory.create(notFound).getProduct("PRD-0001")).isSameAs(notFound);

FeignException serverError = FeignException.errorStatus("ProductClient#getProduct", response(500));
assertThatThrownBy(() -> factory.create(serverError).getProduct("PRD-0001"))
        .isInstanceOf(DependencyUnavailableException.class);
```

avec un `Response` construit par `Response.builder().status(...).reason("test").request(request).headers(Map.of()).build()`.

## Vérifier le fonctionnement

1. Démarrer l'infrastructure et tous les services, avec un panier non vide pour le client 1.
2. Arrêter l'inventory-service.
3. Appeler plusieurs fois `POST /api/orders` avec `{"customerId": 1}` : les premières réponses sont des 503 après l'échec de connexion, puis, une fois le circuit ouvert, des 503 immédiats.
4. Consulter l'état des circuits : `GET http://localhost:8081/actuator/circuitbreakers`, ou `GET http://localhost:8081/actuator/health` (section `circuitBreakers`).
5. Redémarrer l'inventory-service et attendre dix secondes : le circuit passe en semi-ouvert, les appels d'essai réussissent et il se referme.

Les tests unitaires `*FallbackFactoryTest` vérifient la règle de traduction : un 404 est relancé tel quel, un 500 ou une erreur de connexion devient `DependencyUnavailableException`.
