package com.ksa.financing.middleware.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_request_dev")
public class ClientRequestDevJpaEntity extends ClientRequestBaseJpaEntity {
}
