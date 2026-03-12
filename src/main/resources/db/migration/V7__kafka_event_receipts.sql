create table order_event_receipts (
    id bigint not null auto_increment,
    event_id varchar(64) not null,
    event_type varchar(64) not null,
    order_id bigint not null,
    order_no varchar(32) not null,
    order_status varchar(30) not null,
    consumer_group varchar(120) not null,
    payload_json text null,
    published_at timestamp(6) not null,
    received_at timestamp(6) not null default current_timestamp(6),
    created_at timestamp(6) not null default current_timestamp(6),
    constraint pk_order_event_receipts primary key (id),
    constraint uk_order_event_receipts_event_group unique (event_id, consumer_group)
);

create index idx_order_event_receipts_order on order_event_receipts (order_no, received_at);
