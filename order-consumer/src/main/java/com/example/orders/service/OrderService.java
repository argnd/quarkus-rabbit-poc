package com.example.orders.service;

import com.example.orders.model.OrderCreated;
import com.example.orders.persistence.OrderDocument;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    public void register(OrderCreated order) {
        if (order.id() == null || order.id().isBlank()) {
            throw new IllegalArgumentException("Commande sans identifiant");
        }
        // RabbitMQ livre « au moins une fois », et deux instances peuvent recevoir la même commande :
        // on insère sans lire avant, et c'est l'unicité de _id qui tranche.
        try {
            OrderDocument.from(order).persist();
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) {
                throw e;
            }
            LOG.debugf("Commande %s déjà traitée, ignorée", order.id());
            return;
        }
        LOG.infof("Commande %s enregistrée (%s, %s)", order.id(), order.customer(), order.amount());
    }

    public List<OrderCreated> findAll() {
        return OrderDocument.<OrderDocument>listAll().stream().map(OrderDocument::toOrderCreated).toList();
    }
}
