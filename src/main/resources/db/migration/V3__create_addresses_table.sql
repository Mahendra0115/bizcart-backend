create table addresses (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    full_name varchar(100) not null,
    phone varchar(20) not null,
    address_line_1 varchar(255) not null,
    address_line_2 varchar(255),
    landmark varchar(150),
    city varchar(100) not null,
    state varchar(100) not null,
    postal_code varchar(20) not null,
    country varchar(100) not null,
    address_type varchar(50) not null,
    default_address boolean not null default false,
    deleted boolean not null default false,
    primary key (id),
    constraint fk_addresses_user foreign key (user_id) references users (id)
);

create index ix_addresses_user_id on addresses (user_id);
create index ix_addresses_user_default on addresses (user_id, default_address);
create index ix_addresses_user_deleted on addresses (user_id, deleted);
