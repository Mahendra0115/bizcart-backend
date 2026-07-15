create table users (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    email varchar(255) not null,
    phone varchar(20),
    password varchar(255) not null,
    profile_image varchar(500),
    user_type varchar(50) not null,
    status varchar(50) not null,
    email_verified boolean not null,
    admin_approved boolean not null,
    token_version bigint not null,
    last_login_at datetime(6),
    primary key (id)
);

create unique index ux_users_email on users (email);
create unique index ux_users_phone on users (phone);
create index ix_users_status on users (status);
create index ix_users_user_type on users (user_type);

create table roles (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    name varchar(100) not null,
    description varchar(500),
    primary key (id)
);

create unique index ux_roles_name on roles (name);

create table permissions (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    name varchar(100) not null,
    description varchar(500),
    primary key (id)
);

create unique index ux_permissions_name on permissions (name);

create table user_roles (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    role_id bigint not null,
    primary key (id),
    constraint fk_user_roles_user foreign key (user_id) references users (id),
    constraint fk_user_roles_role foreign key (role_id) references roles (id)
);

create unique index ux_user_roles_user_role on user_roles (user_id, role_id);
create index ix_user_roles_user_id on user_roles (user_id);
create index ix_user_roles_role_id on user_roles (role_id);

create table role_permissions (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    role_id bigint not null,
    permission_id bigint not null,
    primary key (id),
    constraint fk_role_permissions_role foreign key (role_id) references roles (id),
    constraint fk_role_permissions_permission foreign key (permission_id) references permissions (id)
);

create unique index ux_role_permissions_role_permission on role_permissions (role_id, permission_id);
create index ix_role_permissions_role_id on role_permissions (role_id);
create index ix_role_permissions_permission_id on role_permissions (permission_id);

create table refresh_tokens (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    token_hash varchar(255) not null,
    token_family_id varchar(36) not null,
    parent_token_id bigint,
    replaced_by_token_id bigint,
    device_info varchar(500),
    ip_address varchar(45),
    expires_at datetime(6) not null,
    revoked_at datetime(6),
    revocation_reason varchar(50),
    primary key (id),
    constraint fk_refresh_tokens_user foreign key (user_id) references users (id),
    constraint fk_refresh_tokens_parent foreign key (parent_token_id) references refresh_tokens (id),
    constraint fk_refresh_tokens_replaced_by foreign key (replaced_by_token_id) references refresh_tokens (id)
);

create unique index ux_refresh_tokens_token_hash on refresh_tokens (token_hash);
create index ix_refresh_tokens_user_id on refresh_tokens (user_id);
create index ix_refresh_tokens_token_family_id on refresh_tokens (token_family_id);
create index ix_refresh_tokens_expires_at on refresh_tokens (expires_at);
create index ix_refresh_tokens_user_revoked_expires on refresh_tokens (user_id, revoked_at, expires_at);

create table password_reset_tokens (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    token_hash varchar(255) not null,
    expires_at datetime(6) not null,
    used_at datetime(6),
    invalidated_at datetime(6),
    primary key (id),
    constraint fk_password_reset_tokens_user foreign key (user_id) references users (id)
);

create unique index ux_password_reset_tokens_token_hash on password_reset_tokens (token_hash);
create index ix_password_reset_tokens_user_id on password_reset_tokens (user_id);
create index ix_password_reset_tokens_expires_at on password_reset_tokens (expires_at);
create index ix_password_reset_tokens_user_used_expires on password_reset_tokens (user_id, used_at, expires_at);

create table verification_tokens (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    token_hash varchar(255) not null,
    verification_type varchar(50) not null,
    expires_at datetime(6) not null,
    verified_at datetime(6),
    invalidated_at datetime(6),
    primary key (id),
    constraint fk_verification_tokens_user foreign key (user_id) references users (id)
);

create unique index ux_verification_tokens_token_hash on verification_tokens (token_hash);
create index ix_verification_tokens_user_id on verification_tokens (user_id);
create index ix_verification_tokens_expires_at on verification_tokens (expires_at);
create index ix_verification_tokens_user_verified_expires on verification_tokens (user_id, verified_at, expires_at);

create table login_attempts (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    user_id bigint,
    email varchar(255),
    ip_address varchar(45),
    user_agent varchar(500),
    was_successful boolean not null,
    failure_reason varchar(255),
    attempted_at datetime(6) not null,
    primary key (id),
    constraint fk_login_attempts_user foreign key (user_id) references users (id)
);

create index ix_login_attempts_email on login_attempts (email);
create index ix_login_attempts_ip_address on login_attempts (ip_address);
create index ix_login_attempts_attempted_at on login_attempts (attempted_at);
create index ix_login_attempts_email_attempted on login_attempts (email, attempted_at);
