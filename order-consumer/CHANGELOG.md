# Changelog

## Unreleased

### Ajouté

- Initialisation du projet Quarkus 3.40 (Java 21, Maven).
- Consommation des événements « commande créée » depuis la queue RabbitMQ `orders.created`, avec dead-letter queue.
- Endpoint `GET /orders` listant les commandes reçues.
- `docker-compose.yml` pour un RabbitMQ et un MongoDB locaux.
- Stockage des commandes dans MongoDB, avec dédoublonnage sur l'identifiant de commande, pour permettre plusieurs instances.
- Plafond de messages non acquittés par instance (`max-outstanding-messages`).
- Manifests Kubernetes d'exemple dans `k8s/`, dont la mise à l'échelle KEDA selon la taille de la queue.
