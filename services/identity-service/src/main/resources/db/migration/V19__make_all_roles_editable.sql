-- V19: Make all roles editable by super_admin
-- All roles are set to is_system = false so they can be modified/edited.
-- The is_system flag was protecting built-in roles, but super_admin must have
-- full authority to manage all roles including built-in ones.

UPDATE roles SET is_system = false;
