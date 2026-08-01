create table user_status_history (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    target_user_id bigint not null,
    admin_user_id bigint not null,
    old_status varchar(50) not null,
    new_status varchar(50) not null,
    reason varchar(500),
    changed_at datetime(6) not null,
    primary key (id),
    constraint fk_user_status_history_target foreign key (target_user_id) references users (id),
    constraint fk_user_status_history_admin foreign key (admin_user_id) references users (id)
);

create index ix_user_status_history_target_changed
    on user_status_history (target_user_id, changed_at);
create index ix_user_status_history_admin_changed
    on user_status_history (admin_user_id, changed_at);
