insert into roles (created_at, updated_at, name, description)
select current_timestamp, current_timestamp, 'SUPER_ADMIN', 'Platform super administrator'
where not exists (select 1 from roles where name = 'SUPER_ADMIN');

insert into permissions (created_at, updated_at, name, description)
select current_timestamp, current_timestamp, 'ADMIN_ROLE_ASSIGN', 'Assign protected administrator roles'
where not exists (select 1 from permissions where name = 'ADMIN_ROLE_ASSIGN');

insert into role_permissions (created_at, updated_at, role_id, permission_id)
select current_timestamp, current_timestamp, r.id, p.id
from roles r cross join permissions p
where r.name = 'SUPER_ADMIN' and p.name = 'ADMIN_ROLE_ASSIGN'
  and not exists (
    select 1 from role_permissions existing
    where existing.role_id = r.id and existing.permission_id = p.id
  );

insert into user_roles (created_at, updated_at, user_id, role_id)
select current_timestamp, current_timestamp, u.id, r.id
from users u cross join roles r
where u.user_type = 'ADMIN' and r.name = 'SUPER_ADMIN'
  and not exists (
    select 1 from user_roles existing
    where existing.user_id = u.id and existing.role_id = r.id
  );
