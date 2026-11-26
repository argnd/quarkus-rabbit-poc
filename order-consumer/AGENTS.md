# order-consumer

Service Quarkus 3.40 (LTS), Java 21, Maven. Consomme les événements « commande créée » depuis RabbitMQ
et les stocke dans MongoDB. Conçu pour tourner en plusieurs instances.

## Commandes

- Tests : `mvn test` (Docker doit être démarré, voir Tests)
- Mode dev : `mvn quarkus:dev` (RabbitMQ et MongoDB doivent tourner, voir ci-dessous)
- Broker et base locaux : `docker compose up -d`

## Ports

- 8080 : API HTTP de l'application
- 5672 : RabbitMQ (AMQP)
- 15672 : console de management RabbitMQ
- 27017 : MongoDB

## Variables d'environnement

`RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `MONGODB_URI`.
Valeurs par défaut locales dans `application.properties`.

## Structure

Package racine `com.example.orders` :

- `model` : payloads des messages (records)
- `messaging` : consommateurs `@Incoming`, sans logique métier
- `service` : logique métier
- `persistence` : documents MongoDB (Panache)
- `api` : ressources REST

`k8s/` : manifests Kubernetes à valeurs factices (Deployment, Service, exemple de Secret, ScaledObject KEDA).

Topologie RabbitMQ : exchange `orders` (topic), routing key `order.created`,
queue `orders.created`, dead-letter queue `orders.created.dlq`.

MongoDB : base `orders`, collection `orders`, l'identifiant de commande sert de `_id`.

## Conventions

- Un consommateur désérialise et délègue au service ; une exception rejette le message vers la DLQ.
- Les traitements sont idempotents entre instances : on insère sans lire avant, et une erreur de clé
  dupliquée sur `_id` signifie « déjà traité ».
- Aucun état en mémoire dans l'application : tout ce qui doit être partagé va dans MongoDB.
- `max-outstanding-messages` plafonne les messages non acquittés par instance, pour que la file se
  répartisse entre les instances.
- Les Dev Services RabbitMQ sont désactivés : le broker local vient de `docker-compose.yml`.
- Le nom de la queue est aussi écrit dans `k8s/keda-scaledobject.yaml` : garder les deux alignés.

## Tests

- Le canal `orders` est basculé sur le connecteur in-memory par `InMemoryMessagingResource` : aucun broker requis.
- MongoDB est démarré dans Docker par les Dev Services (pas de `connection-string` dans le profil test).
- Le consommateur tourne sur un thread worker : un test attend le traitement avant d'affirmer (`awaitOrder`).

## Pièges

- Si les tests échouent au démarrage avec `Unable to establish loopback connection`
  (vu sous Windows 11 build 26300), lancer
  `mvn test "-DargLine=-Djdk.net.unixdomain.tmpdir=Z:\absent"` : un dossier inexistant
  force la JVM à utiliser le loopback TCP.
