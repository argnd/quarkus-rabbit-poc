# order-consumer

Application Quarkus qui reçoit des événements « commande créée » depuis RabbitMQ,
les stocke dans MongoDB et les expose sur `GET /orders`. Elle peut tourner en plusieurs instances.

## Prérequis

- JDK 21
- Maven 3.9+
- Docker (RabbitMQ et MongoDB locaux, et MongoDB pendant les tests)

## Lancer en local

```bash
docker compose up -d
```

```bash
mvn quarkus:dev
```

L'application écoute sur http://localhost:8080 et crée au démarrage l'exchange `orders`,
la queue `orders.created` et sa dead-letter queue `orders.created.dlq`.

## Envoyer un message de test

Dans la console RabbitMQ (http://localhost:15672, compte local par défaut), onglet
Exchanges, exchange `orders`, section « Publish message » :

- Routing key : `order.created`
- Properties : `content_type` = `application/json` (sans elle, le message n'est pas lu comme du JSON)
- Payload :

```json
{"id": "cmd-001", "customer": "Alice Martin", "amount": 42.50, "createdAt": "2026-11-26T10:15:30Z"}
```

Puis ouvrir http://localhost:8080/orders.

Un message sans `id` est rejeté et se retrouve dans `orders.created.dlq`.

## Lancer plusieurs instances

Les instances s'abonnent à la même queue et RabbitMQ répartit les messages entre elles.
Une commande reçue deux fois, par la même instance ou par deux instances, n'est stockée qu'une fois.

```bash
mvn package
```

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

```bash
java "-Dquarkus.http.port=8081" -jar target/quarkus-app/quarkus-run.jar
```

`GET /orders` renvoie la même liste sur les deux ports. L'ordre de traitement entre instances n'est pas garanti.

## Kubernetes et KEDA

Le dossier `k8s/` contient des manifests d'exemple, avec des valeurs factices à remplacer
(image, hôtes, identifiants) :

| Fichier                  | Contenu                                                              |
|--------------------------|----------------------------------------------------------------------|
| `deployment.yaml`        | Deployment de l'application, sans `replicas`                          |
| `service.yaml`           | Service exposant le port 8080                                        |
| `secret.example.yaml`    | Exemple de Secret : identifiants RabbitMQ, URI MongoDB, URL pour KEDA |
| `keda-scaledobject.yaml` | Mise à l'échelle selon la taille de la queue `orders.created`        |

KEDA doit être installé dans le cluster. Il ajoute une instance par tranche de 50 messages
en attente, entre 1 et 10 instances.

## Configuration

| Variable            | Défaut                      | Rôle                    |
|---------------------|-----------------------------|-------------------------|
| `RABBITMQ_HOST`     | `localhost`                 | Hôte du broker          |
| `RABBITMQ_PORT`     | `5672`                      | Port AMQP               |
| `RABBITMQ_USERNAME` | `guest`                     | Utilisateur             |
| `RABBITMQ_PASSWORD` | `guest`                     | Mot de passe            |
| `MONGODB_URI`       | `mongodb://localhost:27017` | Connexion à MongoDB     |

Le reste (exchange, queue, DLQ, nom de la base) est dans `src/main/resources/application.properties`.

## Tests

```bash
mvn test
```

Les tests n'ont pas besoin de RabbitMQ : le canal est remplacé par un connecteur in-memory.
Ils ont besoin de Docker : Quarkus y démarre un MongoDB le temps des tests.
