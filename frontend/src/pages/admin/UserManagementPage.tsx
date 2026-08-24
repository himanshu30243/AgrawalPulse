import { useCallback, useMemo, useState } from 'react';
import Shield from '@mui/icons-material/Shield';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import IconButton from '@mui/material/IconButton';
import Alert from '@mui/material/Alert';
import { useAsync } from '@/hooks/useAsync';
import { LoadingErrorState } from '@/components/LoadingErrorState';
import { adminUsersApi, rolesApi } from '@/api/rbacApi';
import type { AdminUser } from '@/api/rbacApi';

// Legacy/admin-created rows (see UserDto's comment) have no name on file - falls back to the
// email local-part rather than showing a blank cell.
function userDisplayName(user: AdminUser): string {
  const name = [user.firstName, user.middleName, user.lastName].filter(Boolean).join(' ');
  return name || (user.email.split('@')[0] ?? user.email);
}

export default function UserManagementPage() {
  const { data: users, isLoading, error, reload } = useAsync(() => adminUsersApi.list(), []);
  const { data: roles } = useAsync(() => rolesApi.list(), []);

  const [editing, setEditing] = useState<AdminUser | null>(null);
  const [roleCode, setRoleCode] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return users ?? [];
    return (users ?? []).filter(
      (user) =>
        user.email.toLowerCase().includes(query) ||
        userDisplayName(user).toLowerCase().includes(query) ||
        (user.mobileNumber ?? '').includes(query) ||
        (user.role?.roleName ?? '').toLowerCase().includes(query),
    );
  }, [users, search]);

  // Only active roles are assignable - a deactivated role should not gain new holders, though
  // existing ones keep it until reassigned.
  const assignableRoles = useMemo(() => (roles ?? []).filter((role) => role.active), [roles]);

  const openEdit = (user: AdminUser) => {
    setEditing(user);
    setRoleCode(user.role?.roleCode ?? '');
    setFormError(null);
  };

  const close = () => {
    setEditing(null);
    setFormError(null);
  };

  const save = useCallback(async () => {
    if (!editing || !roleCode) {
      setFormError('Select a role.');
      return;
    }
    setBusy(true);
    setFormError(null);
    try {
      await adminUsersApi.setRole(editing.id, roleCode);
      reload();
      close();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'The role could not be changed.');
    } finally {
      setBusy(false);
    }
  }, [editing, roleCode, reload]);

  return (
    <Box sx={{ p: 3 }}>
      <Stack spacing={0.5} sx={{ mb: 3 }}>
        <Typography variant="h5" fontWeight={600}>
          User Management
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Users in your chapter and the role each one holds
        </Typography>
      </Stack>

      <TextField
        fullWidth
        size="small"
        placeholder="Search by name, email, mobile, or role"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 3, maxWidth: 420 }}
      />

      <LoadingErrorState isLoading={isLoading} error={error} onRetry={reload} />

      {users && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Mobile</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Joined</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((user) => (
                <TableRow key={user.id} hover>
                  <TableCell>{userDisplayName(user)}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>{user.mobileNumber ?? '—'}</TableCell>
                  <TableCell>
                    {user.role ? (
                      <Chip label={user.role.roleName} size="small" variant="outlined" />
                    ) : (
                      <Typography variant="caption" color="text.secondary">
                        None
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={user.status}
                      size="small"
                      color={user.status === 'ACTIVE' ? 'success' : 'default'}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    {user.createdAt ? new Date(user.createdAt).toLocaleDateString('en-IN') : '—'}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Change role">
                      <IconButton size="small" onClick={() => openEdit(user)} aria-label={`Change role for ${user.email}`}>
                        <Shield fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {users && filtered.length === 0 && (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">No users match your search.</Typography>
        </Paper>
      )}

      <Dialog open={editing !== null} onClose={close} maxWidth="xs" fullWidth>
        <DialogTitle>Change role</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {formError && <Alert severity="error">{formError}</Alert>}
            <Typography variant="body2" color="text.secondary">
              {editing ? `${userDisplayName(editing)} · ${editing.email}` : ''}
            </Typography>
            {/*
              A single select, not checkboxes: a user holds exactly one role since user-service's
              V2 migration collapsed app_user_roles. What they can do comes from that role's
              permissions, which are edited on the Role Management screen.
            */}
            <TextField
              select
              label="Role"
              value={roleCode}
              onChange={(e) => setRoleCode(e.target.value)}
              fullWidth
            >
              {assignableRoles.map((role) => (
                <MenuItem key={role.roleId} value={role.roleCode}>
                  {role.roleName}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={() => void save()} disabled={busy}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
