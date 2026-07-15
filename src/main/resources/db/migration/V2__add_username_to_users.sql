alter table users
    add column username varchar(50);

update users
set username = concat('user-', id)
where username is null;

alter table users
    modify column username varchar(50) not null;

create unique index ux_users_username on users (username);
